// File: app/src/main/java/com/project/qlcaytrong/ui/goccay/GocCayListActivity.java
package com.project.qlcaytrong.ui.goccay;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.project.qlcaytrong.ui.goccay.GocCayDetailActivity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.project.qlcaytrong.R;
import com.project.qlcaytrong.databinding.ActivityGocCayListBinding;
import com.project.qlcaytrong.model.GocCayModel;
import com.project.qlcaytrong.viewmodel.GocCayViewModel;

import java.util.List;

/**
 * GocCayListActivity — danh sách gốc cây trong một CayTrong.
 *
 * Navigation flow: CayTrongListActivity → GocCayListActivity
 *
 * Intent extras:
 *  - EXTRA_CAY_TRONG_ID: String (bắt buộc)
 *  - EXTRA_LOAI_CAY: String (hiển thị ở subtitle toolbar)
 */
public class GocCayListActivity extends AppCompatActivity
    implements GocCayAdapter.OnItemClickListener {

    public static final String EXTRA_CAY_TRONG_ID = "cay_trong_id";
    public static final String EXTRA_LOAI_CAY     = "loai_cay";

    private ActivityGocCayListBinding binding;
    private GocCayViewModel viewModel;
    private GocCayAdapter adapter;
    private String cayTrongId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGocCayListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        cayTrongId     = getIntent().getStringExtra(EXTRA_CAY_TRONG_ID);
        String loaiCay = getIntent().getStringExtra(EXTRA_LOAI_CAY);

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getString(R.string.goc_cay_title));
            if (loaiCay != null) getSupportActionBar().setSubtitle(loaiCay);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        viewModel = new ViewModelProvider(this).get(GocCayViewModel.class);
        if (cayTrongId != null) viewModel.setCayTrongId(cayTrongId);

        setupRecyclerView();
        setupFab();
        setupSwipeRefresh();
        observeData();
    }

    private void setupRecyclerView() {
        adapter = new GocCayAdapter(this);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);
        binding.recyclerView.setHasFixedSize(true);
    }

    private void setupFab() {
        binding.fabAdd.setOnClickListener(v -> openForm(null));
    }

    private void setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeResources(R.color.colorPrimary);
        binding.swipeRefresh.setOnRefreshListener(() ->
            viewModel.syncFromCloud().observe(this, result -> {
                binding.swipeRefresh.setRefreshing(false);
                if (result != null && result.isError())
                    Snackbar.make(binding.getRoot(),
                        result.message != null ? result.message : getString(R.string.error_sync),
                        Snackbar.LENGTH_SHORT).show();
            })
        );
    }

    private void observeData() {
        viewModel.getGocCayList().observe(this, this::renderList);
    }

    private void renderList(List<GocCayModel> list) {
        boolean empty = list == null || list.isEmpty();
        binding.layoutEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
        if (!empty) adapter.submitList(list);
    }

    // ==================== Adapter Callbacks ====================

    @Override
    public void onItemClick(GocCayModel model) {
        openDetail(model);
    }

    @Override
    public void onEdit(GocCayModel model) {
        openForm(model);
    }

    @Override
    public void onViewQR(GocCayModel model) {
        openDetail(model);
    }

    // ==================== Detail ====================

    private void openDetail(GocCayModel model) {
        Intent intent = new Intent(this, GocCayDetailActivity.class);
        intent.putExtra(GocCayDetailActivity.EXTRA_GOC_CAY_ID, model.getId());
        intent.putExtra(GocCayDetailActivity.EXTRA_MA_QR, model.getMaQRCode());
        intent.putExtra(GocCayDetailActivity.EXTRA_VI_TRI, model.getViTri());
        intent.putExtra(GocCayDetailActivity.EXTRA_TRANG_THAI, model.getTrangThai());
        intent.putExtra(GocCayDetailActivity.EXTRA_NGAY_TRONG, model.getNgayTrong());
        intent.putExtra(GocCayDetailActivity.EXTRA_GHI_CHU, model.getGhiChu());
        intent.putExtra(GocCayDetailActivity.EXTRA_CAY_TRONG_ID, model.getCayTrongId());
        intent.putExtra(GocCayDetailActivity.EXTRA_USER_ID, model.getUserId());
        startActivity(intent);
    }

    // ==================== Form ====================


    private void openForm(GocCayModel model) {
        Intent intent = new Intent(this, GocCayFormActivity.class);
        intent.putExtra(GocCayFormActivity.EXTRA_CAY_TRONG_ID, cayTrongId);
        if (model != null) {
            intent.putExtra(GocCayFormActivity.EXTRA_MODE, GocCayFormActivity.MODE_EDIT);
            intent.putExtra(GocCayFormActivity.EXTRA_ID, model.getId());
            intent.putExtra(GocCayFormActivity.EXTRA_MA_QR, model.getMaQRCode());
            intent.putExtra(GocCayFormActivity.EXTRA_VI_TRI, model.getViTri());
            intent.putExtra(GocCayFormActivity.EXTRA_TRANG_THAI, model.getTrangThai());
            intent.putExtra(GocCayFormActivity.EXTRA_NGAY_TRONG, model.getNgayTrong());
            intent.putExtra(GocCayFormActivity.EXTRA_GHI_CHU, model.getGhiChu());
            intent.putExtra(GocCayFormActivity.EXTRA_USER_ID, model.getUserId());
        } else {
            intent.putExtra(GocCayFormActivity.EXTRA_MODE, GocCayFormActivity.MODE_CREATE);
        }
        startActivity(intent);
    }

    // ==================== Delete Dialog ====================

    public void showDeleteDialog(GocCayModel model) {
        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_xoa_title)
            .setMessage(getString(R.string.dialog_xoa_message, model.getMaQRCode()))
            .setNegativeButton(R.string.huy, (d, w) -> d.dismiss())
            .setPositiveButton(R.string.xoa, (d, w) -> {
                d.dismiss();
                viewModel.deleteGocCay(model).observe(this, result -> {
                    if (result == null) return;
                    if (result.isSuccess())
                        Snackbar.make(binding.getRoot(),
                            R.string.xoa_thanh_cong, Snackbar.LENGTH_SHORT).show();
                    else if (result.isError())
                        Snackbar.make(binding.getRoot(),
                            result.message != null ? result.message : getString(R.string.error_xoa),
                            Snackbar.LENGTH_SHORT).show();
                });
            }).show();
    }

    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }
}
