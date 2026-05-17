// File: app/src/main/java/com/project/qlcaytrong/ui/caytrong/CayTrongFormActivity.java
package com.project.qlcaytrong.ui.caytrong;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;
import com.project.qlcaytrong.R;
import com.project.qlcaytrong.databinding.ActivityCayTrongFormBinding;
import com.project.qlcaytrong.model.CayTrongModel;
import com.project.qlcaytrong.util.AuthResult;
import com.project.qlcaytrong.viewmodel.CayTrongViewModel;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * CayTrongFormActivity — thêm/sửa cây trồng.
 * Dùng chung cho cả hai mode thông qua EXTRA_MODE.
 */
public class CayTrongFormActivity extends AppCompatActivity {

    // ==================== Intent Extras ====================
    public static final String EXTRA_MODE          = "mode";
    public static final String EXTRA_ID            = "id";
    public static final String EXTRA_MANH_DAT_ID   = "manh_dat_id";
    public static final String EXTRA_LOAI_CAY      = "loai_cay";
    public static final String EXTRA_TEN_KHOA_HOC  = "ten_khoa_hoc";
    public static final String EXTRA_SO_LUONG      = "so_luong";
    public static final String EXTRA_DON_VI_TINH   = "don_vi_tinh";
    public static final String EXTRA_NGAY_TRONG    = "ngay_trong";
    public static final String EXTRA_TRANG_THAI    = "trang_thai";
    public static final String EXTRA_MO_TA         = "mo_ta";
    public static final String EXTRA_USER_ID       = "user_id";

    public static final int MODE_CREATE = 0;
    public static final int MODE_EDIT   = 1;

    private static final SimpleDateFormat DATE_DISPLAY =
        new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    private ActivityCayTrongFormBinding binding;
    private CayTrongViewModel viewModel;

    private int mode;
    private String existingId, manhDatId, userId;
    private long selectedNgayTrong = System.currentTimeMillis();
    private String[] trangThaiValues;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCayTrongFormBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mode      = getIntent().getIntExtra(EXTRA_MODE, MODE_CREATE);
        manhDatId = getIntent().getStringExtra(EXTRA_MANH_DAT_ID);
        viewModel = new ViewModelProvider(this).get(CayTrongViewModel.class);
        if (manhDatId != null) viewModel.setManhDatId(manhDatId);

        trangThaiValues = getResources().getStringArray(R.array.trang_thai_cay_trong_values);

