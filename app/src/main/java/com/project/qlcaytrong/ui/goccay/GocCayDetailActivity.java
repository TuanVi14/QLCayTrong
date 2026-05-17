// File: app/src/main/java/com/project/qlcaytrong/ui/goccay/GocCayDetailActivity.java
package com.project.qlcaytrong.ui.goccay;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.project.qlcaytrong.R;
import com.project.qlcaytrong.databinding.ActivityGocCayDetailBinding;
import com.project.qlcaytrong.ui.qr.QrScanActivity;
import com.project.qlcaytrong.util.QrUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executors;

/**
 * GocCayDetailActivity — hiển thị đầy đủ thông tin 1 GocCay + QR Code.
 *
 * Nhận data qua Intent extras (không load lại Room để tránh flickering).
 * QR Bitmap được generate trên background thread và post về main thread.
 *
 * Buttons:
 *  - Lưu QR → MediaStore (API 29+: IS_PENDING pattern; API < 29: cần WRITE_EXTERNAL_STORAGE)
 *  - Chia sẻ QR → Intent.ACTION_SEND
 *  - Chỉnh sửa → GocCayFormActivity (mode EDIT)
 *  - Xem Nhật ký → (stub, Bước 8)
 *  - Quét QR mới → QrScanActivity
 */
public class GocCayDetailActivity extends AppCompatActivity {

    // ==================== Intent Extras ====================
    public static final String EXTRA_GOC_CAY_ID   = "goc_cay_id";
    public static final String EXTRA_MA_QR        = "ma_qr";
    public static final String EXTRA_VI_TRI       = "vi_tri";
    public static final String EXTRA_TRANG_THAI   = "trang_thai";
    public static final String EXTRA_NGAY_TRONG   = "ngay_trong";
    public static final String EXTRA_GHI_CHU      = "ghi_chu";
    public static final String EXTRA_CAY_TRONG_ID = "cay_trong_id";
    public static final String EXTRA_USER_ID      = "user_id";

    private static final int QR_SIZE_PX = 512;
    private static final SimpleDateFormat DATE_FMT =
        new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    private ActivityGocCayDetailBinding binding;
    private String maQRCode;
    private String gocCayId, cayTrongId, userId;
    private Bitmap qrBitmap; // giữ reference để save/share
    private Uri savedQrUri;  // URI sau khi lưu vào gallery

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGocCayDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Đọc extras
        gocCayId   = getIntent().getStringExtra(EXTRA_GOC_CAY_ID);
        maQRCode   = getIntent().getStringExtra(EXTRA_MA_QR);
        cayTrongId = getIntent().getStringExtra(EXTRA_CAY_TRONG_ID);
        userId     = getIntent().getStringExtra(EXTRA_USER_ID);
        String viTri     = getIntent().getStringExtra(EXTRA_VI_TRI);
        String trangThai = getIntent().getStringExtra(EXTRA_TRANG_THAI);
        long ngayTrong   = getIntent().getLongExtra(EXTRA_NGAY_TRONG, 0);
        String ghiChu    = getIntent().getStringExtra(EXTRA_GHI_CHU);

