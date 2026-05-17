// File: app/src/main/java/com/project/qlcaytrong/data/repository/AuthRepositoryImpl.java
package com.project.qlcaytrong.data.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

import com.project.qlcaytrong.data.local.AppDatabase;
import com.project.qlcaytrong.data.local.entity.NguoiDungEntity;
import com.project.qlcaytrong.util.AuthResult;
import com.project.qlcaytrong.util.SessionManager;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Triển khai AuthRepository dùng FirebaseAuth.
 * - Login/Register gọi Firebase Auth
 * - Sau register thành công → lưu NguoiDungEntity vào Room
 * - Lưu session vào SessionManager
 */
public class AuthRepositoryImpl implements AuthRepository {

    private static final String TAG = "AuthRepositoryImpl";

    private final FirebaseAuth firebaseAuth;
    private final AppDatabase  database;
    private final SessionManager sessionManager;
    private final Executor        ioExecutor;

    public AuthRepositoryImpl(Context context) {
        this.firebaseAuth   = FirebaseAuth.getInstance();
        this.database       = AppDatabase.getInstance(context);
        this.sessionManager = SessionManager.getInstance(context);
        this.ioExecutor     = Executors.newSingleThreadExecutor();
    }

    // ==================== LOGIN ====================

    @Override
    public LiveData<AuthResult<String>> login(String email, String password) {
        MutableLiveData<AuthResult<String>> result = new MutableLiveData<>();
        result.setValue(AuthResult.loading());

        firebaseAuth.signInWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener(authResult -> {
                FirebaseUser user = authResult.getUser();
                if (user == null) {
                    result.setValue(AuthResult.error("Không thể lấy thông tin người dùng."));
                    return;
                }

                String userId = user.getUid();
                String displayName = user.getDisplayName() != null
                        ? user.getDisplayName() : "";

                // Lưu session
                sessionManager.saveSession(userId, email.trim(), displayName);

                Log.d(TAG, "Login success: " + userId);
                result.setValue(AuthResult.success(userId));
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Login failed", e);
                result.setValue(AuthResult.error(mapFirebaseError(e)));
            });

        return result;
    }

    // ==================== REGISTER ====================

    @Override
    public LiveData<AuthResult<String>> register(String email,
                                                  String password,
                                                  String hoTen,
                                                  String soDienThoai) {
        MutableLiveData<AuthResult<String>> result = new MutableLiveData<>();
        result.setValue(AuthResult.loading());

        firebaseAuth.createUserWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener(authResult -> {
                FirebaseUser firebaseUser = authResult.getUser();
                if (firebaseUser == null) {
                    result.setValue(AuthResult.error("Đăng ký thất bại, vui lòng thử lại."));
                    return;
                }

                String userId = firebaseUser.getUid();

                // Cập nhật displayName trên Firebase Auth profile
                UserProfileChangeRequest profileUpdate = new UserProfileChangeRequest.Builder()
                    .setDisplayName(hoTen.trim())
                    .build();

                firebaseUser.updateProfile(profileUpdate)
                    .addOnCompleteListener(task -> {
                        // Lưu NguoiDungEntity vào Room (chạy trên IO thread)
                        NguoiDungEntity entity = new NguoiDungEntity(
                            userId,
                            hoTen.trim(),
                            email.trim(),
                            soDienThoai.trim(),
                            System.currentTimeMillis(),
                            "PENDING"   // Sẽ sync lên Firestore sau
                        );

                        ioExecutor.execute(() -> {
                            database.nguoiDungDao().insert(entity);
                            Log.d(TAG, "NguoiDungEntity saved to Room: " + userId);
                        });

                        // Lưu session
                        sessionManager.saveSession(userId, email.trim(), hoTen.trim());

                        Log.d(TAG, "Register success: " + userId);
                        result.setValue(AuthResult.success(userId));
                    });
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Register failed", e);
                result.setValue(AuthResult.error(mapFirebaseError(e)));
            });

        return result;
    }

    // ==================== LOGOUT ====================

    @Override
    public void logout() {
        firebaseAuth.signOut();
        sessionManager.clearSession();
        Log.d(TAG, "User logged out");
    }

    // ==================== HELPERS ====================

    @Override
    public boolean isLoggedIn() {
        return firebaseAuth.getCurrentUser() != null;
    }

    @Override
    public String getCurrentUserId() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    /**
     * Chuyển Firebase exception thành thông báo tiếng Việt thân thiện.
     */
    private String mapFirebaseError(Exception e) {
        if (e instanceof FirebaseAuthWeakPasswordException) {
            return "Mật khẩu quá yếu. Vui lòng nhập ít nhất 6 ký tự.";
        } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
            return "Email hoặc mật khẩu không đúng. Vui lòng kiểm tra lại.";
        } else if (e instanceof FirebaseAuthUserCollisionException) {
            return "Email này đã được đăng ký. Vui lòng dùng email khác hoặc đăng nhập.";
        } else if (e instanceof FirebaseAuthInvalidUserException) {
            return "Tài khoản không tồn tại hoặc đã bị vô hiệu hóa.";
        } else {
            String msg = e.getMessage();
            if (msg != null && msg.contains("NETWORK_ERROR")) {
                return "Lỗi kết nối mạng. Vui lòng kiểm tra internet.";
            }
            return "Đã xảy ra lỗi. Vui lòng thử lại sau.";
        }
    }
}
