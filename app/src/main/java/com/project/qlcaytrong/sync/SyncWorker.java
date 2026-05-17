// File: app/src/main/java/com/project/qlcaytrong/sync/SyncWorker.java
package com.project.qlcaytrong.sync;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.tasks.Tasks;
import com.project.qlcaytrong.data.local.AppDatabase;
import com.project.qlcaytrong.data.local.entity.CayTrongEntity;
import com.project.qlcaytrong.data.local.entity.ChiTietPhunThuocEntity;
import com.project.qlcaytrong.data.local.entity.ChiTietTuoiPhanEntity;
import com.project.qlcaytrong.data.local.entity.GocCayEntity;
import com.project.qlcaytrong.data.local.entity.ManhDatEntity;
import com.project.qlcaytrong.data.local.entity.NhatKyEntity;
import com.project.qlcaytrong.data.remote.FirestoreService;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class SyncWorker extends Worker {

    private static final String TAG = "SyncWorker";
    private static final String WORK_NAME = "qlcaytrong_periodic_sync";

    // ==================== Constructor ====================

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    // ==================== doWork ====================

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "SyncWorker started");

        AppDatabase db = AppDatabase.getInstance(getApplicationContext());
        FirestoreService firestoreService = FirestoreService.getInstance();

        boolean allSuccess = true;

        // --- ManhDat ---
        allSuccess &= syncManhDat(db, firestoreService);

        // --- CayTrong ---
        allSuccess &= syncCayTrong(db, firestoreService);

        // --- GocCay ---
        allSuccess &= syncGocCay(db, firestoreService);

        // --- NhatKy ---
        allSuccess &= syncNhatKy(db, firestoreService);

        // --- ChiTietTuoiPhan ---
        allSuccess &= syncChiTietTuoiPhan(db, firestoreService);

        // --- ChiTietPhunThuoc ---
        allSuccess &= syncChiTietPhunThuoc(db, firestoreService);

        if (allSuccess) {
            Log.d(TAG, "SyncWorker completed successfully");
            return Result.success();
        } else {
            Log.w(TAG, "SyncWorker completed with some failures — will retry");
            return Result.retry();
        }
    }

    // ==================== Sync helpers per entity ====================

    private boolean syncManhDat(AppDatabase db, FirestoreService fs) {
        List<ManhDatEntity> pending = db.manhDatDao().getAllPendingSync();
        if (pending.isEmpty()) return true;

        boolean success = true;
        for (ManhDatEntity entity : pending) {
            AtomicBoolean ok = new AtomicBoolean(false);
            CountDownLatch latch = new CountDownLatch(1);

            fs.syncManhDat(entity, new FirestoreService.OnSyncListener() {
                @Override
                public void onSuccess() {
                    entity.setSyncStatus("SYNCED");
                    db.manhDatDao().update(entity);
                    ok.set(true);
                    latch.countDown();
                }

                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, "ManhDat sync failed: " + entity.getId(), e);
                    entity.setSyncStatus("FAILED");
                    db.manhDatDao().update(entity);
                    latch.countDown();
                }
            });

            awaitLatch(latch);
            if (!ok.get()) success = false;
        }
        return success;
    }

    private boolean syncCayTrong(AppDatabase db, FirestoreService fs) {
        List<CayTrongEntity> pending = db.cayTrongDao().getAllPendingSync();
        if (pending.isEmpty()) return true;

        boolean success = true;
        for (CayTrongEntity entity : pending) {
            AtomicBoolean ok = new AtomicBoolean(false);
            CountDownLatch latch = new CountDownLatch(1);

            fs.syncCayTrong(entity, new FirestoreService.OnSyncListener() {
                @Override
                public void onSuccess() {
                    entity.setSyncStatus("SYNCED");
                    db.cayTrongDao().update(entity);
                    ok.set(true);
                    latch.countDown();
                }

                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, "CayTrong sync failed: " + entity.getId(), e);
                    entity.setSyncStatus("FAILED");
                    db.cayTrongDao().update(entity);
                    latch.countDown();
                }
            });

            awaitLatch(latch);
            if (!ok.get()) success = false;
        }
        return success;
    }

    private boolean syncGocCay(AppDatabase db, FirestoreService fs) {
        List<GocCayEntity> pending = db.gocCayDao().getAllPendingSync();
        if (pending.isEmpty()) return true;

        boolean success = true;
        for (GocCayEntity entity : pending) {
            AtomicBoolean ok = new AtomicBoolean(false);
            CountDownLatch latch = new CountDownLatch(1);

            fs.syncGocCay(entity, new FirestoreService.OnSyncListener() {
                @Override
                public void onSuccess() {
                    entity.setSyncStatus("SYNCED");
                    db.gocCayDao().update(entity);
                    ok.set(true);
                    latch.countDown();
                }

                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, "GocCay sync failed: " + entity.getId(), e);
                    entity.setSyncStatus("FAILED");
                    db.gocCayDao().update(entity);
                    latch.countDown();
                }
            });

            awaitLatch(latch);
            if (!ok.get()) success = false;
        }
        return success;
    }

    private boolean syncNhatKy(AppDatabase db, FirestoreService fs) {
        List<NhatKyEntity> pending = db.nhatKyDao().getAllPendingSync();
        if (pending.isEmpty()) return true;

        boolean success = true;
        for (NhatKyEntity entity : pending) {
            AtomicBoolean ok = new AtomicBoolean(false);
            CountDownLatch latch = new CountDownLatch(1);

            fs.syncNhatKy(entity, new FirestoreService.OnSyncListener() {
                @Override
                public void onSuccess() {
                    entity.setSyncStatus("SYNCED");
                    db.nhatKyDao().update(entity);
                    ok.set(true);
                    latch.countDown();
                }

                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, "NhatKy sync failed: " + entity.getId(), e);
                    entity.setSyncStatus("FAILED");
                    db.nhatKyDao().update(entity);
                    latch.countDown();
                }
            });

            awaitLatch(latch);
            if (!ok.get()) success = false;
        }
        return success;
    }

    private boolean syncChiTietTuoiPhan(AppDatabase db, FirestoreService fs) {
        List<ChiTietTuoiPhanEntity> pending = db.chiTietTuoiPhanDao().getAllPendingSync();
        if (pending.isEmpty()) return true;

        boolean success = true;
        for (ChiTietTuoiPhanEntity entity : pending) {
            AtomicBoolean ok = new AtomicBoolean(false);
            CountDownLatch latch = new CountDownLatch(1);

            fs.syncChiTietTuoiPhan(entity, new FirestoreService.OnSyncListener() {
                @Override
                public void onSuccess() {
                    entity.setSyncStatus("SYNCED");
                    db.chiTietTuoiPhanDao().update(entity);
                    ok.set(true);
                    latch.countDown();
                }

                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, "ChiTietTuoiPhan sync failed: " + entity.getId(), e);
                    entity.setSyncStatus("FAILED");
                    db.chiTietTuoiPhanDao().update(entity);
                    latch.countDown();
                }
            });

            awaitLatch(latch);
            if (!ok.get()) success = false;
        }
        return success;
    }

    private boolean syncChiTietPhunThuoc(AppDatabase db, FirestoreService fs) {
        List<ChiTietPhunThuocEntity> pending = db.chiTietPhunThuocDao().getAllPendingSync();
        if (pending.isEmpty()) return true;

        boolean success = true;
        for (ChiTietPhunThuocEntity entity : pending) {
            AtomicBoolean ok = new AtomicBoolean(false);
            CountDownLatch latch = new CountDownLatch(1);

            fs.syncChiTietPhunThuoc(entity, new FirestoreService.OnSyncListener() {
                @Override
                public void onSuccess() {
                    entity.setSyncStatus("SYNCED");
                    db.chiTietPhunThuocDao().update(entity);
                    ok.set(true);
                    latch.countDown();
                }

                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, "ChiTietPhunThuoc sync failed: " + entity.getId(), e);
                    entity.setSyncStatus("FAILED");
                    db.chiTietPhunThuocDao().update(entity);
                    latch.countDown();
                }
            });

            awaitLatch(latch);
            if (!ok.get()) success = false;
        }
        return success;
    }

    // ==================== Latch helper ====================

    private void awaitLatch(CountDownLatch latch) {
        try {
            // Timeout 30s per document to avoid deadlock
            latch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Log.e(TAG, "Latch interrupted", e);
        }
    }

    // ==================== Schedule ====================

    /**
     * Đăng ký SyncWorker chạy định kỳ mỗi 15 phút, chỉ khi có kết nối mạng.
     * Gọi method này một lần duy nhất trong Application.onCreate() hoặc sau khi login.
     *
     * @param context Application context
     */
    public static void schedulePeriodicSync(@NonNull Context context) {
        Constraints constraints = new Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build();

        PeriodicWorkRequest syncRequest =
            new PeriodicWorkRequest.Builder(SyncWorker.class, 15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .addTag(WORK_NAME)
                .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,   // Giữ lịch cũ nếu đã có
            syncRequest
        );

        Log.d(TAG, "Periodic sync scheduled every 15 minutes (network required)");
    }

    /**
     * Hủy lịch sync (ví dụ khi user logout).
     *
     * @param context Application context
     */
    public static void cancelPeriodicSync(@NonNull Context context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME);
        Log.d(TAG, "Periodic sync cancelled");
    }
}
