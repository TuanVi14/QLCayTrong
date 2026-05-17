// File: app/src/main/java/com/project/qlcaytrong/ui/auth/RegisterActivity.java
package com.project.qlcaytrong.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;
import com.project.qlcaytrong.databinding.ActivityRegisterBinding;
import com.project.qlcaytrong.MainActivity;
import com.project.qlcaytrong.util.AuthResult;
import com.project.qlcaytrong.viewmodel.AuthViewModel;

/**
 * RegisterActivity — màn hình đăng ký tài khoản mới.
 *
 * Sau đăng ký thành công:
 * 1. FirebaseAuth tạo user mới
 * 2. NguoiDungEntity được lưu vào Room (syncStatus = PENDING)
 * 3. SessionManager lưu userId, email, hoTen
 * 4. Navigate → MainActivity (clear back stack)
 */
public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private AuthViewModel authViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        setupListeners();
    }

    // ==================== Listeners ====================

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> finish());

        binding.btnRegister.setOnClickListener(v -> performRegister());

        binding.tvGoToLogin.setOnClickListener(v -> {
            // Quay về LoginActivity (pop back stack)
            finish();
        });
    }

    // ==================== Register logic ====================

    private void performRegister() {
        String hoTen           = getTextFrom(binding.etHoTen);
        String soDienThoai     = getTextFrom(binding.etSoDienThoai);
        String email           = getTextFrom(binding.etEmail);
        String password        = getTextFrom(binding.etPassword);
        String confirmPassword = getTextFrom(binding.etConfirmPassword);

        // Xóa error cũ
        binding.tilHoTen.setError(null);
        binding.tilEmail.setError(null);
        binding.tilPassword.setError(null);
        binding.tilConfirmPassword.setError(null);

        authViewModel.register(email, password, confirmPassword, hoTen, soDienThoai)
            .observe(this, this::handleAuthResult);
    }

    // ==================== Xử lý kết quả ====================

    private void handleAuthResult(AuthResult<String> result) {
        if (result.isLoading()) {
            showLoading(true);
            return;
        }

        showLoading(false);

        if (result.isSuccess()) {
            goToMain();
        } else if (result.isError()) {
            showError(result.message);
        }
    }

    private void goToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // ==================== UI helpers ====================

    private void showLoading(boolean show) {
        binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.btnRegister.setEnabled(!show);
        binding.etHoTen.setEnabled(!show);
        binding.etSoDienThoai.setEnabled(!show);
        binding.etEmail.setEnabled(!show);
        binding.etPassword.setEnabled(!show);
        binding.etConfirmPassword.setEnabled(!show);
    }

    private void showError(String message) {
        if (message == null) return;

        String lower = message.toLowerCase();
        if (lower.contains("họ tên")) {
            binding.tilHoTen.setError(message);
        } else if (lower.contains("email")) {
            binding.tilEmail.setError(message);
        } else if (lower.contains("xác nhận") || lower.contains("khớp")) {
            binding.tilConfirmPassword.setError(message);
        } else if (lower.contains("mật khẩu")) {
            binding.tilPassword.setError(message);
        } else {
            Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
        }
    }

    private String getTextFrom(com.google.android.material.textfield.TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }
}
