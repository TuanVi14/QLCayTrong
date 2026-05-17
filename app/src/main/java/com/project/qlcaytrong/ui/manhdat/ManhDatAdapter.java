// File: app/src/main/java/com/project/qlcaytrong/ui/manhdat/ManhDatAdapter.java
package com.project.qlcaytrong.ui.manhdat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.project.qlcaytrong.R;
import com.project.qlcaytrong.databinding.ItemManhDatBinding;
import com.project.qlcaytrong.model.ManhDatModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

/**
 * RecyclerView Adapter dùng ListAdapter + DiffUtil để update hiệu quả.
 * Expose 2 callback: onEdit và onDelete.
 */
public class ManhDatAdapter extends ListAdapter<ManhDatModel, ManhDatAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onEdit(ManhDatModel model);
        void onDelete(ManhDatModel model);
        void onClick(ManhDatModel model);
    }

    private final OnItemClickListener listener;
    private static final SimpleDateFormat DATE_FMT =
        new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    public ManhDatAdapter(OnItemClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemManhDatBinding binding = ItemManhDatBinding.inflate(
            LayoutInflater.from(parent.getContext()), parent, false
        );
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    // ==================== ViewHolder ====================

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemManhDatBinding b;

        ViewHolder(ItemManhDatBinding binding) {
            super(binding.getRoot());
            this.b = binding;
        }

        void bind(ManhDatModel model) {
            b.tvTenManhDat.setText(model.getTenManhDat());
            b.tvDiaChi.setText(model.getDiaChi() != null && !model.getDiaChi().isEmpty()
                ? model.getDiaChi()
                : b.getRoot().getContext().getString(R.string.chua_co_dia_chi));
            b.tvDienTich.setText(model.getDienTichFormatted());
            b.tvNgayTao.setText(DATE_FMT.format(new Date(model.getNgayTao())));

            // Badge trạng thái sync
            if (model.isPending()) {
                b.tvSyncStatus.setVisibility(View.VISIBLE);
                b.tvSyncStatus.setText(R.string.chua_dong_bo);
            } else if ("FAILED".equals(model.getSyncStatus())) {
                b.tvSyncStatus.setVisibility(View.VISIBLE);
                b.tvSyncStatus.setText(R.string.dong_bo_that_bai);
            } else {
                b.tvSyncStatus.setVisibility(View.GONE);
            }

            // Click handlers
            b.getRoot().setOnClickListener(v -> listener.onClick(model));
            b.btnEdit.setOnClickListener(v -> listener.onEdit(model));
            b.btnDelete.setOnClickListener(v -> listener.onDelete(model));
        }
    }

    // ==================== DiffUtil ====================

    private static final DiffUtil.ItemCallback<ManhDatModel> DIFF_CALLBACK =
        new DiffUtil.ItemCallback<ManhDatModel>() {
            @Override
            public boolean areItemsTheSame(@NonNull ManhDatModel a, @NonNull ManhDatModel b) {
                return Objects.equals(a.getId(), b.getId());
            }

            @Override
            public boolean areContentsTheSame(@NonNull ManhDatModel a, @NonNull ManhDatModel b) {
                return Objects.equals(a.getTenManhDat(), b.getTenManhDat())
                    && Objects.equals(a.getDiaChi(), b.getDiaChi())
                    && a.getDienTich() == b.getDienTich()
                    && Objects.equals(a.getSyncStatus(), b.getSyncStatus());
            }
        };
}
