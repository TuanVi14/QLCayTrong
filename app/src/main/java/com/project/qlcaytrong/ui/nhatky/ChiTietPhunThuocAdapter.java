// File: app/src/main/java/com/project/qlcaytrong/ui/nhatky/ChiTietPhunThuocAdapter.java
package com.project.qlcaytrong.ui.nhatky;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.project.qlcaytrong.databinding.ItemChiTietPhunThuocBinding;
import com.project.qlcaytrong.model.ChiTietPhunThuocModel;

import java.util.ArrayList;
import java.util.List;

/** ChiTietPhunThuocAdapter — giống TuoiPhan nhưng với fields: tenThuoc, lieuLuong, donVi, lyDoPhun */
public class ChiTietPhunThuocAdapter extends RecyclerView.Adapter<ChiTietPhunThuocAdapter.ViewHolder> {

    private final List<ChiTietPhunThuocModel> rows = new ArrayList<>();

    public void addRow() {
        rows.add(new ChiTietPhunThuocModel());
        notifyItemInserted(rows.size() - 1);
    }

    public void removeRow(int position) {
        if (position >= 0 && position < rows.size()) {
            rows.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, rows.size() - position);
        }
    }

    public List<ChiTietPhunThuocModel> getRows() { return new ArrayList<>(rows); }

    public void setRows(List<ChiTietPhunThuocModel> data) {
        rows.clear();
        if (data != null) rows.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemChiTietPhunThuocBinding b = ItemChiTietPhunThuocBinding.inflate(
            LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(b);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(rows.get(position), position);
    }

    @Override
    public int getItemCount() { return rows.size(); }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemChiTietPhunThuocBinding b;
        private TextWatcher tenThuocWatcher, lieuLuongWatcher, donViWatcher, lyDoWatcher;

        ViewHolder(ItemChiTietPhunThuocBinding binding) {
            super(binding.getRoot());
            this.b = binding;
        }

        void bind(ChiTietPhunThuocModel model, int pos) {
            detachWatchers();

            b.etTenThuoc.setText(model.getTenThuoc());
            b.etLieuLuong.setText(model.getLieuLuong() > 0 ? String.valueOf(model.getLieuLuong()) : "");
            b.etDonVi.setText(model.getDonVi());
            b.etLyDoPhun.setText(model.getLyDoPhun());

            tenThuocWatcher  = simpleWatcher(s -> model.setTenThuoc(s.toString().trim()));
            lieuLuongWatcher = simpleWatcher(s -> {
                try { model.setLieuLuong(Double.parseDouble(s.toString())); }
                catch (NumberFormatException e) { model.setLieuLuong(0); }
            });
            donViWatcher = simpleWatcher(s -> model.setDonVi(s.toString().trim()));
            lyDoWatcher  = simpleWatcher(s -> model.setLyDoPhun(s.toString().trim()));

            b.etTenThuoc.addTextChangedListener(tenThuocWatcher);
            b.etLieuLuong.addTextChangedListener(lieuLuongWatcher);
            b.etDonVi.addTextChangedListener(donViWatcher);
            b.etLyDoPhun.addTextChangedListener(lyDoWatcher);

            b.tvRowNumber.setText(String.valueOf(pos + 1));
            b.btnRemove.setOnClickListener(v -> {
                int adapterPos = getAdapterPosition();
                if (adapterPos != RecyclerView.NO_ID) removeRow(adapterPos);
            });
        }

        private void detachWatchers() {
            if (tenThuocWatcher  != null) b.etTenThuoc.removeTextChangedListener(tenThuocWatcher);
            if (lieuLuongWatcher != null) b.etLieuLuong.removeTextChangedListener(lieuLuongWatcher);
            if (donViWatcher     != null) b.etDonVi.removeTextChangedListener(donViWatcher);
            if (lyDoWatcher      != null) b.etLyDoPhun.removeTextChangedListener(lyDoWatcher);
        }

        private TextWatcher simpleWatcher(java.util.function.Consumer<Editable> consumer) {
            return new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
                @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
                @Override public void afterTextChanged(Editable s) { consumer.accept(s); }
            };
        }
    }
}
