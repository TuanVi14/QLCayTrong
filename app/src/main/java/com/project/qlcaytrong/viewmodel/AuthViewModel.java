// File: app/src/main/java/com/project/qlcaytrong/viewmodel/AuthViewModel.java
package com.project.qlcaytrong.viewmodel;

import android.app.Application;
import android.util.Patterns;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.project.qlcaytrong.data.repository.AuthRepository;
import com.project.qlcaytrong.data.repository.AuthRepositoryImpl;
import com.project.qlcaytrong.util.AuthResult;

/**
 * AuthViewModel — trung gian giữa UI và AuthRepository.
 * Không chứa reference đến Activity/Context để tránh memory leak.
 * Dùng AndroidViewModel để access Application context an toàn.
 */
public class AuthViewModel extends AndroidViewModel {

    private final AuthRepository authRepository;

    // LiveData kết quả Login
    private LiveData<AuthResult<String>> loginResult;
    // LiveData kết quả Register
    private LiveData<AuthResult<String>> registerResult;

    // LiveData lỗi validation form (chỉ dùng nội bộ trước khi gọi API)
    private final MutableLiveData<String> validationError = new MutableLiveData<>();

    public AuthViewModel(@NonNull Application application) {
        super(application);
        authRepository = new AuthRepositoryImpl(application);
    }

    // ==================== LOGIN ====================

    /**
     * Validate rồi gọi login. LiveData trả về để Activity observe.
     */
    public LiveData<AuthResult<String>> login(String email, String password) {
        String validationMsg = validateLoginInput(email, password);
        if (validationMsg != null) {
            MutableLiveData<AuthResult<String>> errorLiveData = new MutableLiveData<>();
            errorLiveData.setValue(AuthResult.error(validationMsg));
            loginResult = errorLiveData;
            return loginResult;
        }
        loginResult = authRepository.login(email, password);
        return loginResult;
    }

    // ==================== REGISTER ====================

    /**
     * Validate rồi gọi register. LiveData trả về để Activity observe.
     */
    public LiveData<AuthResult<String>> register(String email,
                                                  String password,
                                                  String confirmPassword,
                                                  String hoTen,
                                                  String soDienThoai) {
        String validationMsg = validateRegisterInput(email, password, confirmPassword, hoTen);
        if (validationMsg != null) {
            MutableLiveData<AuthResult<String>> errorLiveData = new MutableLiveData<>();
            errorLiveData.setValue(AuthResult.error(validationMsg));
            registerResult = errorLiveData;
            return registerResult;
        }
        registerResult = authRepository.register(email, password, hoTen, soDienThoai);
        return registerResult;
    }

    // ==================== LOGOUT ====================

    public void logout() {
        authRepository.logout();
    }

    // ==================== SESSION CHECK ====================

    public boolean isLoggedIn() {
        return authRepository.isLoggedIn();
    }

    public String getCurrentUserId() {
        return authRepository.getCurrentUserId();
    }

    // ==================== VALIDATION ====================

    private String validateLoginInput(String email, String password) {
        if (email == null || email.trim().isEmpty()) {
            return "Vui lòng nhập email.";
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            return "Địa chỉ email không hợp lệ.";
        }
        if (password == null || password.isEmpty()) {
            return "Vui lòng nhập mật khẩu.";
        }
        if (password.length() < 6) {
            return "Mật khẩu phải có ít nhất 6 ký tự.";
        }
        return null;
    }

    private String validateRegisterInput(String email,
                                         String password,
                                         String confirmPassword,
                                         String hoTen) {
        if (hoTen == null || hoTen.trim().isEmpty()) {
            return "Vui lòng nhập họ tên.";
        }
        if (email == null || email.trim().isEmpty()) {
            return "Vui lòng nhập email.";
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            return "Địa chỉ email không hợp lệ.";
        }
        if (password == null || password.isEmpty()) {
            return "Vui lòng nhập mật khẩu.";
        }
        if (password.length() < 6) {
            return "Mật khẩu phải có ít nhất 6 ký tự.";
        }
        if (!password.equals(confirmPassword)) {
            return "Mật khẩu xác nhận không khớp.";
        }
        return null;
    }

    public LiveData<String> getValidationError() {
        return validationError;
    }
}
