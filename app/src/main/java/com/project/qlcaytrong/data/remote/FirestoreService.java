// File: app/src/main/java/com/project/qlcaytrong/data/remote/FirestoreService.java
package com.project.qlcaytrong.data.remote;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import com.project.qlcaytrong.data.local.entity.CayTrongEntity;
import com.project.qlcaytrong.data.local.entity.ChiTietPhunThuocEntity;
import com.project.qlcaytrong.data.local.entity.ChiTietTuoiPhanEntity;
import com.project.qlcaytrong.data.local.entity.GocCayEntity;
import com.project.qlcaytrong.data.local.entity.ManhDatEntity;
import com.project.qlcaytrong.data.local.entity.NhatKyEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirestoreService {

    // ==================== Collection names ====================
    public static final String COL_MANH_DAT          = "manhdat";
    public static final String COL_CAY_TRONG          = "caytrong";
    public static final String COL_GOC_CAY            = "goccay";
    public static final String COL_NHAT_KY            = "nhatky";
    public static final String COL_CHI_TIET_TUOI_PHAN = "chitiettuoiphan";
    public static final String COL_CHI_TIET_PHUN_THUOC = "chitietphunthuoc";

    // ==================== Singleton ====================
    private static volatile FirestoreService INSTANCE;
    private final FirebaseFirestore db;

    private FirestoreService() {
        db = FirebaseFirestore.getInstance();
    }

    public static FirestoreService getInstance() {
        if (INSTANCE == null) {
            synchronized (FirestoreService.class) {
                if (INSTANCE == null) {
                    INSTANCE = new FirestoreService();
                }
            }
        }
        return INSTANCE;
    }

    // ==================== Listener Interface ====================

    public interface OnSyncListener {
        void onSuccess();
        default void onFailure(Exception e) {}
    }

    public interface OnFetchListener<T> {
        void onSuccess(List<T> items);
        default void onFailure(Exception e) {}
    }

    // ==================== Helper ====================

    private CollectionReference col(String name) {
        return db.collection(name);
    }

    // ==================== MANH DAT ====================

    public void syncManhDat(ManhDatEntity entity, OnSyncListener listener) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", entity.getId());
        data.put("userId", entity.getUserId());
        data.put("tenManhDat", entity.getTenManhDat());
        data.put("diaChi", entity.getDiaChi());
        data.put("dienTich", entity.getDienTich());
        data.put("donViDienTich", entity.getDonViDienTich());
        data.put("moTa", entity.getMoTa());
        data.put("ngayTao", entity.getNgayTao());

        col(COL_MANH_DAT).document(entity.getId())
            .set(data, SetOptions.merge())
            .addOnSuccessListener(aVoid -> listener.onSuccess())
            .addOnFailureListener(listener::onFailure);
    }

    public void deleteManhDat(String id, OnSyncListener listener) {
        col(COL_MANH_DAT).document(id)
            .delete()
            .addOnSuccessListener(aVoid -> listener.onSuccess())
            .addOnFailureListener(listener::onFailure);
    }

    public void fetchManhDatByUserId(String userId, OnFetchListener<Map<String, Object>> listener) {
        col(COL_MANH_DAT).whereEqualTo("userId", userId).get()
            .addOnSuccessListener(querySnapshot -> {
                List<Map<String, Object>> result = new ArrayList<>();
                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    if (doc.getData() != null) result.add(doc.getData());
                }
                listener.onSuccess(result);
            })
            .addOnFailureListener(listener::onFailure);
    }

    // ==================== CAY TRONG ====================

    public void syncCayTrong(CayTrongEntity entity, OnSyncListener listener) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", entity.getId());
        data.put("manhDatId", entity.getManhDatId());
        data.put("userId", entity.getUserId());
        data.put("loaiCay", entity.getLoaiCay());
        data.put("tenKhoaHoc", entity.getTenKhoaHoc());
        data.put("soLuong", entity.getSoLuong());
        data.put("donViTinh", entity.getDonViTinh());
        data.put("ngayTrong", entity.getNgayTrong());
        data.put("trangThai", entity.getTrangThai());
        data.put("moTa", entity.getMoTa());

        col(COL_CAY_TRONG).document(entity.getId())
            .set(data, SetOptions.merge())
            .addOnSuccessListener(aVoid -> listener.onSuccess())
            .addOnFailureListener(listener::onFailure);
    }

    public void deleteCayTrong(String id, OnSyncListener listener) {
        col(COL_CAY_TRONG).document(id)
            .delete()
            .addOnSuccessListener(aVoid -> listener.onSuccess())
            .addOnFailureListener(listener::onFailure);
    }

    public void fetchCayTrongByUserId(String userId, OnFetchListener<Map<String, Object>> listener) {
        col(COL_CAY_TRONG).whereEqualTo("userId", userId).get()
            .addOnSuccessListener(querySnapshot -> {
                List<Map<String, Object>> result = new ArrayList<>();
                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    if (doc.getData() != null) result.add(doc.getData());
                }
                listener.onSuccess(result);
            })
            .addOnFailureListener(listener::onFailure);
    }

    // ==================== GOC CAY ====================

    public void syncGocCay(GocCayEntity entity, OnSyncListener listener) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", entity.getId());
        data.put("cayTrongId", entity.getCayTrongId());
        data.put("userId", entity.getUserId());
        data.put("maQRCode", entity.getMaQRCode());
        data.put("viTri", entity.getViTri());
        data.put("trangThai", entity.getTrangThai());
        data.put("ngayTrong", entity.getNgayTrong());
        data.put("ghiChu", entity.getGhiChu());

        col(COL_GOC_CAY).document(entity.getId())
            .set(data, SetOptions.merge())
            .addOnSuccessListener(aVoid -> listener.onSuccess())
            .addOnFailureListener(listener::onFailure);
    }

    public void deleteGocCay(String id, OnSyncListener listener) {
        col(COL_GOC_CAY).document(id)
            .delete()
            .addOnSuccessListener(aVoid -> listener.onSuccess())
            .addOnFailureListener(listener::onFailure);
    }

    public void fetchGocCayByUserId(String userId, OnFetchListener<Map<String, Object>> listener) {
        col(COL_GOC_CAY).whereEqualTo("userId", userId).get()
            .addOnSuccessListener(querySnapshot -> {
                List<Map<String, Object>> result = new ArrayList<>();
                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    if (doc.getData() != null) result.add(doc.getData());
                }
                listener.onSuccess(result);
            })
            .addOnFailureListener(listener::onFailure);
    }

    // ==================== NHAT KY ====================

    public void syncNhatKy(NhatKyEntity entity, OnSyncListener listener) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", entity.getId());
        data.put("gocCayId", entity.getGocCayId());
        data.put("cayTrongId", entity.getCayTrongId());
        data.put("userId", entity.getUserId());
        data.put("loaiNhatKy", entity.getLoaiNhatKy());
        data.put("ngayThucHien", entity.getNgayThucHien());
        data.put("nguoiThucHien", entity.getNguoiThucHien());
        data.put("hinhAnh", entity.getHinhAnh());
        data.put("ghiChu", entity.getGhiChu());

        col(COL_NHAT_KY).document(entity.getId())
            .set(data, SetOptions.merge())
            .addOnSuccessListener(aVoid -> listener.onSuccess())
            .addOnFailureListener(listener::onFailure);
    }

    public void deleteNhatKy(String id, OnSyncListener listener) {
        col(COL_NHAT_KY).document(id)
            .delete()
            .addOnSuccessListener(aVoid -> listener.onSuccess())
            .addOnFailureListener(listener::onFailure);
    }

    public void fetchNhatKyByUserId(String userId, OnFetchListener<Map<String, Object>> listener) {
        col(COL_NHAT_KY).whereEqualTo("userId", userId).get()
            .addOnSuccessListener(querySnapshot -> {
                List<Map<String, Object>> result = new ArrayList<>();
                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    if (doc.getData() != null) result.add(doc.getData());
                }
                listener.onSuccess(result);
            })
            .addOnFailureListener(listener::onFailure);
    }

    // ==================== CHI TIET TUOI PHAN ====================

    public void syncChiTietTuoiPhan(ChiTietTuoiPhanEntity entity, OnSyncListener listener) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", entity.getId());
        data.put("nhatKyId", entity.getNhatKyId());
        data.put("userId", entity.getUserId());
        data.put("tenPhan", entity.getTenPhan());
        data.put("lieuLuong", entity.getLieuLuong());
        data.put("donVi", entity.getDonVi());
        data.put("cachBon", entity.getCachBon());

        col(COL_CHI_TIET_TUOI_PHAN).document(entity.getId())
            .set(data, SetOptions.merge())
            .addOnSuccessListener(aVoid -> listener.onSuccess())
            .addOnFailureListener(listener::onFailure);
    }

    public void fetchChiTietTuoiPhanByUserId(String userId, OnFetchListener<Map<String, Object>> listener) {
        col(COL_CHI_TIET_TUOI_PHAN).whereEqualTo("userId", userId).get()
            .addOnSuccessListener(querySnapshot -> {
                List<Map<String, Object>> result = new ArrayList<>();
                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    if (doc.getData() != null) result.add(doc.getData());
                }
                listener.onSuccess(result);
            })
            .addOnFailureListener(listener::onFailure);
    }

    // ==================== CHI TIET PHUN THUOC ====================

    public void syncChiTietPhunThuoc(ChiTietPhunThuocEntity entity, OnSyncListener listener) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", entity.getId());
        data.put("nhatKyId", entity.getNhatKyId());
        data.put("userId", entity.getUserId());
        data.put("tenThuoc", entity.getTenThuoc());
        data.put("lieuLuong", entity.getLieuLuong());
        data.put("donVi", entity.getDonVi());
        data.put("lyDoPhun", entity.getLyDoPhun());

        col(COL_CHI_TIET_PHUN_THUOC).document(entity.getId())
            .set(data, SetOptions.merge())
            .addOnSuccessListener(aVoid -> listener.onSuccess())
            .addOnFailureListener(listener::onFailure);
    }

    public void fetchChiTietPhunThuocByUserId(String userId, OnFetchListener<Map<String, Object>> listener) {
        col(COL_CHI_TIET_PHUN_THUOC).whereEqualTo("userId", userId).get()
            .addOnSuccessListener(querySnapshot -> {
                List<Map<String, Object>> result = new ArrayList<>();
                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    if (doc.getData() != null) result.add(doc.getData());
                }
                listener.onSuccess(result);
            })
            .addOnFailureListener(listener::onFailure);
    }

    // ==================== BATCH FETCH ALL ====================

    /**
     * Kéo toàn bộ data của user về từ Firestore (tất cả collections).
     * Trả về Task<Void> để caller có thể chain hoặc await.
     */
    public Task<Void> fetchAllFromFirestore(String userId,
                                            OnFetchListener<Map<String, Object>> manhDatListener,
                                            OnFetchListener<Map<String, Object>> cayTrongListener,
                                            OnFetchListener<Map<String, Object>> gocCayListener,
                                            OnFetchListener<Map<String, Object>> nhatKyListener,
                                            OnFetchListener<Map<String, Object>> tuoiPhanListener,
                                            OnFetchListener<Map<String, Object>> phunThuocListener) {

        Task<QuerySnapshot> t1 = col(COL_MANH_DAT).whereEqualTo("userId", userId).get();
        Task<QuerySnapshot> t2 = col(COL_CAY_TRONG).whereEqualTo("userId", userId).get();
        Task<QuerySnapshot> t3 = col(COL_GOC_CAY).whereEqualTo("userId", userId).get();
        Task<QuerySnapshot> t4 = col(COL_NHAT_KY).whereEqualTo("userId", userId).get();
        Task<QuerySnapshot> t5 = col(COL_CHI_TIET_TUOI_PHAN).whereEqualTo("userId", userId).get();
        Task<QuerySnapshot> t6 = col(COL_CHI_TIET_PHUN_THUOC).whereEqualTo("userId", userId).get();

        return Tasks.whenAll(t1, t2, t3, t4, t5, t6)
            .addOnSuccessListener(aVoid -> {
                deliverDocs(t1, manhDatListener);
                deliverDocs(t2, cayTrongListener);
                deliverDocs(t3, gocCayListener);
                deliverDocs(t4, nhatKyListener);
                deliverDocs(t5, tuoiPhanListener);
                deliverDocs(t6, phunThuocListener);
            });
    }

    private void deliverDocs(Task<QuerySnapshot> task,
                             OnFetchListener<Map<String, Object>> listener) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (task.isSuccessful() && task.getResult() != null) {
            for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                if (doc.getData() != null) result.add(doc.getData());
            }
            listener.onSuccess(result);
        } else {
            Exception e = task.getException();
            listener.onFailure(e != null ? e : new Exception("Unknown Firestore error"));
        }
    }
}
