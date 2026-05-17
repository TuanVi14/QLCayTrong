// File: app/src/main/java/com/project/qlcaytrong/ui/goccay/GocCayAdapter.java
package com.project.qlcaytrong.ui.goccay;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.project.qlcaytrong.R;
import com.project.qlcaytrong.databinding.ItemGocCayBinding;
import com.project.qlcaytrong.model.GocCayModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

/** GocCayAdapter — ListAdapter+DiffUtil, badge màu theo trangThai, QR code display */
public class GocCayAdapter extends ListAdapter<GocCayModel, GocCayAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onEdit(GocCayModel model);
        void onViewQR(GocCayModel model);
        void onItemClick(GocCayModel model); // Navigate to NhatKy
    }

    private final OnItemClickListener listener;
    private static final SimpleDateFormat DATE_FMT =
        new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    public GocCayAdapter(OnItemClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemGocCayBinding b = ItemGocCayBinding.inflate(
            LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(b);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemGocCayBinding b;

        ViewHolder(ItemGocCayBinding binding) {
            super(binding.getRoot());
            this.b = binding;
        }

        void bind(GocCayModel m) {
            b.tvMaQR.setText(m.getMaQRCode() != null ? m.getMaQRCode() : "N/A");
            b.tvViTri.setText(m.getViTri() != null && !m.getViTri().isEmpty()
                ? m.getViTri()
                : b.getRoot().getContext().getString(R.string.vi_tri));
            b.tvNgayTrong.setText(DATE_FMT.format(new Date(m.getNgayTrong())));
            b.tvGhiChu.setText(m.getGhiChu() != null && !m.getGhiChu().isEmpty()
                ? m.getGhiChu() : "");

            // Badge trạng thái
            bindStatusBadge(m.getTrangThai());

            // Sync badge
            if ("PENDING".equals(m.getSyncStatus())) {
                b.tvSyncBadge.setVisibility(android.view.View.VISIBLE);
                b.tvSyncBadge.setText(R.string.chua_dong_bo);
            } else {
                b.tvSyncBadge.setVisibility(android.view.View.GONE);
            }

            b.getRoot().setOnClickListener(v -> listener.onItemClick(m));
            b.btnEdit.setOnClickListener(v -> listener.onEdit(m));
            b.btnViewQR.setOnClickListener(v -> listener.onViewQR(m));
        }

        private void bindStatusBadge(String trangThai) {
            if (trangThai == null) return;
            switch (trangThai) {
                case "TOT":
                    b.tvTrangThai.setText(R.string.trang_thai_tot);
                    b.tvTrangThai.setBackgroundResource(R.drawable.bg_badge_tot);
                    break;
                case "BINH_THUONG":
                    b.tvTrangThai.setText(R.string.trang_thai_binh_thuong);
                    b.tvTrangThai.setBackgroundResource(R.drawable.bg_badge_binh_thuong);
                    break;
                case "XAU":
                    b.tvTrangThai.setText(R.string.trang_thai_xau);
                    b.tvTrangThai.setBackgroundResource(R.drawable.bg_badge_xau);
                    break;
                case "CHET":
                    b.tvTrangThai.setText(R.string.trang_thai_chet);
                    b.tvTrangThai.setBackgroundResource(R.drawable.bg_badge_xau);
                    break;
                default:
                    b.tvTrangThai.setText(trangThai);
                    b.tvTrangThai.setBackgroundResource(R.drawable.bg_badge_binh_thuong);
            }
        }
    }

    private static final DiffUtil.ItemCallback<GocCayModel> DIFF_CALLBACK =
        new DiffUtil.ItemCallback<GocCayModel>() {
            @Override
            public boolean areItemsTheSame(@NonNull GocCayModel a, @NonNull GocCayModel b) {
                return Objects.equals(a.getId(), b.getId());
            }

            @Override
            public boolean areContentsTheSame(@NonNull GocCayModel a, @NonNull GocCayModel b) {
                return Objects.equals(a.getTrangThai(), b.getTrangThai())
                    && Objects.equals(a.getViTri(), b.getViTri())
                    && Objects.equals(a.getSyncStatus(), b.getSyncStatus());
            }
        };
}
