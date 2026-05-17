// File: app/src/main/java/com/project/qlcaytrong/ui/dashboard/DashboardFragment.java
package com.project.qlcaytrong.ui.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.project.qlcaytrong.databinding.FragmentDashboardBinding;
import com.project.qlcaytrong.viewmodel.DashboardViewModel;

/**
 * DashboardFragment — trang chủ tổng quan.
 *
 * Hiển thị:
 *  - 3 stat cards: mảnh đất, cây trồng, nhật ký hôm nay
 *  - Cảnh báo cây bỏ quên (> 7 ngày không chăm sóc)
 *  - RecyclerView ngang: mảnh đất nổi bật
 *  - RecyclerView dọc: nhật ký gần nhất (3 mục)
 *
 * SwipeRefreshLayout: kéo để refresh + trigger sync.
 */
public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;
    private DashboardViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                              @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        setupRecyclerViews();
        observeData();
        setupSwipeRefresh();
    }

    private void setupRecyclerViews() {
        // Mảnh đất ngang
        binding.rvManhDat.setLayoutManager(
            new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rvManhDat.setAdapter(new DashboardManhDatAdapter());

        // Nhật ký gần nhất
        binding.rvRecentNhatKy.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvRecentNhatKy.setNestedScrollingEnabled(false);
        binding.rvRecentNhatKy.setAdapter(new DashboardNhatKyAdapter());
    }

    private void observeData() {
        viewModel.getStats().observe(getViewLifecycleOwner(), stats -> {
            if (stats == null) return;
            binding.tvStatManhDat.setText(String.valueOf(stats.totalManhDat));
            binding.tvStatCayTrong.setText(String.valueOf(stats.totalCayTrong));
            binding.tvStatNhatKy.setText(String.valueOf(stats.nhatKyHomNay));

            // Cảnh báo cây bỏ quên
            boolean hasWarning = stats.soGocCayBoQuen > 0;
            binding.cardWarning.setVisibility(hasWarning ? View.VISIBLE : View.GONE);
            if (hasWarning) {
                binding.tvWarning.setText(
                    stats.soGocCayBoQuen + " cây chưa được chăm sóc hơn 7 ngày!");
            }
        });

        viewModel.getManhDatList().observe(getViewLifecycleOwner(), list -> {
            if (binding.rvManhDat.getAdapter() instanceof DashboardManhDatAdapter) {
                ((DashboardManhDatAdapter) binding.rvManhDat.getAdapter()).setData(list);
            }
        });

        viewModel.getRecentNhatKy().observe(getViewLifecycleOwner(), list -> {
            if (binding.rvRecentNhatKy.getAdapter() instanceof DashboardNhatKyAdapter) {
                ((DashboardNhatKyAdapter) binding.rvRecentNhatKy.getAdapter()).setData(list);
            }
            binding.tvEmptyNhatKy.setVisibility(
                (list == null || list.isEmpty()) ? View.VISIBLE : View.GONE);
        });
    }

    private void setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeResources(
            com.project.qlcaytrong.R.color.colorPrimary,
            com.project.qlcaytrong.R.color.colorAccent);
        binding.swipeRefresh.setOnRefreshListener(() -> {
            viewModel.refresh();
            // Stop spinner sau 2s (WorkManager sẽ tự sync)
            binding.swipeRefresh.postDelayed(
                () -> binding.swipeRefresh.setRefreshing(false), 2000);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
