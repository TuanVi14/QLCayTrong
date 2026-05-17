// File: app/src/main/java/com/project/qlcaytrong/ui/auth/LoginActivity.java
package com.project.qlcaytrong.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;
import com.project.qlcaytrong.databinding.ActivityLoginBinding;
import com.project.qlcaytrong.ui.main.MainActivity;
import com.project.qlcaytrong.util.AuthResult;
import com.project.qlcaytrong.viewmodel.AuthViewModel;

/**
 * LoginActivity — màn hình đăng nhập.
 *
 * Flow dữ liệu:
 *  User nhập email/password
 *    → btnLogin.click()
 *      → AuthViewModel.login()
 *        → AuthRepositoryImpl.login() (gọi Firebase)
 *          → MutableLiveData<AuthResult> emit kết quả
 *            → Activity observe → cập nhật UI
 */
public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private AuthViewModel authViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        setupListeners();
    }

    // ==================== Listeners ====================

    private void setupListeners() {
        // Bấm Enter trên field password → trigger login
        binding.etPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                performLogin();
                return true;
            }
            return false;
        });

        binding.btnLogin.setOnClickListener(v -> performLogin());

        binding.tvGoToRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
            // Không finish() — cho phép back về Login
        });
    }

    // ==================== Login logic ====================

    private void performLogin() {
        String email    = getTextFrom(binding.etEmail);
        String password = getTextFrom(binding.etPassword);

        // Xóa error cũ
        binding.tilEmail.setError(null);
        binding.tilPassword.setError(null);

        authViewModel.login(email, password).observe(this, result -> {
            handleAuthResult(result);
        });
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
        binding.btnLogin.setEnabled(!show);
        binding.etEmail.setEnabled(!show);
        binding.etPassword.setEnabled(!show);
    }

    private void showError(String message) {
        // Highlight field tương ứng nếu lỗi liên quan
        if (message != null && message.toLowerCase().contains("email")) {
            binding.tilEmail.setError(message);
        } else if (message != null && message.toLowerCase().contains("mật khẩu")) {
            binding.tilPassword.setError(message);
        } else {
            Snackbar.make(binding.getRoot(), message != null ? message : "Lỗi không xác định",
                    Snackbar.LENGTH_LONG).show();
        }
    }

    private String getTextFrom(com.google.android.material.textfield.TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }
}
