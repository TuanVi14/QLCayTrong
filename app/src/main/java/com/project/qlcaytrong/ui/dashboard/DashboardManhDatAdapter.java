package com.project.qlcaytrong.ui.dashboard;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.project.qlcaytrong.R;
import com.project.qlcaytrong.databinding.ItemManhDatBinding;
import com.project.qlcaytrong.model.ManhDatModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DashboardManhDatAdapter extends RecyclerView.Adapter<DashboardManhDatAdapter.ViewHolder> {
    private final List<ManhDatModel> data = new ArrayList<>();
    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    public void setData(List<ManhDatModel> list) {
        data.clear();
        if (list != null) data.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemManhDatBinding binding = ItemManhDatBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(data.get(position));
    }

    @Override
    public int getItemCount() { return data.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemManhDatBinding b;
        ViewHolder(ItemManhDatBinding binding) { super(binding.getRoot()); this.b = binding; }
        void bind(ManhDatModel m) {
            b.tvTenManhDat.setText(m.getTenManhDat());
            b.tvDiaChi.setText(m.getDiaChi() != null && !m.getDiaChi().isEmpty() ? m.getDiaChi() : b.getRoot().getContext().getString(R.string.chua_co_dia_chi));
            b.tvDienTich.setText(m.getDienTichFormatted());
            b.tvNgayTao.setText(m.getNgayTao() > 0 ? DATE_FMT.format(new Date(m.getNgayTao())) : "—");
            b.tvSyncStatus.setVisibility(View.GONE);
            b.btnEdit.setVisibility(View.GONE);
            b.btnDelete.setVisibility(View.GONE);
        }
    }
}
