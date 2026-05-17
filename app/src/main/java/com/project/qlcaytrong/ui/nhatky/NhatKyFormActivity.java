// File: app/src/main/java/com/project/qlcaytrong/ui/nhatky/NhatKyFormActivity.java
package com.project.qlcaytrong.ui.nhatky;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.snackbar.Snackbar;
import com.project.qlcaytrong.R;
import com.project.qlcaytrong.databinding.ActivityNhatKyFormBinding;
import com.project.qlcaytrong.model.NhatKyModel;
import com.project.qlcaytrong.util.AuthResult;
import com.project.qlcaytrong.viewmodel.NhatKyViewModel;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * NhatKyFormActivity — thêm/sửa nhật ký chăm sóc.
 *
 * Dynamic form behavior:
 *  - RadioGroup chọn loại → show/hide section ChiTiet tương ứng
 *  - TUOI_PHAN → hiện RecyclerView tuoiPhan với ChiTietTuoiPhanAdapter
 *  - PHUN_THUOC → hiện RecyclerView phunThuoc với ChiTietPhunThuocAdapter
 *  - TINH_HINH / THU_HOACH → chỉ dùng ghiChu, ẩn cả 2 RecyclerView
 */
public class NhatKyFormActivity extends AppCompatActivity {

    public static final String EXTRA_MODE         = "mode";
    public static final String EXTRA_NHAT_KY_ID   = "nhat_ky_id";
    public static final String EXTRA_GOC_CAY_ID   = "goc_cay_id";
    public static final String EXTRA_CAY_TRONG_ID = "cay_trong_id";
    public static final String EXTRA_LOAI         = "loai";
    public static final String EXTRA_NGAY         = "ngay";
    public static final String EXTRA_NGUOI        = "nguoi";
    public static final String EXTRA_GHI_CHU      = "ghi_chu";

    public static final int MODE_CREATE = 0;
    public static final int MODE_EDIT   = 1;

    private static final SimpleDateFormat DATE_FMT =
        new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    private ActivityNhatKyFormBinding binding;
    private NhatKyViewModel viewModel;
    private ChiTietTuoiPhanAdapter tuoiPhanAdapter;
    private ChiTietPhunThuocAdapter phunThuocAdapter;

    private int mode;
    private String existingId, gocCayId, cayTrongId;
    private long selectedNgay = System.currentTimeMillis();
    private String selectedLoai = "TINH_HINH";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNhatKyFormBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mode        = getIntent().getIntExtra(EXTRA_MODE, MODE_CREATE);
        existingId  = getIntent().getStringExtra(EXTRA_NHAT_KY_ID);
        gocCayId    = getIntent().getStringExtra(EXTRA_GOC_CAY_ID);
        cayTrongId  = getIntent().getStringExtra(EXTRA_CAY_TRONG_ID);
        viewModel   = new ViewModelProvider(this).get(NhatKyViewModel.class);

