package com.project.qlcaytrong.ui.dashboard;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.project.qlcaytrong.R;
import com.project.qlcaytrong.databinding.ItemNhatKyBinding;
import com.project.qlcaytrong.model.NhatKyModel;
import com.project.qlcaytrong.util.ImageLoader;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DashboardNhatKyAdapter extends RecyclerView.Adapter<DashboardNhatKyAdapter.ViewHolder> {
    private final List<NhatKyModel> data = new ArrayList<>();
    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    public void setData(List<NhatKyModel> list) {
        data.clear();
        if (list != null) data.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemNhatKyBinding binding = ItemNhatKyBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(data.get(position));
    }

    @Override
    public int getItemCount() { return data.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemNhatKyBinding b;
        ViewHolder(ItemNhatKyBinding binding) { super(binding.getRoot()); this.b = binding; }
        void bind(NhatKyModel m) {
            b.tvNgayThucHien.setText(m.getNgayThucHien() > 0 ? DATE_FMT.format(new Date(m.getNgayThucHien())) : "—");
            b.tvNguoiThucHien.setText(m.getNguoiThucHien() != null && !m.getNguoiThucHien().isEmpty() ? m.getNguoiThucHien() : "—");
            b.tvGhiChu.setText(m.getGhiChu() != null ? m.getGhiChu() : "");
            b.tvSyncBadge.setVisibility(View.GONE);
            b.btnDelete.setVisibility(View.GONE);
            String loai = m.getLoaiNhatKy();
            if ("TUOI_PHAN".equals(loai)) b.tvLoai.setText(R.string.loai_tuoi_phan);
            else if ("PHUN_THUOC".equals(loai)) b.tvLoai.setText(R.string.loai_phun_thuoc);
            else if ("TINH_HINH".equals(loai)) b.tvLoai.setText(R.string.loai_tinh_hinh);
            else if ("THU_HOACH".equals(loai)) b.tvLoai.setText(R.string.loai_thu_hoach);
            else b.tvLoai.setText(loai != null ? loai : "—");
            boolean hasImage = m.getHinhAnh() != null && !m.getHinhAnh().isEmpty();
            b.ivThumbnail.setVisibility(hasImage ? View.VISIBLE : View.GONE);
            if (hasImage) ImageLoader.loadThumbnail(b.getRoot().getContext(), m.getHinhAnh(), b.ivThumbnail);
        }
    }
}
