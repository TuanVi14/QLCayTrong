// File: app/src/main/java/com/project/qlcaytrong/ui/nhatky/NhatKyDetailActivity.java
package com.project.qlcaytrong.ui.nhatky;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.project.qlcaytrong.R;
import com.project.qlcaytrong.databinding.ActivityNhatKyDetailBinding;
import com.project.qlcaytrong.model.NhatKyModel;
import com.project.qlcaytrong.util.ImageLoader;
import com.project.qlcaytrong.util.ImagePickerHelper;
import com.project.qlcaytrong.viewmodel.NhatKyViewModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * NhatKyDetailActivity — hiển thị đầy đủ nhật ký + upload ảnh Firebase Storage.
 *
 * Bước 9:
 *  - Tap vùng ảnh → ImagePickerHelper (Camera/Gallery)
 *  - Compress → putBytes lên Storage → download URL
 *  - ProgressBar 0-100% với tvUploadPercent
 *  - Sau upload: Glide reload từ https:// URL, Room.hinhAnh cập nhật
 */
public class NhatKyDetailActivity extends AppCompatActivity {

    public static final String EXTRA_NHAT_KY_ID   = "nhat_ky_id";
    public static final String EXTRA_LOAI         = "loai";
    public static final String EXTRA_NGAY         = "ngay";
    public static final String EXTRA_NGUOI        = "nguoi";
    public static final String EXTRA_GHI_CHU      = "ghi_chu";
    public static final String EXTRA_HINH_ANH     = "hinh_anh";
    public static final String EXTRA_GOC_CAY_ID   = "goc_cay_id";
    public static final String EXTRA_CAY_TRONG_ID = "cay_trong_id";
    public static final String EXTRA_USER_ID      = "user_id";

    private static final SimpleDateFormat DATE_FMT =
        new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    private ActivityNhatKyDetailBinding binding;
    private NhatKyViewModel viewModel;
    private ImagePickerHelper imagePicker;

    private String nhatKyId, loai, nguoi, ghiChu, hinhAnh;
    private String gocCayId, cayTrongId, userId;
    private long ngay;

    private ChiTietTuoiPhanAdapter tuoiPhanAdapter;
    private ChiTietPhunThuocAdapter phunThuocAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNhatKyDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        readExtras();

