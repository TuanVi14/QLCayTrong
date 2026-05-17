// File: app/src/main/java/com/project/qlcaytrong/ui/caytrong/CayTrongAdapter.java
package com.project.qlcaytrong.ui.caytrong;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.project.qlcaytrong.R;
import com.project.qlcaytrong.databinding.ItemCayTrongBinding;
import com.project.qlcaytrong.model.CayTrongModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

/**
 * CayTrongAdapter — ListAdapter với DiffUtil cho performance tốt nhất.
 * Chỉ re-draw item thực sự thay đổi.
 */
public class CayTrongAdapter extends ListAdapter<CayTrongModel, CayTrongAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onEdit(CayTrongModel model);
        void onItemClick(CayTrongModel model); // Navigate đến GocCay
    }

    private final OnItemClickListener listener;
    private static final SimpleDateFormat DATE_FMT =
        new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    public CayTrongAdapter(OnItemClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCayTrongBinding b = ItemCayTrongBinding.inflate(
            LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(b);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    // ==================== ViewHolder ====================

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemCayTrongBinding b;

        ViewHolder(ItemCayTrongBinding binding) {
            super(binding.getRoot());
            this.b = binding;
        }

        void bind(CayTrongModel m) {
            b.tvLoaiCay.setText(m.getLoaiCay());
            b.tvTenKhoaHoc.setText(
                m.getTenKhoaHoc() != null && !m.getTenKhoaHoc().isEmpty()
                    ? m.getTenKhoaHoc()
                    : b.getRoot().getContext().getString(R.string.ten_khoa_hoc));
            b.tvSoLuong.setText(m.getSoLuong() + " " + m.getDonViTinh());
            b.tvNgayTrong.setText(DATE_FMT.format(new Date(m.getNgayTrong())));

            // Status chip
            bindStatusChip(m.getTrangThai());

            // Sync badge
            if ("PENDING".equals(m.getSyncStatus())) {
                b.tvSyncBadge.setVisibility(android.view.View.VISIBLE);
                b.tvSyncBadge.setText(R.string.chua_dong_bo);
            } else if ("FAILED".equals(m.getSyncStatus())) {
                b.tvSyncBadge.setVisibility(android.view.View.VISIBLE);
                b.tvSyncBadge.setText(R.string.dong_bo_that_bai);
            } else {
                b.tvSyncBadge.setVisibility(android.view.View.GONE);
            }

            b.getRoot().setOnClickListener(v -> listener.onItemClick(m));
            b.btnEdit.setOnClickListener(v -> listener.onEdit(m));
        }

        private void bindStatusChip(String trangThai) {
            if (trangThai == null) return;
            android.content.Context ctx = b.getRoot().getContext();
            switch (trangThai) {
                case "DANG_TRONG":
                    b.tvTrangThai.setText(R.string.trang_thai_dang_trong);
                    b.tvTrangThai.setBackgroundResource(R.drawable.bg_chip_green);
                    break;
                case "THU_HOACH":
                    b.tvTrangThai.setText(R.string.trang_thai_thu_hoach);
                    b.tvTrangThai.setBackgroundResource(R.drawable.bg_chip_orange);
                    break;
                case "HU_HONG":
                    b.tvTrangThai.setText(R.string.trang_thai_hu_hong);
                    b.tvTrangThai.setBackgroundResource(R.drawable.bg_chip_red);
                    break;
                default:
                    b.tvTrangThai.setText(trangThai);
                    b.tvTrangThai.setBackgroundResource(R.drawable.bg_chip_green);
            }
        }
    }

    // ==================== DiffUtil ====================

    private static final DiffUtil.ItemCallback<CayTrongModel> DIFF_CALLBACK =
        new DiffUtil.ItemCallback<CayTrongModel>() {
            @Override
            public boolean areItemsTheSame(@NonNull CayTrongModel a, @NonNull CayTrongModel b) {
                return Objects.equals(a.getId(), b.getId());
            }

            @Override
            public boolean areContentsTheSame(@NonNull CayTrongModel a, @NonNull CayTrongModel b) {
                return Objects.equals(a.getLoaiCay(), b.getLoaiCay())
                    && a.getSoLuong() == b.getSoLuong()
                    && Objects.equals(a.getTrangThai(), b.getTrangThai())
                    && Objects.equals(a.getSyncStatus(), b.getSyncStatus());
            }
        };
}
