// File: app/src/main/java/com/project/qlcaytrong/QLCayTrongApp.java
package com.project.qlcaytrong;

import android.app.Application;
import android.util.Log;

import androidx.work.Configuration;

import com.google.firebase.auth.FirebaseAuth;
import com.project.qlcaytrong.sync.SyncManager;
import com.project.qlcaytrong.util.NetworkMonitor;

/**
 * Application class — entry point toàn cục.
 *
 * Phải đăng ký trong AndroidManifest.xml:
 *   android:name=".QLCayTrongApp"
 *
 * == Tại sao khởi tạo WorkManager ở đây? ==
 *   WorkManager cần được configure trước khi bất kỳ Activity nào dùng.
 *   Application.onCreate() là nơi duy nhất đảm bảo chạy trước tất cả.
 *
 * == WorkManager Configuration ==
 *   Mặc định WorkManager dùng ContentProvider để tự-init → không cần config.
 *   Custom config hữu ích để:
 *   - setMinimumLoggingLevel(Log.DEBUG): debug log trong dev build
 *   - setWorkerFactory(): inject dependencies vào Worker (Dagger/Hilt)
 *   Nếu custom config: phải thêm tools:node="remove" trong Manifest để tắt auto-init.
 *
 * == NetworkMonitor lifecycle ==
 *   startMonitoring() ở Application.onCreate() → chạy suốt app lifetime.
 *   stopMonitoring() không cần gọi ở đây (app process die = cleanup tự động).
 *   Chỉ cần stopMonitoring() khi muốn tắt giữa chừng (vd: user logout + stop sync).
 */
public class QLCayTrongApp extends Application implements Configuration.Provider {

    private static final String TAG = "QLCayTrongApp";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Application started");

        // Bắt đầu monitor network ngay từ đầu (kể cả trước login)
        NetworkMonitor.getInstance(this).startMonitoring();

        // Đăng ký WorkManager periodic sync nếu user đã login
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            SyncManager.schedulePeriodicSync(this);
            Log.d(TAG, "Periodic sync scheduled (user already logged in)");
        }
        // Nếu chưa login → AuthRepository.login() sẽ gọi SyncManager.schedulePeriodicSync()
    }

    /**
     * Custom WorkManager configuration.
     * DEBUG logging để dễ trace trong logcat.
     *
     * Trong Manifest, thêm vào <application>:
     *   <provider
     *     android:name="androidx.startup.InitializationProvider"
     *     android:authorities="${applicationId}.androidx-startup"
     *     android:exported="false"
     *     tools:node="merge">
     *     <meta-data android:name="androidx.work.WorkManagerInitializer"
     *       android:value="androidx.startup"
     *       tools:node="remove" />
     *   </provider>
     */
    @Override
    public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder()
            .setMinimumLoggingLevel(Log.DEBUG)
            .build();
    }
}