        // PHẢI tạo ImagePickerHelper trước khi Activity started để registerForActivityResult hoạt động
        imagePicker = new ImagePickerHelper(this, this::onImagePicked);

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.nhat_ky_detail_title);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        viewModel = new ViewModelProvider(this).get(NhatKyViewModel.class);

        populateInfo();
        loadExistingImage();
        setupChiTietSections();
        loadChiTiet();
        setupButtons();
    }

    // ==================== Read Intent ====================

    private void readExtras() {
        nhatKyId   = getIntent().getStringExtra(EXTRA_NHAT_KY_ID);
        loai       = getIntent().getStringExtra(EXTRA_LOAI);
        ngay       = getIntent().getLongExtra(EXTRA_NGAY, 0);
        nguoi      = getIntent().getStringExtra(EXTRA_NGUOI);
        ghiChu     = getIntent().getStringExtra(EXTRA_GHI_CHU);
        hinhAnh    = getIntent().getStringExtra(EXTRA_HINH_ANH);
        gocCayId   = getIntent().getStringExtra(EXTRA_GOC_CAY_ID);
        cayTrongId = getIntent().getStringExtra(EXTRA_CAY_TRONG_ID);
        userId     = getIntent().getStringExtra(EXTRA_USER_ID);
    }

    // ==================== Populate ====================

    private void populateInfo() {
        binding.tvNgayThucHien.setText(ngay > 0 ? DATE_FMT.format(new Date(ngay)) : "—");
        binding.tvNguoiThucHien.setText(
            nguoi != null && !nguoi.isEmpty() ? nguoi : getString(R.string.khong_ro));
        binding.tvGhiChu.setText(
            ghiChu != null && !ghiChu.isEmpty() ? ghiChu : getString(R.string.khong_co_ghi_chu));
        bindLoai(loai);
    }

    private void bindLoai(String loai) {
        if (loai == null) return;
        switch (loai) {
            case "TUOI_PHAN":
                binding.tvLoai.setText(R.string.loai_tuoi_phan);
                binding.tvLoai.setBackgroundResource(R.drawable.bg_chip_green); break;
            case "PHUN_THUOC":
                binding.tvLoai.setText(R.string.loai_phun_thuoc);
                binding.tvLoai.setBackgroundResource(R.drawable.bg_chip_red); break;
            case "TINH_HINH":
                binding.tvLoai.setText(R.string.loai_tinh_hinh);
                binding.tvLoai.setBackgroundResource(R.drawable.bg_chip_orange); break;
            case "THU_HOACH":
                binding.tvLoai.setText(R.string.loai_thu_hoach);
                binding.tvLoai.setBackgroundResource(R.drawable.bg_chip_green); break;
        }
    }

    // ==================== Image Display ====================

    private void loadExistingImage() {
        if (hinhAnh != null && !hinhAnh.isEmpty()) {
            binding.ivHinhAnh.setVisibility(View.VISIBLE);
            binding.tvHinhAnhHint.setVisibility(View.GONE);
            ImageLoader.loadDetail(this, hinhAnh, binding.ivHinhAnh);
        } else {
            binding.ivHinhAnh.setVisibility(View.GONE);
            binding.tvHinhAnhHint.setVisibility(View.VISIBLE);
        }
    }

    // ==================== ChiTiet ====================

    private void setupChiTietSections() {
        tuoiPhanAdapter = new ChiTietTuoiPhanAdapter();
        binding.rvTuoiPhan.setLayoutManager(new LinearLayoutManager(this));
        binding.rvTuoiPhan.setAdapter(tuoiPhanAdapter);
        binding.rvTuoiPhan.setNestedScrollingEnabled(false);

        phunThuocAdapter = new ChiTietPhunThuocAdapter();
        binding.rvPhunThuoc.setLayoutManager(new LinearLayoutManager(this));
        binding.rvPhunThuoc.setAdapter(phunThuocAdapter);
        binding.rvPhunThuoc.setNestedScrollingEnabled(false);
    }

    private void loadChiTiet() {
        boolean isTuoiPhan  = "TUOI_PHAN".equals(loai);
        boolean isPhunThuoc = "PHUN_THUOC".equals(loai);

        binding.cardTuoiPhan.setVisibility(isTuoiPhan ? View.VISIBLE : View.GONE);
        binding.cardPhunThuoc.setVisibility(isPhunThuoc ? View.VISIBLE : View.GONE);

        if (nhatKyId == null) return;

        if (isTuoiPhan) {
            viewModel.getTuoiPhanByNhatKy(nhatKyId).observe(this, list -> {
                tuoiPhanAdapter.setRows(list);
                binding.tvTuoiPhanCount.setText(
                    getString(R.string.so_dong_chi_tiet, list != null ? list.size() : 0));
            });
        } else if (isPhunThuoc) {
            viewModel.getPhunThuocByNhatKy(nhatKyId).observe(this, list -> {
                phunThuocAdapter.setRows(list);
                binding.tvPhunThuocCount.setText(
                    getString(R.string.so_dong_chi_tiet, list != null ? list.size() : 0));
            });
        }
    }

    // ==================== Buttons ====================

    private void setupButtons() {
        binding.btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(this, NhatKyFormActivity.class);
            intent.putExtra(NhatKyFormActivity.EXTRA_MODE, NhatKyFormActivity.MODE_EDIT);
            intent.putExtra(NhatKyFormActivity.EXTRA_NHAT_KY_ID, nhatKyId);
            intent.putExtra(NhatKyFormActivity.EXTRA_GOC_CAY_ID, gocCayId);
            intent.putExtra(NhatKyFormActivity.EXTRA_CAY_TRONG_ID, cayTrongId);
            intent.putExtra(NhatKyFormActivity.EXTRA_LOAI, loai);
            intent.putExtra(NhatKyFormActivity.EXTRA_NGAY, ngay);
            intent.putExtra(NhatKyFormActivity.EXTRA_NGUOI, nguoi);
            intent.putExtra(NhatKyFormActivity.EXTRA_GHI_CHU, ghiChu);
            startActivity(intent);
        });

        binding.btnDelete.setOnClickListener(v ->
            new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_xoa_title)
                .setMessage(R.string.dialog_xoa_nhat_ky)
                .setNegativeButton(R.string.huy, (d, w) -> d.dismiss())
                .setPositiveButton(R.string.xoa, (d, w) -> {
                    d.dismiss();
                    viewModel.deleteNhatKy(buildModel()).observe(this, result -> {
                        if (result != null && result.isSuccess()) finish();
                        else if (result != null && result.isError())
                            Snackbar.make(binding.getRoot(),
                                result.message != null ? result.message : "Lỗi xóa",
                                Snackbar.LENGTH_SHORT).show();
                    });
                }).show());

        // Tap vùng ảnh → chọn nguồn
        binding.layoutHinhAnh.setOnClickListener(v -> imagePicker.showPickerDialog());
    }

    // ==================== Image Upload ====================

    /**
     * Callback từ ImagePickerHelper sau khi user chọn/chụp ảnh.
     *
     * Pattern: Optimistic UI — preview ảnh local ngay,
     * hiển thị progress bar trong khi upload chạy background.
     * Nếu fail → rollback về ảnh cũ.
     */
    private void onImagePicked(Uri imageUri) {
        if (nhatKyId == null) {
            Snackbar.make(binding.getRoot(),
                "Chưa có ID nhật ký. Hãy lưu nhật ký trước.", Snackbar.LENGTH_SHORT).show();
            return;
        }

        // Bước 1: Preview local ngay (optimistic)
        binding.ivHinhAnh.setVisibility(View.VISIBLE);
        binding.tvHinhAnhHint.setVisibility(View.GONE);
        ImageLoader.loadDetail(this, imageUri.toString(), binding.ivHinhAnh);

        // Bước 2: Hiện progress container
        binding.uploadProgressContainer.setVisibility(View.VISIBLE);
        binding.uploadProgressBar.setProgress(0);
        binding.tvUploadPercent.setText("0%");

        // Bước 3: Upload + observe state
        viewModel.uploadImage(imageUri, nhatKyId).observe(this, state -> {
            if (state == null) return;

            if (state.isLoading()) {
                binding.uploadProgressBar.setProgress(state.percent);
                binding.tvUploadPercent.setText(state.percent + "%");

            } else if (state.isSuccess()) {
                binding.uploadProgressContainer.setVisibility(View.GONE);
                hinhAnh = state.url; // cập nhật local state
                // Load lại từ URL Firebase (thay local preview)
                ImageLoader.loadDetail(this, hinhAnh, binding.ivHinhAnh);
                Snackbar.make(binding.getRoot(),
                    R.string.upload_anh_thanh_cong, Snackbar.LENGTH_SHORT).show();

            } else if (state.isError()) {
                binding.uploadProgressContainer.setVisibility(View.GONE);
                Snackbar.make(binding.getRoot(),
                    state.error != null ? state.error : getString(R.string.upload_anh_that_bai),
                    Snackbar.LENGTH_LONG)
                    .setAction(R.string.thu_lai, v -> imagePicker.showPickerDialog())
                    .show();
                // Rollback preview về ảnh cũ
                loadExistingImage();
            }
        });
    }

    // ==================== Model builder ====================

    private NhatKyModel buildModel() {
        NhatKyModel m = new NhatKyModel();
        m.setId(nhatKyId); m.setLoaiNhatKy(loai);
        m.setNgayThucHien(ngay); m.setNguoiThucHien(nguoi);
        m.setGhiChu(ghiChu); m.setHinhAnh(hinhAnh);
        m.setGocCayId(gocCayId); m.setCayTrongId(cayTrongId);
        m.setUserId(userId); m.setSyncStatus("PENDING");
        return m;
    }

    @Override public boolean onSupportNavigateUp() { finish(); return true; }
}