        setupToolbar();
        populateInfo(viTri, trangThai, ngayTrong, ghiChu);
        generateAndDisplayQR();
        setupButtons(viTri, trangThai, ngayTrong, ghiChu);
    }

    // ==================== Setup ====================

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.goc_cay_detail_title);
            if (maQRCode != null)
                getSupportActionBar().setSubtitle(maQRCode);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void populateInfo(String viTri, String trangThai, long ngayTrong, String ghiChu) {
        // Mã QR
        binding.tvMaQR.setText(maQRCode != null ? maQRCode : "N/A");

        // Vị trí
        android.widget.TextView tvViTri = findViewById(R.id.tvViTri);
        tvViTri.setText(
            viTri != null && !viTri.isEmpty() ? viTri : getString(R.string.chua_co_vi_tri));

        // Ngày trồng
        android.widget.TextView tvNgayTrong = findViewById(R.id.tvNgayTrong);
        tvNgayTrong.setText(ngayTrong > 0
            ? DATE_FMT.format(new Date(ngayTrong))
            : "—");

        // Trạng thái với màu tương ứng
        bindStatusBadge(trangThai);

        // Ghi chú
        boolean hasNote = ghiChu != null && !ghiChu.isEmpty();
        binding.tvGhiChu.setText(hasNote ? ghiChu : getString(R.string.khong_co_ghi_chu));
        binding.tvGhiChu.setTextColor(getColor(hasNote
            ? R.color.textPrimary : R.color.textHint));
    }

    private void bindStatusBadge(String trangThai) {
        if (trangThai == null) return;
        switch (trangThai) {
            case "TOT":
                binding.tvTrangThai.setText(R.string.trang_thai_tot);
                binding.tvTrangThai.setBackgroundResource(R.drawable.bg_badge_tot);
                break;
            case "BINH_THUONG":
                binding.tvTrangThai.setText(R.string.trang_thai_binh_thuong);
                binding.tvTrangThai.setBackgroundResource(R.drawable.bg_badge_binh_thuong);
                break;
            case "XAU":
                binding.tvTrangThai.setText(R.string.trang_thai_xau);
                binding.tvTrangThai.setBackgroundResource(R.drawable.bg_badge_xau);
                break;
            case "CHET":
                binding.tvTrangThai.setText(R.string.trang_thai_chet);
                binding.tvTrangThai.setBackgroundResource(R.drawable.bg_badge_xau);
                break;
            default:
                binding.tvTrangThai.setText(trangThai);
                binding.tvTrangThai.setBackgroundResource(R.drawable.bg_badge_binh_thuong);
        }
    }

    // ==================== QR Generate ====================

    /**
     * Generate QR trên background thread (tránh ANR nếu maQRCode dài),
     * post bitmap về main thread để hiển thị.
     */
    private void generateAndDisplayQR() {
        if (maQRCode == null || maQRCode.isEmpty()) {
            binding.ivQrCode.setVisibility(View.GONE);
            binding.tvQrError.setVisibility(View.VISIBLE);
            binding.tvQrError.setText(R.string.qr_khong_co_ma);
            return;
        }

        binding.progressQr.setVisibility(View.VISIBLE);
        binding.ivQrCode.setVisibility(View.GONE);

        Executors.newSingleThreadExecutor().execute(() -> {
            qrBitmap = QrUtils.generateQrBitmap(maQRCode, QR_SIZE_PX);
            runOnUiThread(() -> {
                binding.progressQr.setVisibility(View.GONE);
                if (qrBitmap != null) {
                    binding.ivQrCode.setImageBitmap(qrBitmap);
                    binding.ivQrCode.setVisibility(View.VISIBLE);
                    binding.btnSaveQr.setEnabled(true);
                    binding.btnShareQr.setEnabled(true);
                } else {
                    binding.tvQrError.setVisibility(View.VISIBLE);
                    binding.tvQrError.setText(R.string.qr_generate_error);
                }
            });
        });
    }

    // ==================== Buttons ====================

    private void setupButtons(String viTri, String trangThai, long ngayTrong, String ghiChu) {
        // Lưu QR
        binding.btnSaveQr.setEnabled(false); // enable sau khi generate xong
        binding.btnSaveQr.setOnClickListener(v -> saveQrToGallery());

        // Chia sẻ QR
        binding.btnShareQr.setEnabled(false);
        binding.btnShareQr.setOnClickListener(v -> shareQr());

        // Chỉnh sửa gốc cây
        binding.btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(this, GocCayFormActivity.class);
            intent.putExtra(GocCayFormActivity.EXTRA_MODE, GocCayFormActivity.MODE_EDIT);
            intent.putExtra(GocCayFormActivity.EXTRA_ID, gocCayId);
            intent.putExtra(GocCayFormActivity.EXTRA_CAY_TRONG_ID, cayTrongId);
            intent.putExtra(GocCayFormActivity.EXTRA_MA_QR, maQRCode);
            intent.putExtra(GocCayFormActivity.EXTRA_VI_TRI, viTri);
            intent.putExtra(GocCayFormActivity.EXTRA_TRANG_THAI, trangThai);
            intent.putExtra(GocCayFormActivity.EXTRA_NGAY_TRONG, ngayTrong);
            intent.putExtra(GocCayFormActivity.EXTRA_GHI_CHU, ghiChu);
            intent.putExtra(GocCayFormActivity.EXTRA_USER_ID, userId);
            startActivity(intent);
        });

        // Xem nhật ký → NhatKyListActivity
        binding.btnNhatKy.setOnClickListener(v -> {
            Intent nhatKyIntent = new Intent(this,
                com.project.qlcaytrong.ui.nhatky.NhatKyListActivity.class);
            nhatKyIntent.putExtra(
                com.project.qlcaytrong.ui.nhatky.NhatKyListActivity.EXTRA_GOC_CAY_ID, gocCayId);
            nhatKyIntent.putExtra(
                com.project.qlcaytrong.ui.nhatky.NhatKyListActivity.EXTRA_LABEL, maQRCode);
            startActivity(nhatKyIntent);
        });


        // Quét QR mới
        binding.btnScanNew.setOnClickListener(v -> {
            Intent intent = new Intent(this, QrScanActivity.class);
            intent.putExtra(QrScanActivity.EXTRA_AUTO_LAUNCH, true);
            startActivity(intent);
        });
    }

    // ==================== Save QR ====================

    private void saveQrToGallery() {
        if (qrBitmap == null) {
            Snackbar.make(binding.getRoot(), R.string.qr_chua_tao, Snackbar.LENGTH_SHORT).show();
            return;
        }

        binding.btnSaveQr.setEnabled(false);
        Executors.newSingleThreadExecutor().execute(() -> {
            String fileName = QrUtils.buildFileName(maQRCode);
            savedQrUri = QrUtils.saveQrToGallery(this, qrBitmap, fileName);
            runOnUiThread(() -> {
                binding.btnSaveQr.setEnabled(true);
                if (savedQrUri != null) {
                    Snackbar.make(binding.getRoot(),
                        R.string.qr_luu_thanh_cong, Snackbar.LENGTH_LONG)
                        .setAction(R.string.xem, v ->
                            startActivity(new Intent(Intent.ACTION_VIEW, savedQrUri)
                                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)))
                        .show();
                } else {
                    Snackbar.make(binding.getRoot(),
                        R.string.qr_luu_that_bai, Snackbar.LENGTH_SHORT).show();
                }
            });
        });
    }

    // ==================== Share QR ====================

    private void shareQr() {
        if (qrBitmap == null) {
            Snackbar.make(binding.getRoot(), R.string.qr_chua_tao, Snackbar.LENGTH_SHORT).show();
            return;
        }

        // Cần URI để share — lưu tạm nếu chưa có
        if (savedQrUri != null) {
            doShare(savedQrUri);
        } else {
            binding.btnShareQr.setEnabled(false);
            Executors.newSingleThreadExecutor().execute(() -> {
                String fileName = QrUtils.buildFileName(maQRCode) + "_share";
                Uri uri = QrUtils.saveQrToGallery(this, qrBitmap, fileName);
                runOnUiThread(() -> {
                    binding.btnShareQr.setEnabled(true);
                    if (uri != null) {
                        savedQrUri = uri;
                        doShare(uri);
                    } else {
                        Snackbar.make(binding.getRoot(),
                            R.string.qr_share_error, Snackbar.LENGTH_SHORT).show();
                    }
                });
            });
        }
    }

    private void doShare(Uri uri) {
        Intent shareIntent = QrUtils.buildShareIntent(this, uri, maQRCode);
        startActivity(Intent.createChooser(shareIntent,
            getString(R.string.chia_se_qr_title)));
    }

    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }
}