        setupToolbar();
        setupDatePicker();
        setupLoaiSelector();
        setupChiTietRecyclerViews();
        if (mode == MODE_EDIT) populateFields();
        binding.btnSave.setOnClickListener(v -> performSave());
    }

    // ==================== Setup ====================

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(mode == MODE_CREATE
                ? R.string.them_nhat_ky : R.string.sua_nhat_ky);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupDatePicker() {
        binding.tvNgayThucHien.setText(DATE_FMT.format(new Date(selectedNgay)));
        View.OnClickListener pick = v -> {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(selectedNgay);
            new DatePickerDialog(this, (view, y, m, d) -> {
                Calendar c = Calendar.getInstance();
                c.set(y, m, d, 0, 0, 0);
                c.set(Calendar.MILLISECOND, 0);
                selectedNgay = c.getTimeInMillis();
                binding.tvNgayThucHien.setText(DATE_FMT.format(new Date(selectedNgay)));
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)).show();
        };
        binding.layoutNgayThucHien.setOnClickListener(pick);
        binding.tvNgayThucHien.setOnClickListener(pick);
    }

    /**
     * RadioGroup chọn loại nhật ký → show/hide section ChiTiet.
     * Sử dụng RadioGroup thay vì Spinner để rõ ràng hơn trên màn hình form.
     */
    private void setupLoaiSelector() {
        binding.radioGroupLoai.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioTuoiPhan) {
                selectedLoai = "TUOI_PHAN";
            } else if (checkedId == R.id.radioPhunThuoc) {
                selectedLoai = "PHUN_THUOC";
            } else if (checkedId == R.id.radioTinhHinh) {
                selectedLoai = "TINH_HINH";
            } else if (checkedId == R.id.radioThuHoach) {
                selectedLoai = "THU_HOACH";
            }
            updateChiTietVisibility();
        });
        // Default: TINH_HINH
        binding.radioTinhHinh.setChecked(true);
        updateChiTietVisibility();
    }

    private void updateChiTietVisibility() {
        boolean isTuoiPhan   = "TUOI_PHAN".equals(selectedLoai);
        boolean isPhunThuoc  = "PHUN_THUOC".equals(selectedLoai);

        binding.sectionTuoiPhan.setVisibility(isTuoiPhan ? View.VISIBLE : View.GONE);
        binding.sectionPhunThuoc.setVisibility(isPhunThuoc ? View.VISIBLE : View.GONE);
    }

    /**
     * Setup 2 RecyclerView inline (không scroll — chiều cao cố định theo nội dung).
     *
     * setNestedScrollingEnabled(false) rất quan trọng khi RecyclerView nằm trong ScrollView:
     * - Tắt scroll của RecyclerView → ScrollView tự handle toàn bộ scroll
     * - Nếu không tắt: RecyclerView scroll riêng → trải nghiệm tệ
     */
    private void setupChiTietRecyclerViews() {
        // TuoiPhan
        tuoiPhanAdapter = new ChiTietTuoiPhanAdapter();
        binding.rvTuoiPhan.setLayoutManager(new LinearLayoutManager(this));
        binding.rvTuoiPhan.setAdapter(tuoiPhanAdapter);
        binding.rvTuoiPhan.setNestedScrollingEnabled(false);
        binding.btnThemTuoiPhan.setOnClickListener(v -> {
            tuoiPhanAdapter.addRow();
            // Scroll xuống cuối để thấy row mới
            binding.scrollView.post(() ->
                binding.scrollView.fullScroll(View.FOCUS_DOWN));
        });

        // PhunThuoc
        phunThuocAdapter = new ChiTietPhunThuocAdapter();
        binding.rvPhunThuoc.setLayoutManager(new LinearLayoutManager(this));
        binding.rvPhunThuoc.setAdapter(phunThuocAdapter);
        binding.rvPhunThuoc.setNestedScrollingEnabled(false);
        binding.btnThemPhunThuoc.setOnClickListener(v -> {
            phunThuocAdapter.addRow();
            binding.scrollView.post(() ->
                binding.scrollView.fullScroll(View.FOCUS_DOWN));
        });
    }

    private void populateFields() {
        selectedNgay = getIntent().getLongExtra(EXTRA_NGAY, System.currentTimeMillis());
        selectedLoai = getIntent().getStringExtra(EXTRA_LOAI);
        if (selectedLoai == null) selectedLoai = "TINH_HINH";

        binding.etNguoiThucHien.setText(getIntent().getStringExtra(EXTRA_NGUOI));
        binding.etGhiChu.setText(getIntent().getStringExtra(EXTRA_GHI_CHU));
        binding.tvNgayThucHien.setText(DATE_FMT.format(new Date(selectedNgay)));

        // Set RadioButton
        switch (selectedLoai) {
            case "TUOI_PHAN":  binding.radioTuoiPhan.setChecked(true); break;
            case "PHUN_THUOC": binding.radioPhunThuoc.setChecked(true); break;
            case "THU_HOACH":  binding.radioThuHoach.setChecked(true); break;
            default:           binding.radioTinhHinh.setChecked(true);
        }

        // Load ChiTiet nếu mode EDIT
        if (existingId != null && "TUOI_PHAN".equals(selectedLoai)) {
            viewModel.getTuoiPhanByNhatKy(existingId).observe(this, list ->
                tuoiPhanAdapter.setRows(list));
        } else if (existingId != null && "PHUN_THUOC".equals(selectedLoai)) {
            viewModel.getPhunThuocByNhatKy(existingId).observe(this, list ->
                phunThuocAdapter.setRows(list));
        }
    }

    // ==================== Save ====================

    private void performSave() {
        String nguoi  = getText(binding.etNguoiThucHien);
        String ghiChu = getText(binding.etGhiChu);

        // Validate chi tiết phân/thuốc
        if ("TUOI_PHAN".equals(selectedLoai)) {
            for (var m : tuoiPhanAdapter.getRows()) {
                if (m.getLieuLuong() <= 0) {
                    Snackbar.make(binding.getRoot(),
                        R.string.error_lieu_luong, Snackbar.LENGTH_SHORT).show();
                    return;
                }
            }
        }
        if ("PHUN_THUOC".equals(selectedLoai)) {
            for (var m : phunThuocAdapter.getRows()) {
                if (m.getTenThuoc() == null || m.getTenThuoc().isEmpty()) {
                    Snackbar.make(binding.getRoot(),
                        R.string.error_ten_thuoc, Snackbar.LENGTH_SHORT).show();
                    return;
                }
                if (m.getLieuLuong() <= 0) {
                    Snackbar.make(binding.getRoot(),
                        R.string.error_lieu_luong, Snackbar.LENGTH_SHORT).show();
                    return;
                }
            }
        }

        if (mode == MODE_CREATE) {
            viewModel.insertWithChiTiet(
                selectedLoai, gocCayId, cayTrongId, selectedNgay, nguoi, ghiChu,
                tuoiPhanAdapter.getRows(), phunThuocAdapter.getRows()
            ).observe(this, this::handleResult);
        } else {
            NhatKyModel existing = buildExisting(nguoi, ghiChu);
            viewModel.updateWithChiTiet(
                existing, selectedLoai, selectedNgay, nguoi, ghiChu,
                tuoiPhanAdapter.getRows(), phunThuocAdapter.getRows()
            ).observe(this, this::handleResult);
        }
    }

    private NhatKyModel buildExisting(String nguoi, String ghiChu) {
        NhatKyModel m = new NhatKyModel();
        m.setId(existingId);
        m.setGocCayId(gocCayId);
        m.setCayTrongId(cayTrongId);
        m.setUserId(viewModel.getCurrentUserId());
        m.setNgayThucHien(selectedNgay);
        m.setNguoiThucHien(nguoi);
        m.setGhiChu(ghiChu);
        return m;
    }

    private void handleResult(AuthResult<NhatKyModel> result) {
        if (result == null) return;
        if (result.isLoading()) { showLoading(true); return; }
        showLoading(false);

        if (result.isSuccess()) {
            Snackbar.make(binding.getRoot(),
                mode == MODE_CREATE ? R.string.them_nhat_ky_thanh_cong : R.string.cap_nhat_thanh_cong,
                Snackbar.LENGTH_SHORT)
                .addCallback(new Snackbar.Callback() {
                    @Override public void onDismissed(Snackbar sb, int event) { finish(); }
                }).show();
        } else if (result.isError()) {
            Snackbar.make(binding.getRoot(),
                result.message != null ? result.message : "Lỗi không xác định",
                Snackbar.LENGTH_LONG).show();
        }
    }

    private void showLoading(boolean show) {
        binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.btnSave.setEnabled(!show);
    }

    private String getText(com.google.android.material.textfield.TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    @Override public boolean onSupportNavigateUp() { finish(); return true; }
}
