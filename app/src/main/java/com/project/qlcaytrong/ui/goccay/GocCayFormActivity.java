// File: app/src/main/java/com/project/qlcaytrong/ui/goccay/GocCayFormActivity.java
package com.project.qlcaytrong.ui.goccay;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;
import com.project.qlcaytrong.R;
import com.project.qlcaytrong.databinding.ActivityGocCayFormBinding;
import com.project.qlcaytrong.model.GocCayModel;
import com.project.qlcaytrong.util.AuthResult;
import com.project.qlcaytrong.viewmodel.GocCayViewModel;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/** GocCayFormActivity — thêm/sửa gốc cây. Tự sinh mã QR khi tạo mới. */
public class GocCayFormActivity extends AppCompatActivity {

    public static final String EXTRA_MODE         = "mode";
    public static final String EXTRA_ID           = "id";
    public static final String EXTRA_CAY_TRONG_ID = "cay_trong_id";
    public static final String EXTRA_MA_QR        = "ma_qr";
    public static final String EXTRA_VI_TRI       = "vi_tri";
    public static final String EXTRA_TRANG_THAI   = "trang_thai";
    public static final String EXTRA_NGAY_TRONG   = "ngay_trong";
    public static final String EXTRA_GHI_CHU      = "ghi_chu";
    public static final String EXTRA_USER_ID      = "user_id";

    public static final int MODE_CREATE = 0;
    public static final int MODE_EDIT   = 1;

    private static final SimpleDateFormat DATE_FMT =
        new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    private ActivityGocCayFormBinding binding;
    private GocCayViewModel viewModel;

    private int mode;
    private String existingId, cayTrongId, userId, existingQR;
    private long selectedNgayTrong = System.currentTimeMillis();
    private String[] trangThaiValues;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGocCayFormBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mode        = getIntent().getIntExtra(EXTRA_MODE, MODE_CREATE);
        cayTrongId  = getIntent().getStringExtra(EXTRA_CAY_TRONG_ID);
        viewModel   = new ViewModelProvider(this).get(GocCayViewModel.class);
        if (cayTrongId != null) viewModel.setCayTrongId(cayTrongId);

        trangThaiValues = getResources().getStringArray(R.array.trang_thai_goc_cay_values);

        setupToolbar();
        setupSpinner();
        setupDatePicker();
        if (mode == MODE_EDIT) populateFields();
        setupSaveButton();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(mode == MODE_CREATE
                ? R.string.them_goc_cay : R.string.sua_goc_cay);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
            this, R.array.trang_thai_goc_cay, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerTrangThai.setAdapter(adapter);
    }

    private void setupDatePicker() {
        binding.tvNgayTrong.setText(DATE_FMT.format(new Date(selectedNgayTrong)));
        binding.layoutNgayTrong.setOnClickListener(v -> showDatePicker());
        binding.tvNgayTrong.setOnClickListener(v -> showDatePicker());
    }

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(selectedNgayTrong);
        new DatePickerDialog(this, (view, y, m, d) -> {
            Calendar c = Calendar.getInstance();
            c.set(y, m, d, 0, 0, 0);
            c.set(Calendar.MILLISECOND, 0);
            selectedNgayTrong = c.getTimeInMillis();
            binding.tvNgayTrong.setText(DATE_FMT.format(new Date(selectedNgayTrong)));
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void populateFields() {
        existingId = getIntent().getStringExtra(EXTRA_ID);
        userId     = getIntent().getStringExtra(EXTRA_USER_ID);
        existingQR = getIntent().getStringExtra(EXTRA_MA_QR);
        selectedNgayTrong = getIntent().getLongExtra(EXTRA_NGAY_TRONG, System.currentTimeMillis());

        // Hiển thị mã QR (readonly khi edit)
        binding.tvMaQRDisplay.setText(existingQR != null ? existingQR : "Auto-generated");
        binding.etViTri.setText(getIntent().getStringExtra(EXTRA_VI_TRI));
        binding.etGhiChu.setText(getIntent().getStringExtra(EXTRA_GHI_CHU));
        binding.tvNgayTrong.setText(DATE_FMT.format(new Date(selectedNgayTrong)));

        setSpinnerByValue(getIntent().getStringExtra(EXTRA_TRANG_THAI));
    }

    private void setSpinnerByValue(String value) {
        if (value == null) return;
        for (int i = 0; i < trangThaiValues.length; i++) {
            if (trangThaiValues[i].equals(value)) {
                binding.spinnerTrangThai.setSelection(i);
                return;
            }
        }
    }

    private void setupSaveButton() {
        binding.btnSave.setOnClickListener(v -> performSave());
    }

    private void performSave() {
        String viTri = getText(binding.etViTri);
        String ghiChu = getText(binding.etGhiChu);
        int idx = binding.spinnerTrangThai.getSelectedItemPosition();
        String trangThai = (idx >= 0 && idx < trangThaiValues.length)
            ? trangThaiValues[idx] : "TOT";

        if (mode == MODE_CREATE) {
            // maQRCode = null → ViewModel tự sinh
            viewModel.addGocCay(null, viTri, trangThai, selectedNgayTrong, ghiChu)
                .observe(this, this::handleResult);
        } else {
            GocCayModel existing = new GocCayModel();
            existing.setId(existingId);
            existing.setCayTrongId(cayTrongId);
            existing.setUserId(userId);
            existing.setMaQRCode(existingQR);
            existing.setNgayTrong(selectedNgayTrong);
            viewModel.updateGocCay(existing, viTri, trangThai, ghiChu)
                .observe(this, this::handleResult);
        }
    }

    private void handleResult(AuthResult<GocCayModel> result) {
        if (result == null) return;
        if (result.isLoading()) { showLoading(true); return; }
        showLoading(false);

        if (result.isSuccess()) {
            Snackbar.make(binding.getRoot(),
                mode == MODE_CREATE
                    ? getString(R.string.them_goc_cay_thanh_cong)
                    : getString(R.string.cap_nhat_thanh_cong),
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
