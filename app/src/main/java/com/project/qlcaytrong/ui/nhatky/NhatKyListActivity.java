// File: app/src/main/java/com/project/qlcaytrong/ui/nhatky/NhatKyListActivity.java
package com.project.qlcaytrong.ui.nhatky;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.project.qlcaytrong.R;
import com.project.qlcaytrong.databinding.ActivityNhatKyListBinding;
import com.project.qlcaytrong.model.NhatKyModel;
import com.project.qlcaytrong.viewmodel.NhatKyViewModel;

import java.util.List;

/**
 * NhatKyListActivity — danh sách nhật ký của 1 GocCay hoặc CayTrong.
 *
 * Intent extras:
 *  - EXTRA_GOC_CAY_ID: String (ưu tiên)
 *  - EXTRA_CAY_TRONG_ID: String (fallback nếu không có gocCayId)
 *  - EXTRA_LABEL: String (hiển thị subtitle toolbar)
 */
public class NhatKyListActivity extends AppCompatActivity
    implements NhatKyAdapter.OnItemClickListener {

    public static final String EXTRA_GOC_CAY_ID   = "goc_cay_id";
    public static final String EXTRA_CAY_TRONG_ID = "cay_trong_id";
    public static final String EXTRA_LABEL        = "label";

    private ActivityNhatKyListBinding binding;
    private NhatKyViewModel viewModel;
    private NhatKyAdapter adapter;
    private String gocCayId, cayTrongId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNhatKyListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        gocCayId   = getIntent().getStringExtra(EXTRA_GOC_CAY_ID);
        cayTrongId = getIntent().getStringExtra(EXTRA_CAY_TRONG_ID);
        String label = getIntent().getStringExtra(EXTRA_LABEL);

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.nhat_ky_title);
            if (label != null) getSupportActionBar().setSubtitle(label);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        viewModel = new ViewModelProvider(this).get(NhatKyViewModel.class);
        if (gocCayId != null)        viewModel.setGocCayId(gocCayId);
        else if (cayTrongId != null) viewModel.setCayTrongId(cayTrongId);

        setupRecyclerView();
        setupChipFilter();
        setupFab();
        observeData();
    }

    private void setupRecyclerView() {
        adapter = new NhatKyAdapter(this);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);
        binding.recyclerView.setHasFixedSize(false); // chiều cao thay đổi theo data
    }

    /**
     * ChipGroup filter theo loại nhật ký.
     * "Tất cả" checked → observe context list.
     * Chip cụ thể → observe getAllByLoai/getAllByGocCayAndLoai.
     */
    private void setupChipFilter() {
        binding.chipAll.setOnClickListener(v -> observeData());

        binding.chipTuoiPhan.setOnClickListener(v ->
            viewModel.getByLoai("TUOI_PHAN").observe(this, this::renderList));

        binding.chipPhunThuoc.setOnClickListener(v ->
            viewModel.getByLoai("PHUN_THUOC").observe(this, this::renderList));

        binding.chipTinhHinh.setOnClickListener(v ->
            viewModel.getByLoai("TINH_HINH").observe(this, this::renderList));

        binding.chipThuHoach.setOnClickListener(v ->
            viewModel.getByLoai("THU_HOACH").observe(this, this::renderList));
    }

    private void setupFab() {
        binding.fabAdd.setOnClickListener(v -> openForm(null));
    }

    private void observeData() {
        viewModel.getNhatKyList().observe(this, this::renderList);
    }

    private void renderList(List<NhatKyModel> list) {
        boolean empty = list == null || list.isEmpty();
        binding.layoutEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
        if (!empty) adapter.submitList(list);
    }

    // ==================== Adapter Callbacks ====================

    @Override
    public void onItemClick(NhatKyModel model) {
        Intent intent = new Intent(this, NhatKyDetailActivity.class);
        intent.putExtra(NhatKyDetailActivity.EXTRA_NHAT_KY_ID, model.getId());
        intent.putExtra(NhatKyDetailActivity.EXTRA_LOAI, model.getLoaiNhatKy());
        intent.putExtra(NhatKyDetailActivity.EXTRA_NGAY, model.getNgayThucHien());
        intent.putExtra(NhatKyDetailActivity.EXTRA_NGUOI, model.getNguoiThucHien());
        intent.putExtra(NhatKyDetailActivity.EXTRA_GHI_CHU, model.getGhiChu());
        intent.putExtra(NhatKyDetailActivity.EXTRA_GOC_CAY_ID, model.getGocCayId());
        intent.putExtra(NhatKyDetailActivity.EXTRA_CAY_TRONG_ID, model.getCayTrongId());
        intent.putExtra(NhatKyDetailActivity.EXTRA_USER_ID, model.getUserId());
        startActivity(intent);
    }

    @Override
    public void onDelete(NhatKyModel model) {
        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_xoa_title)
            .setMessage(R.string.dialog_xoa_nhat_ky)
            .setNegativeButton(R.string.huy, (d, w) -> d.dismiss())
            .setPositiveButton(R.string.xoa, (d, w) -> {
                d.dismiss();
                viewModel.deleteNhatKy(model).observe(this, result -> {
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

    private void openForm(NhatKyModel model) {
        Intent intent = new Intent(this, NhatKyFormActivity.class);
        intent.putExtra(NhatKyFormActivity.EXTRA_GOC_CAY_ID, gocCayId);
        intent.putExtra(NhatKyFormActivity.EXTRA_CAY_TRONG_ID, cayTrongId);
        if (model != null) {
            intent.putExtra(NhatKyFormActivity.EXTRA_MODE, NhatKyFormActivity.MODE_EDIT);
            intent.putExtra(NhatKyFormActivity.EXTRA_NHAT_KY_ID, model.getId());
            intent.putExtra(NhatKyFormActivity.EXTRA_LOAI, model.getLoaiNhatKy());
            intent.putExtra(NhatKyFormActivity.EXTRA_NGAY, model.getNgayThucHien());
            intent.putExtra(NhatKyFormActivity.EXTRA_NGUOI, model.getNguoiThucHien());
            intent.putExtra(NhatKyFormActivity.EXTRA_GHI_CHU, model.getGhiChu());
        } else {
            intent.putExtra(NhatKyFormActivity.EXTRA_MODE, NhatKyFormActivity.MODE_CREATE);
        }
        startActivity(intent);
    }

    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }
}
