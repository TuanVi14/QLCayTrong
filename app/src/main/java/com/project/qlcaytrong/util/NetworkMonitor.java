// File: app/src/main/java/com/project/qlcaytrong/util/NetworkMonitor.java
package com.project.qlcaytrong.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.project.qlcaytrong.sync.SyncManager;

/**
 * NetworkMonitor — theo dõi trạng thái mạng và trigger sync tự động.
 *
 * == Tại sao không chỉ dùng WorkManager constraints? ==
 *   WorkManager CONNECTED constraint đảm bảo work KHÔNG chạy khi offline.
 *   Nhưng không trigger ngay khi network restored — phải chờ hết interval.
 *   NetworkMonitor bổ sung: triggerImmediateSync() ngay khi network available.
 *
 * == NetworkCallback vs BroadcastReceiver ==
 *   BroadcastReceiver CONNECTIVITY_CHANGE bị deprecated từ Android 7.
 *   ConnectivityManager.NetworkCallback là API hiện đại, chính xác hơn.
 *   Không cần đăng ký trong Manifest — đăng ký trong code → tự clean up.
 *
 * == isNetworkAvailable() ==
 *   Kiểm tra hasCapability(INTERNET) + hasCapability(VALIDATED).
 *   VALIDATED = Android đã verify có internet thật (ping google.com thành công).
 *   Tránh captive portal (WiFi khách sạn chưa login) bị nhận nhầm là có mạng.
 *
 * == Banner "Offline mode" ==
 *   isOnline LiveData → observe trong BaseActivity hoặc từng Activity cần hiển thị.
 *   postValue phải dùng (callback từ OS thread, không phải main thread).
 */
public class NetworkMonitor {

    private static final String TAG = "NetworkMonitor";

    private final Context appContext;
    private final ConnectivityManager connectivityManager;
    private final MutableLiveData<Boolean> isOnlineLiveData = new MutableLiveData<>(false);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private ConnectivityManager.NetworkCallback networkCallback;

    private static volatile NetworkMonitor INSTANCE;

    public static NetworkMonitor getInstance(Context context) {
        if (INSTANCE == null) synchronized (NetworkMonitor.class) {
            if (INSTANCE == null) INSTANCE = new NetworkMonitor(context.getApplicationContext());
        }
        return INSTANCE;
    }

    private NetworkMonitor(Context context) {
        this.appContext = context;
        this.connectivityManager =
            (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        // Set initial state
        isOnlineLiveData.postValue(isNetworkAvailable());
    }

    // ==================== Start / Stop ====================

    /**
     * Bắt đầu theo dõi mạng. Gọi trong Application.onCreate().
     */
    public void startMonitoring() {
        if (networkCallback != null) return; // Already monitoring

        NetworkRequest request = new NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build();

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                Log.d(TAG, "Network available — triggering sync");
                isOnlineLiveData.postValue(true);
                // Delay nhỏ để network ổn định trước khi sync
                mainHandler.postDelayed(() ->
                    SyncManager.triggerImmediateSync(appContext), 1500);
            }

            @Override
            public void onLost(@NonNull Network network) {
                // Kiểm tra lại xem còn network nào khác không (WiFi + Mobile)
                boolean stillOnline = isNetworkAvailable();
                Log.d(TAG, "Network lost. Still online: " + stillOnline);
                isOnlineLiveData.postValue(stillOnline);
            }

            @Override
            public void onCapabilitiesChanged(@NonNull Network network,
                                               @NonNull NetworkCapabilities caps) {
                boolean validated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
                isOnlineLiveData.postValue(validated);
            }
        };

        connectivityManager.registerNetworkCallback(request, networkCallback);
        Log.d(TAG, "NetworkMonitor started");
    }

    /**
     * Dừng theo dõi. Gọi khi user logout để tránh leak.
     */
    public void stopMonitoring() {
        if (networkCallback != null) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            } catch (Exception e) {
                Log.w(TAG, "unregisterNetworkCallback error: " + e.getMessage());
            }
            networkCallback = null;
            Log.d(TAG, "NetworkMonitor stopped");
        }
    }

    // ==================== Public API ====================

    /** LiveData<Boolean> — true = online, false = offline */
    public LiveData<Boolean> getIsOnline() { return isOnlineLiveData; }

    /** Kiểm tra ngay (blocking, synchronous) */
    public boolean isNetworkAvailable() {
        if (connectivityManager == null) return false;
        Network active = connectivityManager.getActiveNetwork();
        if (active == null) return false;
        NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(active);
        return caps != null
            && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }
}
