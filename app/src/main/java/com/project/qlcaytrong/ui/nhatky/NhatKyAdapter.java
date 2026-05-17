// File: app/src/main/java/com/project/qlcaytrong/ui/nhatky/NhatKyAdapter.java
package com.project.qlcaytrong.ui.nhatky;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.project.qlcaytrong.R;
import com.project.qlcaytrong.databinding.ItemNhatKyBinding;
import com.project.qlcaytrong.model.NhatKyModel;
import com.project.qlcaytrong.util.ImageLoader;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

/** NhatKyAdapter — hiển thị nhật ký theo loại với icon + màu tương ứng */
public class NhatKyAdapter extends ListAdapter<NhatKyModel, NhatKyAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(NhatKyModel model);
        void onDelete(NhatKyModel model);
    }

    private final OnItemClickListener listener;
    private static final SimpleDateFormat DATE_FMT =
        new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    public NhatKyAdapter(OnItemClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemNhatKyBinding b = ItemNhatKyBinding.inflate(
            LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(b);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemNhatKyBinding b;

        ViewHolder(ItemNhatKyBinding binding) {
            super(binding.getRoot());
            this.b = binding;
        }

        void bind(NhatKyModel m) {
            // Loại nhật ký → icon + màu
            bindLoai(m.getLoaiNhatKy());

            // Ngày thực hiện
            b.tvNgayThucHien.setText(DATE_FMT.format(new Date(m.getNgayThucHien())));

            // Người thực hiện
            b.tvNguoiThucHien.setText(
                m.getNguoiThucHien() != null && !m.getNguoiThucHien().isEmpty()
                    ? m.getNguoiThucHien()
                    : "—");

            // Ghi chú (giới hạn 2 dòng)
            b.tvGhiChu.setText(
                m.getGhiChu() != null && !m.getGhiChu().isEmpty()
                    ? m.getGhiChu() : "");

            // Sync badge
            b.tvSyncBadge.setVisibility(
                "PENDING".equals(m.getSyncStatus()) || "FAILED".equals(m.getSyncStatus())
                    ? android.view.View.VISIBLE : android.view.View.GONE);

            // Thumbnail
            boolean hasImage = m.getHinhAnh() != null && !m.getHinhAnh().isEmpty();
            b.ivThumbnail.setVisibility(hasImage ? android.view.View.VISIBLE : android.view.View.GONE);
            if (hasImage) {
                ImageLoader.loadThumbnail(b.getRoot().getContext(), m.getHinhAnh(), b.ivThumbnail);
            }

            b.getRoot().setOnClickListener(v -> listener.onItemClick(m));
            b.btnDelete.setOnClickListener(v -> listener.onDelete(m));
        }

        private void bindLoai(String loai) {
            if (loai == null) return;
            switch (loai) {
                case "TUOI_PHAN":
                    b.tvLoai.setText(R.string.loai_tuoi_phan);
                    b.tvLoai.setBackgroundResource(R.drawable.bg_chip_green);
                    b.ivIcon.setImageResource(android.R.drawable.ic_menu_add);
                    break;
                case "PHUN_THUOC":
                    b.tvLoai.setText(R.string.loai_phun_thuoc);
                    b.tvLoai.setBackgroundResource(R.drawable.bg_chip_red);
                    b.ivIcon.setImageResource(android.R.drawable.ic_menu_report_image);
                    break;
                case "TINH_HINH":
                    b.tvLoai.setText(R.string.loai_tinh_hinh);
                    b.tvLoai.setBackgroundResource(R.drawable.bg_chip_orange);
                    b.ivIcon.setImageResource(android.R.drawable.ic_menu_info_details);
                    break;
                case "THU_HOACH":
                    b.tvLoai.setText(R.string.loai_thu_hoach);
                    b.tvLoai.setBackgroundResource(R.drawable.bg_chip_green);
                    b.ivIcon.setImageResource(android.R.drawable.ic_menu_gallery);
                    break;
                default:
                    b.tvLoai.setText(loai);
            }
        }
    }

    private static final DiffUtil.ItemCallback<NhatKyModel> DIFF_CALLBACK =
        new DiffUtil.ItemCallback<NhatKyModel>() {
            @Override
            public boolean areItemsTheSame(@NonNull NhatKyModel a, @NonNull NhatKyModel b) {
                return Objects.equals(a.getId(), b.getId());
            }

            @Override
            public boolean areContentsTheSame(@NonNull NhatKyModel a, @NonNull NhatKyModel b) {
                return Objects.equals(a.getLoaiNhatKy(), b.getLoaiNhatKy())
                    && a.getNgayThucHien() == b.getNgayThucHien()
                    && Objects.equals(a.getSyncStatus(), b.getSyncStatus());
            }
        };
}
