// File: app/src/main/java/com/project/qlcaytrong/sync/SyncWorker.java
package com.project.qlcaytrong.sync;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.project.qlcaytrong.data.local.AppDatabase;
import com.project.qlcaytrong.data.local.entity.CayTrongEntity;
import com.project.qlcaytrong.data.local.entity.ChiTietPhunThuocEntity;
import com.project.qlcaytrong.data.local.entity.ChiTietTuoiPhanEntity;
import com.project.qlcaytrong.data.local.entity.GocCayEntity;
import com.project.qlcaytrong.data.local.entity.ManhDatEntity;
import com.project.qlcaytrong.data.local.entity.NhatKyEntity;
import com.project.qlcaytrong.data.remote.FirestoreService;
import com.project.qlcaytrong.data.remote.StorageRepository;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * SyncWorker — đồng bộ offline Room → Firestore.
 *
 * == Thứ tự sync (tại sao quan trọng?) ==
 *   Firestore là flat (không có foreign key thật), nhưng app logic cần thứ tự:
 *   1. NguoiDung: base entity, không phụ thuộc gì
 *   2. ManhDat: phụ thuộc userId
 *   3. CayTrong: phụ thuộc manhDatId → ManhDat phải SYNCED trước
 *   4. GocCay: phụ thuộc cayTrongId → CayTrong phải tồn tại trên Firestore
 *   5. NhatKy: phụ thuộc gocCayId / cayTrongId
 *   6. ChiTiet: phụ thuộc nhatKyId → NhatKy phải tồn tại trước
 *
 *   Nếu upload ChiTiet trước NhatKy → Firestore Rules có thể chặn (nếu check parent)
 *   và về logic app: query ChiTiet mà không có NhatKy cha → orphan records.
 *
 * == Thread Safety với Room ==
 *   doWork() chạy trên WorkManager background thread (không phải main thread).
 *   Room DAO blocking calls (không phải LiveData) an toàn trên background thread.
 *   Firestore callbacks chạy trên main thread → dùng CountDownLatch để block
 *   worker thread chờ Firestore hoàn thành trước khi tiếp tục.
 *
 * == Conflict Resolution (Server Wins) ==
 *   Pull step: sau khi push xong → fetch từ Firestore về
 *   So sánh ngayTao/ngayCapNhat: nếu Firestore mới hơn → Room.insert(REPLACE)
 *   REPLACE strategy trong DAO tự overwrite bản ghi cũ theo primary key.
 *
 * == hinhAnh offline handling ==
 *   Nếu NhatKyEntity.hinhAnh bắt đầu bằng "content://" hoặc "file://"
 *   → vẫn là local URI (upload chưa thành công khi offline)
 *   → SyncWorker upload lên Storage, cập nhật hinhAnh = https:// URL
 *
 * == Lỗi thường gặp WorkManager ==
 *   1. Worker không chạy: constraints chưa thỏa (không có network)
 *      Debug: adb shell dumpsys jobscheduler | grep qlcaytrong
 *   2. Worker chạy nhiều lần: ExistingPeriodicWorkPolicy.UPDATE reset timer
 *      Fix: dùng KEEP
 *   3. Result.retry() loop vô hạn: mặc định WorkManager retry tối đa ~6 giờ
 *      Check: setBackoffCriteria để control interval
 *   4. CountDownLatch timeout 30s: Firestore chậm / offline mid-sync
 *      → entity giữ PENDING, retry lần sau
 *   5. Room accessed on main thread: Worker thread không phải main → OK
 *      Nhưng KHÔNG gọi DAO trong addOnSuccessListener trực tiếp nếu nó bị invoke
 *      trên main thread → dùng latch để return về worker thread rồi gọi DAO
 */
public class SyncWorker extends Worker {

    private static final String TAG = "SyncWorker";
    private static final int LATCH_TIMEOUT_SEC = 30;

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    // ==================== doWork ====================

