// File: app/src/main/java/com/project/qlcaytrong/ui/manhdat/ManhDatListActivity.java
package com.project.qlcaytrong.ui.manhdat;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.project.qlcaytrong.ui.caytrong.CayTrongListActivity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.project.qlcaytrong.R;
import com.project.qlcaytrong.databinding.ActivityManhDatListBinding;
import com.project.qlcaytrong.model.ManhDatModel;
import com.project.qlcaytrong.viewmodel.ManhDatViewModel;

import java.util.List;

/**
 * ManhDatListActivity — hiển thị danh sách mảnh đất.
 *
 * UI Flow:
 *  - RecyclerView observe Room LiveData → auto-update
 *  - FAB → ManhDatFormActivity (mode: CREATE)
 *  - Item click → ManhDatFormActivity (mode: VIEW — tương lai)
 *  - Edit icon → ManhDatFormActivity (mode: EDIT)
 *  - Delete icon → Dialog xác nhận → deleteManhDat()
 *  - SwipeRefresh → syncFromFirestore()
 */
public class ManhDatListActivity extends AppCompatActivity
    implements ManhDatAdapter.OnItemClickListener {

    public static final String EXTRA_MANH_DAT_ID = "manh_dat_id";

    private ActivityManhDatListBinding binding;
    private ManhDatViewModel viewModel;
    private ManhDatAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityManhDatListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.manh_dat_title);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        viewModel = new ViewModelProvider(this).get(ManhDatViewModel.class);

        setupRecyclerView();
        setupFab();
        setupSwipeRefresh();
        observeData();
    }

    // ==================== Setup ====================

    private void setupRecyclerView() {
        adapter = new ManhDatAdapter(this);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);
        binding.recyclerView.setHasFixedSize(true);
    }

    private void setupFab() {
        binding.fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(this, ManhDatFormActivity.class);
            intent.putExtra(ManhDatFormActivity.EXTRA_MODE, ManhDatFormActivity.MODE_CREATE);
            startActivity(intent);
        });
    }

    private void setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeResources(R.color.colorPrimary);
        binding.swipeRefresh.setOnRefreshListener(() ->
            viewModel.syncFromFirestore().observe(this, result -> {
                binding.swipeRefresh.setRefreshing(false);
                if (result != null && result.isError()) {
                    Snackbar.make(binding.getRoot(),
                        result.message != null ? result.message : getString(R.string.error_sync),
                        Snackbar.LENGTH_SHORT).show();
                }
            })
        );
    }

    private void observeData() {
        viewModel.getManhDatList().observe(this, this::renderList);
    }

    // ==================== Render ====================

    private void renderList(List<ManhDatModel> list) {
        if (list == null || list.isEmpty()) {
            binding.layoutEmpty.setVisibility(View.VISIBLE);
            binding.recyclerView.setVisibility(View.GONE);
        } else {
            binding.layoutEmpty.setVisibility(View.GONE);
            binding.recyclerView.setVisibility(View.VISIBLE);
            adapter.submitList(list);
        }
    }

    // ==================== Adapter Callbacks ====================

    @Override
    public void onClick(ManhDatModel model) {
        // Navigate đến CayTrongListActivity — Bước 6 ✓
        Intent intent = new Intent(this, CayTrongListActivity.class);
        intent.putExtra(CayTrongListActivity.EXTRA_MANH_DAT_ID, model.getId());
        intent.putExtra(CayTrongListActivity.EXTRA_TEN_MANH_DAT, model.getTenManhDat());
        startActivity(intent);
    }

    @Override
    public void onEdit(ManhDatModel model) {
        Intent intent = new Intent(this, ManhDatFormActivity.class);
        intent.putExtra(ManhDatFormActivity.EXTRA_MODE, ManhDatFormActivity.MODE_EDIT);
        intent.putExtra(ManhDatFormActivity.EXTRA_MANH_DAT_ID, model.getId());
        intent.putExtra(ManhDatFormActivity.EXTRA_TEN, model.getTenManhDat());
        intent.putExtra(ManhDatFormActivity.EXTRA_DIA_CHI, model.getDiaChi());
        intent.putExtra(ManhDatFormActivity.EXTRA_DIEN_TICH, String.valueOf(model.getDienTich()));
        intent.putExtra(ManhDatFormActivity.EXTRA_DON_VI, model.getDonViDienTich());
        intent.putExtra(ManhDatFormActivity.EXTRA_MO_TA, model.getMoTa());
        intent.putExtra(ManhDatFormActivity.EXTRA_USER_ID, model.getUserId());
        intent.putExtra(ManhDatFormActivity.EXTRA_NGAY_TAO, model.getNgayTao());
        startActivity(intent);
    }

    @Override
    public void onDelete(ManhDatModel model) {
        showDeleteDialog(model);
    }

    // ==================== Delete Dialog ====================

    private void showDeleteDialog(ManhDatModel model) {
        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_xoa_title)
            .setMessage(getString(R.string.dialog_xoa_message, model.getTenManhDat()))
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setNegativeButton(R.string.huy, (dialog, which) -> dialog.dismiss())
            .setPositiveButton(R.string.xoa, (dialog, which) -> {
                dialog.dismiss();
                performDelete(model);
            })
            .show();
    }

    private void performDelete(ManhDatModel model) {
        viewModel.deleteManhDat(model).observe(this, result -> {
            if (result == null) return;
            if (result.isSuccess()) {
                Snackbar.make(binding.getRoot(),
                    R.string.xoa_thanh_cong, Snackbar.LENGTH_SHORT).show();
            } else if (result.isError()) {
                Snackbar.make(binding.getRoot(),
                    result.message != null ? result.message : getString(R.string.error_xoa),
                    Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
