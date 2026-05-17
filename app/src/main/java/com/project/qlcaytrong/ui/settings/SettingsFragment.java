// File: app/src/main/java/com/project/qlcaytrong/ui/settings/SettingsFragment.java
package com.project.qlcaytrong.ui.settings;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.project.qlcaytrong.databinding.FragmentSettingsBinding;
import com.project.qlcaytrong.sync.SyncManager;
import com.project.qlcaytrong.ui.auth.LoginActivity;

/**
 * SettingsFragment — Profile + Dark mode + Logout.
 *
 * Dark mode: AppCompatDelegate.setDefaultNightMode() — persist bằng SharedPreferences.
 * Logout: hủy WorkManager sync → FirebaseAuth.signOut() → về LoginActivity.
 */
public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                              @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Populate user info
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            binding.tvUserEmail.setText(user.getEmail());
            binding.tvUserName.setText(user.getDisplayName() != null
                ? user.getDisplayName() : "Nông dân QLCayTrong");
        }

        // App version
        binding.tvVersion.setText("Phiên bản 1.0");

        // Dark mode toggle — read current mode
        boolean isDark = AppCompatDelegate.getDefaultNightMode()
            == AppCompatDelegate.MODE_NIGHT_YES;
        binding.switchDarkMode.setChecked(isDark);
        binding.switchDarkMode.setOnCheckedChangeListener((btn, checked) -> {
            AppCompatDelegate.setDefaultNightMode(checked
                ? AppCompatDelegate.MODE_NIGHT_YES
                : AppCompatDelegate.MODE_NIGHT_NO);
            // Persist choice
            requireActivity().getSharedPreferences("qlcaytrong_prefs", 0)
                .edit().putBoolean("dark_mode", checked).apply();
        });

        // Logout
        binding.btnLogout.setOnClickListener(v -> {
            SyncManager.cancelAll(requireContext());
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(requireActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