    @NonNull
    @Override
    public Result doWork() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.w(TAG, "doWork: user not logged in — skip");
            return Result.success(); // Không retry nếu chưa login
        }

        String userId = user.getUid();
        AppDatabase db = AppDatabase.getInstance(getApplicationContext());
        FirestoreService fs = FirestoreService.getInstance();

        Log.d(TAG, "SyncWorker started for user: " + userId);

        boolean allOk = true;

        // === PUSH: Room → Firestore (thứ tự phụ thuộc) ===
        // 1. ManhDat (không phụ thuộc entity nào)
        allOk &= syncManhDat(db, fs);
        // 2. CayTrong (phụ thuộc ManhDat)
        allOk &= syncCayTrong(db, fs);
        // 3. GocCay (phụ thuộc CayTrong)
        allOk &= syncGocCay(db, fs);
        // 4. NhatKy (phụ thuộc GocCay/CayTrong) + upload ảnh offline
        allOk &= syncNhatKy(db, fs);
        // 5. ChiTiet (phụ thuộc NhatKy — phải sau bước 4)
        allOk &= syncChiTietTuoiPhan(db, fs);
        allOk &= syncChiTietPhunThuoc(db, fs);

        // === PULL: Firestore → Room (conflict resolution: Server Wins) ===
        // Chỉ pull nếu push thành công để tránh overwrite data chưa sync
        if (allOk) {
            pullFromFirestore(userId, db, fs);
        }

        if (allOk) {
            Log.d(TAG, "SyncWorker completed successfully");
            return Result.success();
        } else {
            Log.w(TAG, "SyncWorker partial failure — will retry with backoff");
            return Result.retry();
        }
    }

    // ==================== PUSH helpers ====================

    private boolean syncManhDat(AppDatabase db, FirestoreService fs) {
        List<ManhDatEntity> pending = db.manhDatDao().getAllPendingSync();
        if (pending.isEmpty()) return true;
        boolean ok = true;
        for (ManhDatEntity e : pending) {
            boolean synced = syncOneItem(
                listener -> fs.syncManhDat(e, listener),
                () -> { e.setSyncStatus("SYNCED"); db.manhDatDao().update(e); },
                () -> { e.setSyncStatus("FAILED"); db.manhDatDao().update(e); },
                "ManhDat/" + e.getId()
            );
            if (!synced) ok = false;
        }
        return ok;
    }

    private boolean syncCayTrong(AppDatabase db, FirestoreService fs) {
        List<CayTrongEntity> pending = db.cayTrongDao().getAllPendingSync();
        if (pending.isEmpty()) return true;
        boolean ok = true;
        for (CayTrongEntity e : pending) {
            boolean synced = syncOneItem(
                listener -> fs.syncCayTrong(e, listener),
                () -> { e.setSyncStatus("SYNCED"); db.cayTrongDao().update(e); },
                () -> { e.setSyncStatus("FAILED"); db.cayTrongDao().update(e); },
                "CayTrong/" + e.getId()
            );
            if (!synced) ok = false;
        }
        return ok;
    }

    private boolean syncGocCay(AppDatabase db, FirestoreService fs) {
        List<GocCayEntity> pending = db.gocCayDao().getAllPendingSync();
        if (pending.isEmpty()) return true;
        boolean ok = true;
        for (GocCayEntity e : pending) {
            boolean synced = syncOneItem(
                listener -> fs.syncGocCay(e, listener),
                () -> { e.setSyncStatus("SYNCED"); db.gocCayDao().update(e); },
                () -> { e.setSyncStatus("FAILED"); db.gocCayDao().update(e); },
                "GocCay/" + e.getId()
            );
            if (!synced) ok = false;
        }
        return ok;
    }

    private boolean syncNhatKy(AppDatabase db, FirestoreService fs) {
        List<NhatKyEntity> pending = db.nhatKyDao().getAllPendingSync();
        if (pending.isEmpty()) return true;
        boolean ok = true;
        for (NhatKyEntity e : pending) {
            // Upload ảnh offline nếu hinhAnh là local URI
            uploadOfflineImageIfNeeded(e, db);

            boolean synced = syncOneItem(
                listener -> fs.syncNhatKy(e, listener),
                () -> { e.setSyncStatus("SYNCED"); db.nhatKyDao().update(e); },
                () -> { e.setSyncStatus("FAILED"); db.nhatKyDao().update(e); },
                "NhatKy/" + e.getId()
            );
            if (!synced) ok = false;
        }
        return ok;
    }

    private boolean syncChiTietTuoiPhan(AppDatabase db, FirestoreService fs) {
        List<ChiTietTuoiPhanEntity> pending = db.chiTietTuoiPhanDao().getAllPendingSync();
        if (pending.isEmpty()) return true;
        boolean ok = true;
        for (ChiTietTuoiPhanEntity e : pending) {
            boolean synced = syncOneItem(
                listener -> fs.syncChiTietTuoiPhan(e, listener),
                () -> { e.setSyncStatus("SYNCED"); db.chiTietTuoiPhanDao().update(e); },
                () -> { e.setSyncStatus("FAILED"); db.chiTietTuoiPhanDao().update(e); },
                "TuoiPhan/" + e.getId()
            );
            if (!synced) ok = false;
        }
        return ok;
    }

    private boolean syncChiTietPhunThuoc(AppDatabase db, FirestoreService fs) {
        List<ChiTietPhunThuocEntity> pending = db.chiTietPhunThuocDao().getAllPendingSync();
        if (pending.isEmpty()) return true;
        boolean ok = true;
        for (ChiTietPhunThuocEntity e : pending) {
            boolean synced = syncOneItem(
                listener -> fs.syncChiTietPhunThuoc(e, listener),
                () -> { e.setSyncStatus("SYNCED"); db.chiTietPhunThuocDao().update(e); },
                () -> { e.setSyncStatus("FAILED"); db.chiTietPhunThuocDao().update(e); },
                "PhunThuoc/" + e.getId()
            );
            if (!synced) ok = false;
        }
        return ok;
    }

    // ==================== Generic sync one item ====================

    /**
     * Generic template: fire Firestore async call, block latch, call onSuccess/onFail.
     * Latch timeout = 30s per document.
     *
     * @param firestoreCall lambda gọi FirestoreService.syncXxx(listener)
     * @param onSuccess     update Room SYNCED
     * @param onFail        update Room FAILED
     * @param label         log label
     * @return true nếu sync thành công
     */
    private boolean syncOneItem(FirestoreCall firestoreCall,
                                 Runnable onSuccess, Runnable onFail, String label) {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean success = new AtomicBoolean(false);

        firestoreCall.execute(new FirestoreService.OnSyncListener() {
            @Override public void onSuccess() {
                onSuccess.run();
                success.set(true);
                latch.countDown();
            }
            @Override public void onFailure(Exception e) {
                Log.e(TAG, "Sync failed: " + label + " — " + e.getMessage());
                onFail.run();
                latch.countDown();
            }
        });

        awaitLatch(latch, label);
        return success.get();
    }

    @FunctionalInterface
    interface FirestoreCall {
        void execute(FirestoreService.OnSyncListener listener);
    }

    // ==================== Upload offline image ====================

    /**
     * Nếu hinhAnh là local URI (chưa upload khi offline) → upload ngay.
     * Blocking call trên worker thread (dùng CountDownLatch).
     */
    private void uploadOfflineImageIfNeeded(NhatKyEntity e, AppDatabase db) {
        String hinhAnh = e.getHinhAnh();
        if (hinhAnh == null) return;
        if (!hinhAnh.startsWith("content://") && !hinhAnh.startsWith("file://")) return;

        Log.d(TAG, "Uploading offline image for NhatKy: " + e.getId());

        CountDownLatch latch = new CountDownLatch(1);
        android.net.Uri uri = android.net.Uri.parse(hinhAnh);

        StorageRepository.getInstance(getApplicationContext())
            .uploadImage(uri, e.getId(), new StorageRepository.UploadCallback() {
                @Override public void onProgress(int percent) { /* worker thread — ignore */ }
                @Override public void onSuccess(String downloadUrl) {
                    e.setHinhAnh(downloadUrl);
                    db.nhatKyDao().update(e);
                    latch.countDown();
                }
                @Override public void onFailure(String error) {
                    Log.w(TAG, "Offline image upload failed: " + error + " — keep local URI");
                    latch.countDown();
                }
            });

        awaitLatch(latch, "uploadImage/" + e.getId());
    }

    // ==================== PULL: Firestore → Room ====================

    /**
     * Conflict Resolution — Server Wins strategy:
     *   Fetch tất cả data của user từ Firestore.
     *   Dùng DAO.insert(REPLACE) → nếu đã tồn tại trong Room → overwrite.
     *   Điều kiện "Server Wins": khi Room entity là SYNCED (không phải PENDING).
     *   Entity PENDING → local thay đổi chưa push → KHÔNG overwrite.
     *
     * Blocking: Tasks.await() chờ tất cả 6 collection fetch xong.
     */
    private void pullFromFirestore(String userId, AppDatabase db, FirestoreService fs) {
        Log.d(TAG, "Pulling data from Firestore for user: " + userId);
        CountDownLatch latch = new CountDownLatch(1);

        fs.fetchAllFromFirestore(userId,
            // ManhDat
            items -> { for (Map<String, Object> d : items) upsertManhDat(d, db); },
            // CayTrong
            items -> { for (Map<String, Object> d : items) upsertCayTrong(d, db); },
            // GocCay
            items -> { for (Map<String, Object> d : items) upsertGocCay(d, db); },
            // NhatKy
            items -> { for (Map<String, Object> d : items) upsertNhatKy(d, db); },
            // TuoiPhan
            items -> { for (Map<String, Object> d : items) upsertTuoiPhan(d, db); },
            // PhunThuoc
            items -> { for (Map<String, Object> d : items) upsertPhunThuoc(d, db); }
        ).addOnCompleteListener(task -> latch.countDown());

        awaitLatch(latch, "pullFromFirestore");
    }

    // ==================== Upsert from Firestore map ====================

    private void upsertManhDat(Map<String, Object> d, AppDatabase db) {
        try {
            String id = str(d, "id"); if (id == null) return;
            // Server Wins: chỉ upsert nếu Room không có PENDING record này
            ManhDatEntity existing = db.manhDatDao().getById(id);
            if (existing != null && "PENDING".equals(existing.getSyncStatus())) return;

            ManhDatEntity e = new ManhDatEntity();
            e.setId(id); e.setUserId(str(d, "userId"));
            e.setTenManhDat(str(d, "tenManhDat")); e.setDiaChi(str(d, "diaChi"));
            e.setDienTich(toDouble(d, "dienTich")); e.setDonViDienTich(str(d, "donViDienTich"));
            e.setMoTa(str(d, "moTa")); e.setNgayTao(toLong(d, "ngayTao"));
            e.setSyncStatus("SYNCED");
            db.manhDatDao().insert(e);
        } catch (Exception ex) { Log.w(TAG, "upsertManhDat error: " + ex.getMessage()); }
    }

    private void upsertCayTrong(Map<String, Object> d, AppDatabase db) {
        try {
            String id = str(d, "id"); if (id == null) return;
            CayTrongEntity existing = db.cayTrongDao().getById(id);
            if (existing != null && "PENDING".equals(existing.getSyncStatus())) return;

            CayTrongEntity e = new CayTrongEntity();
            e.setId(id); e.setManhDatId(str(d, "manhDatId")); e.setUserId(str(d, "userId"));
            e.setLoaiCay(str(d, "loaiCay")); e.setTenKhoaHoc(str(d, "tenKhoaHoc"));
            e.setSoLuong(toInt(d, "soLuong")); e.setDonViTinh(str(d, "donViTinh"));
            e.setNgayTrong(toLong(d, "ngayTrong")); e.setTrangThai(str(d, "trangThai"));
            e.setMoTa(str(d, "moTa")); e.setSyncStatus("SYNCED");
            db.cayTrongDao().insert(e);
        } catch (Exception ex) { Log.w(TAG, "upsertCayTrong error: " + ex.getMessage()); }
    }

    private void upsertGocCay(Map<String, Object> d, AppDatabase db) {
        try {
            String id = str(d, "id"); if (id == null) return;
            GocCayEntity existing = db.gocCayDao().getById(id);
            if (existing != null && "PENDING".equals(existing.getSyncStatus())) return;

            GocCayEntity e = new GocCayEntity();
            e.setId(id); e.setCayTrongId(str(d, "cayTrongId")); e.setUserId(str(d, "userId"));
            e.setMaQRCode(str(d, "maQRCode")); e.setViTri(str(d, "viTri"));
            e.setTrangThai(str(d, "trangThai")); e.setNgayTrong(toLong(d, "ngayTrong"));
            e.setGhiChu(str(d, "ghiChu")); e.setSyncStatus("SYNCED");
            db.gocCayDao().insert(e);
        } catch (Exception ex) { Log.w(TAG, "upsertGocCay error: " + ex.getMessage()); }
    }

    private void upsertNhatKy(Map<String, Object> d, AppDatabase db) {
        try {
            String id = str(d, "id"); if (id == null) return;
            NhatKyEntity existing = db.nhatKyDao().getById(id);
            if (existing != null && "PENDING".equals(existing.getSyncStatus())) return;

            NhatKyEntity e = new NhatKyEntity();
            e.setId(id); e.setGocCayId(str(d, "gocCayId")); e.setCayTrongId(str(d, "cayTrongId"));
            e.setUserId(str(d, "userId")); e.setLoaiNhatKy(str(d, "loaiNhatKy"));
            e.setNgayThucHien(toLong(d, "ngayThucHien")); e.setNguoiThucHien(str(d, "nguoiThucHien"));
            e.setHinhAnh(str(d, "hinhAnh")); e.setGhiChu(str(d, "ghiChu"));
            e.setSyncStatus("SYNCED");
            db.nhatKyDao().insert(e);
        } catch (Exception ex) { Log.w(TAG, "upsertNhatKy error: " + ex.getMessage()); }
    }

    private void upsertTuoiPhan(Map<String, Object> d, AppDatabase db) {
        try {
            String id = str(d, "id"); if (id == null) return;
            ChiTietTuoiPhanEntity existing = db.chiTietTuoiPhanDao().getById(id);
            if (existing != null && "PENDING".equals(existing.getSyncStatus())) return;

            ChiTietTuoiPhanEntity e = new ChiTietTuoiPhanEntity();
            e.setId(id); e.setNhatKyId(str(d, "nhatKyId")); e.setUserId(str(d, "userId"));
            e.setTenPhan(str(d, "tenPhan")); e.setLieuLuong(toDouble(d, "lieuLuong"));
            e.setDonVi(str(d, "donVi")); e.setCachBon(str(d, "cachBon")); e.setSyncStatus("SYNCED");
            db.chiTietTuoiPhanDao().insert(e);
        } catch (Exception ex) { Log.w(TAG, "upsertTuoiPhan error: " + ex.getMessage()); }
    }

    private void upsertPhunThuoc(Map<String, Object> d, AppDatabase db) {
        try {
            String id = str(d, "id"); if (id == null) return;
            ChiTietPhunThuocEntity existing = db.chiTietPhunThuocDao().getById(id);
            if (existing != null && "PENDING".equals(existing.getSyncStatus())) return;

            ChiTietPhunThuocEntity e = new ChiTietPhunThuocEntity();
            e.setId(id); e.setNhatKyId(str(d, "nhatKyId")); e.setUserId(str(d, "userId"));
            e.setTenThuoc(str(d, "tenThuoc")); e.setLieuLuong(toDouble(d, "lieuLuong"));
            e.setDonVi(str(d, "donVi")); e.setLyDoPhun(str(d, "lyDoPhun")); e.setSyncStatus("SYNCED");
            db.chiTietPhunThuocDao().insert(e);
        } catch (Exception ex) { Log.w(TAG, "upsertPhunThuoc error: " + ex.getMessage()); }
    }

    // ==================== Utils ====================

    private void awaitLatch(CountDownLatch latch, String label) {
        try {
            boolean done = latch.await(LATCH_TIMEOUT_SEC, TimeUnit.SECONDS);
            if (!done) Log.w(TAG, "Latch timeout: " + label);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Log.e(TAG, "Latch interrupted: " + label);
        }
    }

    private String str(Map<String, Object> m, String k) {
        Object v = m.get(k); return v != null ? v.toString() : null;
    }
    private long toLong(Map<String, Object> m, String k) {
        Object v = m.get(k); if (v == null) return 0;
        if (v instanceof Long) return (Long) v;
        if (v instanceof Number) return ((Number) v).longValue();
        try { return Long.parseLong(v.toString()); } catch (Exception e) { return 0; }
    }
    private double toDouble(Map<String, Object> m, String k) {
        Object v = m.get(k); if (v == null) return 0;
        if (v instanceof Number) return ((Number) v).doubleValue();
        try { return Double.parseDouble(v.toString()); } catch (Exception e) { return 0; }
    }
    private int toInt(Map<String, Object> m, String k) {
        Object v = m.get(k); if (v == null) return 0;
        if (v instanceof Number) return ((Number) v).intValue();
        try { return Integer.parseInt(v.toString()); } catch (Exception e) { return 0; }
    }
}