        setupToolbar();
        setupSpinners();
        setupDatePicker();
        if (mode == MODE_EDIT) populateFields();
        setupSaveButton();
    }

    // ==================== Setup ====================

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(mode == MODE_CREATE
                ? R.string.them_cay_trong : R.string.sua_cay_trong);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupSpinners() {
        // Spinner đơn vị tính
        ArrayAdapter<CharSequence> donViAdapter = ArrayAdapter.createFromResource(
            this, R.array.don_vi_tinh_cay, android.R.layout.simple_spinner_item);
        donViAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerDonViTinh.setAdapter(donViAdapter);

        // Spinner trạng thái
        ArrayAdapter<CharSequence> trangThaiAdapter = ArrayAdapter.createFromResource(
            this, R.array.trang_thai_cay_trong, android.R.layout.simple_spinner_item);
        trangThaiAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerTrangThai.setAdapter(trangThaiAdapter);
    }

    private void setupDatePicker() {
        // Hiển thị ngày hiện tại
        binding.tvNgayTrong.setText(DATE_DISPLAY.format(new Date(selectedNgayTrong)));

        binding.layoutNgayTrong.setOnClickListener(v -> showDatePicker());
        binding.tvNgayTrong.setOnClickListener(v -> showDatePicker());
    }

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(selectedNgayTrong);

        new DatePickerDialog(this,
            (view, year, month, dayOfMonth) -> {
                Calendar selected = Calendar.getInstance();
                selected.set(year, month, dayOfMonth, 0, 0, 0);
                selected.set(Calendar.MILLISECOND, 0);
                selectedNgayTrong = selected.getTimeInMillis();
                binding.tvNgayTrong.setText(DATE_DISPLAY.format(new Date(selectedNgayTrong)));
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void populateFields() {
        existingId = getIntent().getStringExtra(EXTRA_ID);
        userId     = getIntent().getStringExtra(EXTRA_USER_ID);
        selectedNgayTrong = getIntent().getLongExtra(EXTRA_NGAY_TRONG, System.currentTimeMillis());

        binding.etLoaiCay.setText(getIntent().getStringExtra(EXTRA_LOAI_CAY));
        binding.etTenKhoaHoc.setText(getIntent().getStringExtra(EXTRA_TEN_KHOA_HOC));
        binding.etSoLuong.setText(String.valueOf(getIntent().getIntExtra(EXTRA_SO_LUONG, 1)));
        binding.etMoTa.setText(getIntent().getStringExtra(EXTRA_MO_TA));
        binding.tvNgayTrong.setText(DATE_DISPLAY.format(new Date(selectedNgayTrong)));

        // Set spinner đơn vị tính
        setSpinnerSelection(binding.spinnerDonViTinh,
            getResources().getStringArray(R.array.don_vi_tinh_cay),
            getIntent().getStringExtra(EXTRA_DON_VI_TINH));

        // Set spinner trạng thái
        setSpinnerSelectionByValue(getIntent().getStringExtra(EXTRA_TRANG_THAI));
    }

    private void setSpinnerSelection(android.widget.Spinner spinner, String[] options, String value) {
        if (value == null) return;
        for (int i = 0; i < options.length; i++) {
            if (options[i].equals(value)) { spinner.setSelection(i); return; }
        }
    }

    private void setSpinnerSelectionByValue(String trangThaiValue) {
        if (trangThaiValue == null) return;
        for (int i = 0; i < trangThaiValues.length; i++) {
            if (trangThaiValues[i].equals(trangThaiValue)) {
                binding.spinnerTrangThai.setSelection(i);
                return;
            }
        }
    }

    private void setupSaveButton() {
        binding.btnSave.setOnClickListener(v -> performSave());
    }

    // ==================== Save ====================

    private void performSave() {
        binding.tilLoaiCay.setError(null);
        binding.tilSoLuong.setError(null);

        String loaiCay     = getText(binding.etLoaiCay);
        String tenKhoaHoc  = getText(binding.etTenKhoaHoc);
        String soLuongStr  = getText(binding.etSoLuong);
        String donViTinh   = binding.spinnerDonViTinh.getSelectedItem() != null
            ? binding.spinnerDonViTinh.getSelectedItem().toString() : "cây";
        int trangThaiIdx   = binding.spinnerTrangThai.getSelectedItemPosition();
        String trangThai   = trangThaiIdx >= 0 && trangThaiIdx < trangThaiValues.length
            ? trangThaiValues[trangThaiIdx] : "DANG_TRONG";
        String moTa        = getText(binding.etMoTa);

        int soLuong;
        try { soLuong = Integer.parseInt(soLuongStr); }
        catch (NumberFormatException e) { soLuong = 0; }

        if (mode == MODE_CREATE) {
            viewModel.addCayTrong(loaiCay, tenKhoaHoc, soLuong, donViTinh,
                selectedNgayTrong, trangThai, moTa)
                .observe(this, this::handleResult);
        } else {
            CayTrongModel existing = buildExisting(loaiCay, tenKhoaHoc, soLuong,
                donViTinh, trangThai, moTa);
            viewModel.updateCayTrong(existing, loaiCay, tenKhoaHoc, soLuong,
                donViTinh, selectedNgayTrong, trangThai, moTa)
                .observe(this, this::handleResult);
        }
    }

    private CayTrongModel buildExisting(String loaiCay, String tenKhoaHoc, int soLuong,
                                         String donViTinh, String trangThai, String moTa) {
        CayTrongModel m = new CayTrongModel();
        m.setId(existingId);
        m.setManhDatId(manhDatId);
        m.setUserId(userId != null ? userId : viewModel.getCurrentUserId());
        m.setNgayTrong(selectedNgayTrong);
        m.setLoaiCay(loaiCay); m.setTenKhoaHoc(tenKhoaHoc);
        m.setSoLuong(soLuong); m.setDonViTinh(donViTinh);
        m.setTrangThai(trangThai); m.setMoTa(moTa);
        return m;
    }

    private void handleResult(AuthResult<CayTrongModel> result) {
        if (result == null) return;
        if (result.isLoading()) { showLoading(true); return; }
        showLoading(false);

        if (result.isSuccess()) {
            Snackbar.make(binding.getRoot(),
                mode == MODE_CREATE
                    ? getString(R.string.them_cay_trong_thanh_cong)
                    : getString(R.string.cap_nhat_thanh_cong),
                Snackbar.LENGTH_SHORT)
                .addCallback(new Snackbar.Callback() {
                    @Override public void onDismissed(Snackbar sb, int event) { finish(); }
                }).show();
        } else if (result.isError()) {
            String msg = result.message;
            if (msg != null && msg.toLowerCase().contains("loại cây"))
                binding.tilLoaiCay.setError(msg);
            else if (msg != null && msg.toLowerCase().contains("số lượng"))
                binding.tilSoLuong.setError(msg);
            else
                Snackbar.make(binding.getRoot(),
                    msg != null ? msg : "Lỗi không xác định", Snackbar.LENGTH_LONG).show();
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
