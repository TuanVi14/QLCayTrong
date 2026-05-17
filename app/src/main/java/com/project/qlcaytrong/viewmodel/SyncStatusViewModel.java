// File: app/src/main/java/com/project/qlcaytrong/viewmodel/SyncStatusViewModel.java
package com.project.qlcaytrong.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.Transformations;
import androidx.work.WorkInfo;

import com.project.qlcaytrong.sync.SyncManager;
import com.project.qlcaytrong.util.NetworkMonitor;

import java.util.List;

/**
 * SyncStatusViewModel — expose sync state và network state cho UI.
 *
 * Dùng trong MainActivity hoặc BaseActivity để:
 *   1. Hiển thị badge "Đang đồng bộ..." / "Đã đồng bộ"
 *   2. Hiển thị banner "Offline mode" khi mất mạng
 *   3. Đếm số record PENDING (optional — từ Room query)
 *
 * == Sử dụng trong Activity ==
 *   SyncStatusViewModel vm = new ViewModelProvider(this).get(SyncStatusViewModel.class);
 *
 *   // Badge sync
 *   vm.getSyncState().observe(this, state -> {
 *       binding.badgeSync.setVisibility(state == SyncState.RUNNING ? VISIBLE : GONE);
 *       binding.tvSyncStatus.setText(state.label);
 *   });
 *
 *   // Offline banner
 *   vm.getIsOnline().observe(this, online -> {
 *       binding.bannerOffline.setVisibility(online ? GONE : VISIBLE);
 *   });
 */
public class SyncStatusViewModel extends AndroidViewModel {

    /** Enum trạng thái sync đơn giản cho UI */
    public enum SyncState {
        IDLE(""),
        RUNNING("Đang đồng bộ..."),
        SUCCESS("Đã đồng bộ ✓"),
        FAILED("Đồng bộ thất bại"),
        BLOCKED("Chờ mạng...");

        public final String label;
        SyncState(String l) { this.label = l; }
    }

    private final LiveData<List<WorkInfo>> rawWorkInfo;
    private final LiveData<SyncState> syncState;
    private final LiveData<Boolean> isOnline;

    public SyncStatusViewModel(@NonNull Application app) {
        super(app);

        // WorkManager LiveData — emit mỗi khi WorkInfo thay đổi
        rawWorkInfo = SyncManager.observeSyncState(app);

        // Map List<WorkInfo> → SyncState
        syncState = Transformations.map(rawWorkInfo, infos -> {
            if (infos == null || infos.isEmpty()) return SyncState.IDLE;
            // Kiểm tra trạng thái ưu tiên: RUNNING > BLOCKED > FAILED > SUCCEEDED
            for (WorkInfo info : infos) {
                if (info.getState() == WorkInfo.State.RUNNING)  return SyncState.RUNNING;
            }
            for (WorkInfo info : infos) {
                if (info.getState() == WorkInfo.State.BLOCKED)  return SyncState.BLOCKED;
            }
            for (WorkInfo info : infos) {
                if (info.getState() == WorkInfo.State.FAILED)   return SyncState.FAILED;
            }
            for (WorkInfo info : infos) {
                if (info.getState() == WorkInfo.State.SUCCEEDED) return SyncState.SUCCESS;
            }
            return SyncState.IDLE;
        });

        // Network state từ NetworkMonitor singleton
        isOnline = NetworkMonitor.getInstance(app).getIsOnline();
    }

    public LiveData<SyncState> getSyncState() { return syncState; }
    public LiveData<Boolean> getIsOnline()     { return isOnline; }
}
