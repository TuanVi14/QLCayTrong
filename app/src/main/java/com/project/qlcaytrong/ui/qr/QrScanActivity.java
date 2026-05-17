// File: app/src/main/java/com/project/qlcaytrong/ui/qr/QrScanActivity.java
package com.project.qlcaytrong.ui.qr;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanIntentResult;
import com.journeyapps.barcodescanner.ScanOptions;
import com.project.qlcaytrong.R;
import com.project.qlcaytrong.databinding.ActivityQrScanBinding;
import com.project.qlcaytrong.model.GocCayModel;
import com.project.qlcaytrong.ui.goccay.GocCayDetailActivity;
import com.project.qlcaytrong.viewmodel.GocCayViewModel;

import java.util.concurrent.Executors;

/**
 * QrScanActivity — màn hình quét QR Code.
 *
 * == Flow ==
 *   Camera Permission OK → ZXing ScanContract → onScanResult(content)
 *     → GocCayViewModel.findByQRCode(content) [IO thread]
 *       → GocCay found    → navigate GocCayDetailActivity
 *       → GocCay not found → dialog thông báo
 *       → Scan empty/cancel → show hint
 *
 * == Lỗi thường gặp với ZXing ==
 *   1. "No cameras found" trên emulator: AVD cần enable camera (Webcam / Virtual Scene)
 *   2. "CAMERA permission denied": runtime request trước khi gọi ScanContract
 *   3. ZXing crash với transitive deps: dùng { isTransitive = false } khi import
 *      android-embedded, để tránh conflict với com.google.zxing:core tự import thêm
 *   4. "decode" trả về null: QR không đọc được → hỏi người dùng quét lại
 *
 * == Test trên emulator ==
 *   Tools → Camera → Virtual Scene: kéo barcode simulator từ internet,
 *   đặt trước camera ảo để test.
 *   Hoặc dùng "3D Room" environment → kéo image panel có QR code vào view.
 */
public class QrScanActivity extends AppCompatActivity {

    private ActivityQrScanBinding binding;
    private GocCayViewModel viewModel;

    // ==================== Activity Result Launchers ====================

    /** Launcher quét QR dùng ZXing ScanContract (thay thế deprecated IntentIntegrator) */
    private final ActivityResultLauncher<ScanOptions> qrScanLauncher =
        registerForActivityResult(new ScanContract(), this::onScanResult);

    /** Launcher request CAMERA permission */
    private final ActivityResultLauncher<String> cameraPermissionLauncher =
        registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            granted -> {
                if (granted) {
                    launchScanner();
                } else {
                    showPermissionDeniedDialog();
                }
            });

    // ==================== Lifecycle ====================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityQrScanBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.quet_qr_title);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        viewModel = new ViewModelProvider(this).get(GocCayViewModel.class);

        binding.btnScan.setOnClickListener(v -> checkPermissionAndScan());
        binding.btnScanHint.setOnClickListener(v -> checkPermissionAndScan());

        // Auto-launch scanner khi mở từ FAB
        if (getIntent().getBooleanExtra(EXTRA_AUTO_LAUNCH, false)) {
            checkPermissionAndScan();
        }
    }

    public static final String EXTRA_AUTO_LAUNCH = "auto_launch";

    // ==================== Permission ====================

    private void checkPermissionAndScan() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            launchScanner();
        } else if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            // Người dùng đã từ chối một lần → giải thích lý do
            new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.camera_permission_title)
                .setMessage(R.string.camera_permission_rationale)
                .setPositiveButton(R.string.dong_y, (d, w) ->
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA))
                .setNegativeButton(R.string.huy, (d, w) -> d.dismiss())
                .show();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    /**
     * Hiển thị dialog khi người dùng chọn "Never ask again".
     * Hướng dẫn vào Settings để bật thủ công.
     */
    private void showPermissionDeniedDialog() {
        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.camera_permission_denied_title)
            .setMessage(R.string.camera_permission_denied_message)
            .setPositiveButton(R.string.mo_cai_dat, (d, w) -> {
                d.dismiss();
                Intent intent = new Intent(
                    android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    android.net.Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            })
            .setNegativeButton(R.string.huy, (d, w) -> { d.dismiss(); finish(); })
            .show();
    }

    // ==================== Scanner ====================

    private void launchScanner() {
        ScanOptions options = new ScanOptions()
            .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            .setPrompt(getString(R.string.qr_scan_prompt))
            .setBeepEnabled(true)
            .setBarcodeImageEnabled(false)
            .setOrientationLocked(false)
            .setCaptureActivity(CustomCaptureActivity.class);

        qrScanLauncher.launch(options);
    }

    // ==================== Scan Result ====================

    private void onScanResult(@NonNull ScanIntentResult result) {
        if (result.getContents() == null) {
            // Người dùng bấm Back / hủy scan
            Snackbar.make(binding.getRoot(),
                R.string.qr_scan_cancelled, Snackbar.LENGTH_SHORT).show();
            return;
        }

        String maQRCode = result.getContents();
        showLoading(true);
        binding.tvLastScan.setText(getString(R.string.qr_last_scan, maQRCode));

        // Tìm GocCay trên IO thread vì Room không cho phép main thread
        Executors.newSingleThreadExecutor().execute(() -> {
            GocCayModel gocCay = viewModel.findByQRCode(maQRCode);
            runOnUiThread(() -> {
                showLoading(false);
                if (gocCay != null) {
                    navigateToDetail(gocCay);
                } else {
                    showNotFoundDialog(maQRCode);
                }
            });
        });
    }

    // ==================== Navigation ====================

    private void navigateToDetail(@NonNull GocCayModel gocCay) {
        Intent intent = new Intent(this, GocCayDetailActivity.class);
        intent.putExtra(GocCayDetailActivity.EXTRA_GOC_CAY_ID, gocCay.getId());
        intent.putExtra(GocCayDetailActivity.EXTRA_MA_QR, gocCay.getMaQRCode());
        intent.putExtra(GocCayDetailActivity.EXTRA_VI_TRI, gocCay.getViTri());
        intent.putExtra(GocCayDetailActivity.EXTRA_TRANG_THAI, gocCay.getTrangThai());
        intent.putExtra(GocCayDetailActivity.EXTRA_NGAY_TRONG, gocCay.getNgayTrong());
        intent.putExtra(GocCayDetailActivity.EXTRA_GHI_CHU, gocCay.getGhiChu());
        intent.putExtra(GocCayDetailActivity.EXTRA_CAY_TRONG_ID, gocCay.getCayTrongId());
        intent.putExtra(GocCayDetailActivity.EXTRA_USER_ID, gocCay.getUserId());
        startActivity(intent);
    }

    // ==================== Dialogs ====================

    private void showNotFoundDialog(String maQRCode) {
        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.qr_not_found_title)
            .setMessage(getString(R.string.qr_not_found_message, maQRCode))
            .setIcon(android.R.drawable.ic_dialog_info)
            .setPositiveButton(R.string.quet_lai, (d, w) -> {
                d.dismiss();
                launchScanner();
            })
            .setNegativeButton(R.string.dong, (d, w) -> d.dismiss())
            .show();
    }

    // ==================== UI State ====================

    private void showLoading(boolean show) {
        binding.progressBar.setVisibility(show
            ? android.view.View.VISIBLE : android.view.View.GONE);
        binding.btnScan.setEnabled(!show);
    }

    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }
}
