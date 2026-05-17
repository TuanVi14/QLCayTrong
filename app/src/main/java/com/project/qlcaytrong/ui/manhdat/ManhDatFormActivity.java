// File: app/src/main/java/com/project/qlcaytrong/ui/manhdat/ManhDatFormActivity.java
package com.project.qlcaytrong.ui.manhdat;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;
import com.project.qlcaytrong.R;
import com.project.qlcaytrong.databinding.ActivityManhDatFormBinding;
import com.project.qlcaytrong.model.ManhDatModel;
import com.project.qlcaytrong.util.AuthResult;
import com.project.qlcaytrong.viewmodel.ManhDatViewModel;

/**
 * ManhDatFormActivity — dùng chung cho Thêm và Sửa mảnh đất.
 * Mode được truyền qua Intent Extra: MODE_CREATE | MODE_EDIT
 */
public class ManhDatFormActivity extends AppCompatActivity {

    // ==================== Intent keys ====================
    public static final String EXTRA_MODE       = "mode";
    public static final String EXTRA_MANH_DAT_ID= "manh_dat_id";
    public static final String EXTRA_TEN        = "ten_manh_dat";
    public static final String EXTRA_DIA_CHI    = "dia_chi";
    public static final String EXTRA_DIEN_TICH  = "dien_tich";
    public static final String EXTRA_DON_VI     = "don_vi_dien_tich";
    public static final String EXTRA_MO_TA      = "mo_ta";
    public static final String EXTRA_USER_ID    = "user_id";
    public static final String EXTRA_NGAY_TAO   = "ngay_tao";

    // ==================== Mode constants ====================
    public static final int MODE_CREATE = 0;
    public static final int MODE_EDIT   = 1;

    private ActivityManhDatFormBinding binding;
    private ManhDatViewModel viewModel;

    private int mode;
    private String manhDatId;
    private String userId;
    private long ngayTao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityManhDatFormBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mode = getIntent().getIntExtra(EXTRA_MODE, MODE_CREATE);
        viewModel = new ViewModelProvider(this).get(ManhDatViewModel.class);

        setupToolbar();
        populateIfEdit();
        setupSaveButton();
    }

    // ==================== Setup ====================

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(
                mode == MODE_CREATE ? R.string.them_manh_dat : R.string.sua_manh_dat
            );
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void populateIfEdit() {
        if (mode != MODE_EDIT) return;

        manhDatId = getIntent().getStringExtra(EXTRA_MANH_DAT_ID);
        userId    = getIntent().getStringExtra(EXTRA_USER_ID);
        ngayTao   = getIntent().getLongExtra(EXTRA_NGAY_TAO, 0L);

        String ten      = getIntent().getStringExtra(EXTRA_TEN);
        String diaChi   = getIntent().getStringExtra(EXTRA_DIA_CHI);
        String dienTich = getIntent().getStringExtra(EXTRA_DIEN_TICH);
        String donVi    = getIntent().getStringExtra(EXTRA_DON_VI);
        String moTa     = getIntent().getStringExtra(EXTRA_MO_TA);

        binding.etTenManhDat.setText(ten);
        binding.etDiaChi.setText(diaChi);
        binding.etDienTich.setText(dienTich);
        binding.etMoTa.setText(moTa);

        // Set spinner đơn vị
        setSpinnerValue(donVi);
    }

    private void setSpinnerValue(String donVi) {
        if (donVi == null) return;
        String[] options = getResources().getStringArray(R.array.don_vi_dien_tich);
        for (int i = 0; i < options.length; i++) {
            if (options[i].equals(donVi)) {
                binding.spinnerDonVi.setSelection(i);
                break;
            }
        }
    }

    private void setupSaveButton() {
        binding.btnSave.setOnClickListener(v -> performSave());
    }

    // ==================== Save logic ====================

    private void performSave() {
        String tenManhDat  = getText(binding.etTenManhDat);
        String diaChi      = getText(binding.etDiaChi);
        String dienTichStr = getText(binding.etDienTich);
        String donViDienTich = binding.spinnerDonVi.getSelectedItem() != null
            ? binding.spinnerDonVi.getSelectedItem().toString() : "";
        String moTa        = getText(binding.etMoTa);

        // Xóa error cũ
        binding.tilTenManhDat.setError(null);
        binding.tilDienTich.setError(null);

        if (mode == MODE_CREATE) {
            viewModel.createManhDat(tenManhDat, diaChi, dienTichStr, donViDienTich, moTa)
                .observe(this, this::handleResult);
        } else {
            ManhDatModel existing = buildExistingModel(tenManhDat, diaChi, dienTichStr, donViDienTich, moTa);
            viewModel.updateManhDat(existing, tenManhDat, diaChi, dienTichStr, donViDienTich, moTa)
                .observe(this, this::handleResult);
        }
    }

    private ManhDatModel buildExistingModel(String ten, String diaChi,
                                             String dienTich, String donVi, String moTa) {
        ManhDatModel model = new ManhDatModel();
        model.setId(manhDatId);
        model.setUserId(userId != null ? userId : viewModel.getCurrentUserIdPublic());
        model.setNgayTao(ngayTao);
        model.setTenManhDat(ten);
        model.setDiaChi(diaChi);
        model.setDienTich(0); // sẽ được set lại trong updateManhDat
        model.setDonViDienTich(donVi);
        model.setMoTa(moTa);
        return model;
    }

    // ==================== Handle result ====================

    private void handleResult(AuthResult<ManhDatModel> result) {
        if (result == null) return;

        if (result.isLoading()) {
            showLoading(true);
            return;
        }

        showLoading(false);

        if (result.isSuccess()) {
            String msg = mode == MODE_CREATE
                ? getString(R.string.them_thanh_cong)
                : getString(R.string.cap_nhat_thanh_cong);

            Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_SHORT)
                .addCallback(new com.google.android.material.snackbar.Snackbar.Callback() {
                    @Override
                    public void onDismissed(com.google.android.material.snackbar.Snackbar sb, int event) {
                        finish();
                    }
                }).show();

        } else if (result.isError()) {
            showValidationError(result.message);
        }
    }

    private void showValidationError(String message) {
        if (message == null) return;
        String lower = message.toLowerCase();
        if (lower.contains("tên mảnh đất") || lower.contains("ten manh dat")) {
            binding.tilTenManhDat.setError(message);
        } else if (lower.contains("diện tích") || lower.contains("dien tich")) {
            binding.tilDienTich.setError(message);
        } else {
            Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
        }
    }

    private void showLoading(boolean show) {
        binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.btnSave.setEnabled(!show);
    }

    private String getText(com.google.android.material.textfield.TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
