// File: app/src/main/java/com/project/qlcaytrong/MainActivity.java
package com.project.qlcaytrong;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.snackbar.Snackbar;
import com.project.qlcaytrong.databinding.ActivityMainBinding;
import com.project.qlcaytrong.sync.SyncManager;
import com.project.qlcaytrong.viewmodel.SyncStatusViewModel;

/**
 * MainActivity — shell với BottomNavigationView + NavController.
 *
 * Pattern: Single-Activity, Fragments navigate nội bộ qua NavController.
 * Sync badge + offline Snackbar hiển thị ở mức Activity để không bị fragment replace.
 */
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private SyncStatusViewModel syncViewModel;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupNavigation();
        setupSyncObservers();
        SyncManager.triggerImmediateSync(this);
    }

    private void setupNavigation() {
        NavHostFragment navHost = (NavHostFragment) getSupportFragmentManager()
            .findFragmentById(R.id.navHostFragment);
        if (navHost != null) {
            navController = navHost.getNavController();
            NavigationUI.setupWithNavController(binding.bottomNav, navController);
        }
    }

    private void setupSyncObservers() {
        syncViewModel = new ViewModelProvider(this).get(SyncStatusViewModel.class);

        syncViewModel.getSyncState().observe(this, state -> {
            boolean running = state == SyncStatusViewModel.SyncState.RUNNING;
            binding.syncStatusBar.setVisibility(running ? View.VISIBLE : View.GONE);
            binding.tvSyncStatus.setText(state.label);
        });

        // Offline banner — chỉ show 1 lần khi chuyển sang offline
        syncViewModel.getIsOnline().observe(this, online -> {
            if (!online) {
                Snackbar.make(binding.coordinatorLayout,
                    "Chế độ Offline — dữ liệu tự đồng bộ khi có mạng",
                    Snackbar.LENGTH_LONG).show();
            }
        });
    }
}