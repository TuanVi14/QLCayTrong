// File: app/src/main/java/com/project/qlcaytrong/ui/caytrong/CayTrongListActivity.java
package com.project.qlcaytrong.ui.caytrong;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.project.qlcaytrong.R;
import com.project.qlcaytrong.databinding.ActivityCayTrongListBinding;
import com.project.qlcaytrong.model.CayTrongModel;
import com.project.qlcaytrong.ui.goccay.GocCayListActivity;
import com.project.qlcaytrong.viewmodel.CayTrongViewModel;

import java.util.List;

/**
 * CayTrongListActivity — danh sách cây trồng trong 1 mảnh đất.
 *
 * Navigation flow: ManhDatListActivity → CayTrongListActivity → GocCayListActivity
 *
 * Intent extras nhận vào:
 *  - EXTRA_MANH_DAT_ID: String (bắt buộc)
 *  - EXTRA_TEN_MANH_DAT: String (hiển thị ở toolbar)
 */
public class CayTrongListActivity extends AppCompatActivity
    implements CayTrongAdapter.OnItemClickListener {

    public static final String EXTRA_MANH_DAT_ID   = "manh_dat_id";
    public static final String EXTRA_TEN_MANH_DAT  = "ten_manh_dat";

    private ActivityCayTrongListBinding binding;
    private CayTrongViewModel viewModel;
    private CayTrongAdapter adapter;
    private String manhDatId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCayTrongListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        manhDatId = getIntent().getStringExtra(EXTRA_MANH_DAT_ID);
        String tenManhDat = getIntent().getStringExtra(EXTRA_TEN_MANH_DAT);

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(tenManhDat != null ? tenManhDat : getString(R.string.cay_trong_title));
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        viewModel = new ViewModelProvider(this).get(CayTrongViewModel.class);
        if (manhDatId != null) viewModel.setManhDatId(manhDatId);

        setupRecyclerView();
        setupSwipeToDelete();
        setupFab();
        setupSwipeRefresh();
        observeData();
    }

    // ==================== Setup ====================

    private void setupRecyclerView() {
        adapter = new CayTrongAdapter(this);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);
        binding.recyclerView.setHasFixedSize(true);
    }

    /**
     * Swipe sang trái để xóa với nền đỏ.
     * Hiển thị confirm dialog trước khi xóa thật.
     */
    private void setupSwipeToDelete() {
        ColorDrawable swipeBg = new ColorDrawable(
            ContextCompat.getColor(this, R.color.swipe_delete_bg));

        ItemTouchHelper.SimpleCallback callback =
            new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

                @Override
                public boolean onMove(@NonNull RecyclerView rv,
                                      @NonNull RecyclerView.ViewHolder vh,
                                      @NonNull RecyclerView.ViewHolder target) {
                    return false; // Không hỗ trợ drag-drop
                }

                @Override
                public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                    int pos = viewHolder.getAdapterPosition();
                    CayTrongModel model = adapter.getCurrentList().get(pos);

                    // Restore item trước khi hỏi (tránh item bị xóa khỏi RecyclerView trước khi confirm)
                    adapter.notifyItemChanged(pos);
                    showDeleteDialog(model);
                }

                @Override
                public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView rv,
                                        @NonNull RecyclerView.ViewHolder vh,
                                        float dX, float dY, int state, boolean active) {
                    View item = vh.itemView;
                    swipeBg.setBounds(item.getRight() + (int) dX, item.getTop(),
                        item.getRight(), item.getBottom());
                    swipeBg.draw(c);
                    super.onChildDraw(c, rv, vh, dX, dY, state, active);
                }
            };

        new ItemTouchHelper(callback).attachToRecyclerView(binding.recyclerView);
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
        viewModel.getCayTrongList().observe(this, this::renderList);
    }

    // ==================== Render ====================

    private void renderList(List<CayTrongModel> list) {
        boolean empty = list == null || list.isEmpty();
        binding.layoutEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
        if (!empty) adapter.submitList(list);
    }

    // ==================== Adapter Callbacks ====================

    @Override
    public void onItemClick(CayTrongModel model) {
        Intent intent = new Intent(this, GocCayListActivity.class);
        intent.putExtra(GocCayListActivity.EXTRA_CAY_TRONG_ID, model.getId());
        intent.putExtra(GocCayListActivity.EXTRA_LOAI_CAY, model.getLoaiCay());
        startActivity(intent);
    }

    @Override
    public void onEdit(CayTrongModel model) {
        openForm(model);
    }

    // ==================== Form Navigation ====================

    private void openForm(CayTrongModel model) {
        Intent intent = new Intent(this, CayTrongFormActivity.class);
        intent.putExtra(CayTrongFormActivity.EXTRA_MANH_DAT_ID, manhDatId);
        if (model != null) {
            intent.putExtra(CayTrongFormActivity.EXTRA_MODE, CayTrongFormActivity.MODE_EDIT);
            intent.putExtra(CayTrongFormActivity.EXTRA_ID, model.getId());
            intent.putExtra(CayTrongFormActivity.EXTRA_LOAI_CAY, model.getLoaiCay());
            intent.putExtra(CayTrongFormActivity.EXTRA_TEN_KHOA_HOC, model.getTenKhoaHoc());
            intent.putExtra(CayTrongFormActivity.EXTRA_SO_LUONG, model.getSoLuong());
            intent.putExtra(CayTrongFormActivity.EXTRA_DON_VI_TINH, model.getDonViTinh());
            intent.putExtra(CayTrongFormActivity.EXTRA_NGAY_TRONG, model.getNgayTrong());
            intent.putExtra(CayTrongFormActivity.EXTRA_TRANG_THAI, model.getTrangThai());
            intent.putExtra(CayTrongFormActivity.EXTRA_MO_TA, model.getMoTa());
            intent.putExtra(CayTrongFormActivity.EXTRA_USER_ID, model.getUserId());
        } else {
            intent.putExtra(CayTrongFormActivity.EXTRA_MODE, CayTrongFormActivity.MODE_CREATE);
        }
        startActivity(intent);
    }

    // ==================== Delete Dialog ====================

    private void showDeleteDialog(CayTrongModel model) {
        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_xoa_title)
            .setMessage(getString(R.string.dialog_xoa_message, model.getLoaiCay()))
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setNegativeButton(R.string.huy, (d, w) -> d.dismiss())
            .setPositiveButton(R.string.xoa, (d, w) -> {
                d.dismiss();
                viewModel.deleteCayTrong(model).observe(this, result -> {
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
