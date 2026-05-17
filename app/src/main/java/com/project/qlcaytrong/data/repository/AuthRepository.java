// File: app/src/main/java/com/project/qlcaytrong/data/repository/AuthRepository.java
package com.project.qlcaytrong.data.repository;

import androidx.lifecycle.LiveData;

import com.project.qlcaytrong.util.AuthResult;

/**
 * Contract (interface) cho Auth operations.
 * ViewModel chỉ phụ thuộc vào interface này — không biết Firebase.
 */
public interface AuthRepository {

    /**
     * Đăng nhập bằng email/password.
     * @return LiveData phát ra trạng thái Loading → Success(userId) | Error(message)
     */
    LiveData<AuthResult<String>> login(String email, String password);

    /**
     * Đăng ký tài khoản mới.
     * @return LiveData phát ra trạng thái Loading → Success(userId) | Error(message)
     */
    LiveData<AuthResult<String>> register(String email,
                                          String password,
                                          String hoTen,
                                          String soDienThoai);

    /**
     * Đăng xuất khỏi Firebase Auth và xóa session local.
     */
    void logout();

    /**
     * Kiểm tra xem Firebase Auth hiện có session hợp lệ không.
     */
    boolean isLoggedIn();

    /**
     * Lấy UID của user đang đăng nhập (null nếu chưa đăng nhập).
     */
    String getCurrentUserId();
}
