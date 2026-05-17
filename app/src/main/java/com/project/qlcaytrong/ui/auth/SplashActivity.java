// File: app/src/main/java/com/project/qlcaytrong/ui/auth/SplashActivity.java
package com.project.qlcaytrong.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.project.qlcaytrong.databinding.ActivitySplashBinding;
import com.project.qlcaytrong.MainActivity;
import com.project.qlcaytrong.viewmodel.AuthViewModel;

/**
 * SplashActivity — điểm khởi đầu của ứng dụng.
 * Delay 1.5 giây → kiểm tra Firebase Auth session → redirect.
 *
 * Flow:
 *  SplashActivity
 *      └─ isLoggedIn? ──YES──→ MainActivity
 *                    └─ NO ──→ LoginActivity
 */
public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY_MS = 1500;

    private ActivitySplashBinding binding;
    private AuthViewModel authViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        new Handler(Looper.getMainLooper()).postDelayed(this::checkSession, SPLASH_DELAY_MS);
    }

    private void checkSession() {
        if (authViewModel.isLoggedIn()) {
            goToMain();
        } else {
            goToLogin();
        }
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void goToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}
