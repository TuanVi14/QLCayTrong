// File: app/src/main/java/com/project/qlcaytrong/ui/qr/CustomCaptureActivity.java
package com.project.qlcaytrong.ui.qr;

import com.journeyapps.barcodescanner.CaptureActivity;

/**
 * CustomCaptureActivity — extends ZXing CaptureActivity để dùng layout tùy chỉnh.
 *
 * Tại sao cần class này?
 * - ZXing mặc định dùng layout của thư viện (xấu, khó tuỳ chỉnh)
 * - Bằng cách extends CaptureActivity và khai báo trong Manifest với custom layout,
 *   ta có thể thêm overlay, branding, hay toolbar phù hợp với app.
 *
 * Lưu ý: Khai báo trong AndroidManifest.xml với
 *   android:screenOrientation="fullSensor"  (ZXing yêu cầu)
 */
public class CustomCaptureActivity extends CaptureActivity {
    // ZXing sẽ tự dùng layout từ ScanOptions.setCaptureActivity()
    // Override setContentView() nếu muốn custom layout XML:
    //
    // @Override
    // protected void onCreate(Bundle savedInstanceState) {
    //     super.onCreate(savedInstanceState);
    //     // setContentView() đã được CaptureActivity gọi — không gọi lại
    // }
}
