// File: app/src/main/java/com/project/qlcaytrong/sync/SyncManager.java
package com.project.qlcaytrong.sync;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * SyncManager — điều phối toàn bộ WorkManager schedule + OneTime trigger.
 *
 * == Tại sao tách khỏi SyncWorker? ==
 *   SyncWorker.doWork() chạy trên background thread — không nên lẫn logic schedule.
 *   SyncManager là singleton stateless, gọi từ Application / Activity / NetworkMonitor.
 *
 * == Hai loại work ==
 *   PeriodicWork (15 phút): chạy định kỳ background, persist qua reboot.
 *   OneTimeWork (immediate): trigger ngay khi app mở hoặc network restored.
 *   Cả hai dùng cùng constraint CONNECTED → không tốn pin khi offline.
 *
 * == Exponential Backoff ==
 *   Khi doWork() trả Result.retry() → WorkManager chờ backoffDelay * 2^attempt.
 *   Initial = 30s, max = 5 phút (WorkManager cap).
 *   Tránh spam Firestore khi server tạm lỗi.
 *
 * == ExistingPeriodicWorkPolicy.KEEP vs UPDATE ==
 *   KEEP: giữ lịch cũ (không reset timer) — tránh trường hợp schedule liên tục reset
 *         khi app mở nhiều lần → work không bao giờ chạy.
 *   UPDATE: cập nhật constraints/params nhưng giữ timer — dùng khi thay đổi config.
 *
 * == ExistingWorkPolicy.REPLACE cho OneTime ==
 *   Nếu đang có OneTime work pending (network vừa restored, chưa chạy xong)
 *   và app mở lại → REPLACE để trigger fresh work thay vì chờ cái cũ.
 */
public class SyncManager {

    private static final String TAG = "SyncManager";

    // WorkManager unique work names — dùng để cancel/query theo tên
    public static final String WORK_PERIODIC = "qlcaytrong_sync_periodic";
    public static final String WORK_ONETIME  = "qlcaytrong_sync_onetime";
    public static final String WORK_TAG      = "qlcaytrong_sync";

    private static final int  BACKOFF_INITIAL_SECONDS = 30;
    private static final long PERIODIC_INTERVAL_MIN   = 15;

    private SyncManager() {}

    // ==================== Schedule ====================

    /**
     * Đăng ký PeriodicWork 15 phút.
     * Gọi 1 lần trong: Application.onCreate() (sau khi user đã login).
     */
    public static void schedulePeriodicSync(Context ctx) {
        Constraints constraints = networkConstraint();

        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                SyncWorker.class, PERIODIC_INTERVAL_MIN, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL,
                BACKOFF_INITIAL_SECONDS, TimeUnit.SECONDS)
            .addTag(WORK_TAG)
            .build();

        WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
            WORK_PERIODIC,
            ExistingPeriodicWorkPolicy.KEEP,
            request);

        Log.d(TAG, "Periodic sync scheduled (15 min, network required)");
    }

    /**
     * Trigger sync ngay lập tức (OneTimeWork).
     * Gọi khi: app mở, network restored, sau khi save data penting.
     */
    public static void triggerImmediateSync(Context ctx) {
        Constraints constraints = networkConstraint();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(SyncWorker.class)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL,
                BACKOFF_INITIAL_SECONDS, TimeUnit.SECONDS)
            .addTag(WORK_TAG)
            .build();

        WorkManager.getInstance(ctx).enqueueUniqueWork(
            WORK_ONETIME,
            ExistingWorkPolicy.REPLACE,   // hủy pending work cũ, queue mới
            request);

        Log.d(TAG, "One-time sync triggered");
    }

    /**
     * Hủy tất cả sync work — gọi khi user logout.
     */
    public static void cancelAll(Context ctx) {
        WorkManager.getInstance(ctx).cancelAllWorkByTag(WORK_TAG);
        Log.d(TAG, "All sync work cancelled");
    }

    // ==================== Observe ====================

    /**
     * LiveData<List<WorkInfo>> để observe trạng thái sync trong UI.
     * Observer trong Activity/Fragment:
     *   syncManager.observeSyncState(this).observe(this, infos -> {
     *     boolean running = infos.stream().anyMatch(i -> i.getState() == RUNNING);
     *   });
     */
    public static LiveData<List<WorkInfo>> observeSyncState(Context ctx) {
        return WorkManager.getInstance(ctx).getWorkInfosByTagLiveData(WORK_TAG);
    }

    // ==================== Helper ====================

    private static Constraints networkConstraint() {
        return new Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build();
    }
}
