// File: app/src/main/java/com/project/qlcaytrong/ui/nhatky/ChiTietTuoiPhanAdapter.java
package com.project.qlcaytrong.ui.nhatky;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.project.qlcaytrong.databinding.ItemChiTietTuoiPhanBinding;
import com.project.qlcaytrong.model.ChiTietTuoiPhanModel;

import java.util.ArrayList;
import java.util.List;

/**
 * ChiTietTuoiPhanAdapter — danh sách động inline trong NhatKyFormActivity.
 *
 * == Cách hiển thị danh sách động trong form ==
 * 1. Dùng RecyclerView KHÔNG scroll (setNestedScrollingEnabled = false)
 *    bên trong ScrollView của form
 * 2. Người dùng nhấn "+" → adapter.addRow() thêm model rỗng → notifyItemInserted
 * 3. Mỗi row có TextWatcher cập nhật model tương ứng khi người dùng nhập
 * 4. Nhấn "×" → adapter.removeRow(pos) → notifyItemRemoved
 * 5. NhatKyFormActivity.getChiTietList() lấy data từ adapter trước khi submit
 *
 * == Vấn đề TextWatcher trong RecyclerView ==
 * ViewHolder được tái sử dụng (recycled), TextWatcher cũ còn gắn vào EditText mới
 * → Giải pháp: dùng tag setTag/getTag để track vị trí, hoặc detach TextWatcher
 *   trong onBindViewHolder và re-attach sau khi setText.
 */
public class ChiTietTuoiPhanAdapter extends RecyclerView.Adapter<ChiTietTuoiPhanAdapter.ViewHolder> {

    private final List<ChiTietTuoiPhanModel> rows = new ArrayList<>();

    // ==================== Public API ====================

    public void addRow() {
        rows.add(new ChiTietTuoiPhanModel());
        notifyItemInserted(rows.size() - 1);
    }

    public void removeRow(int position) {
        if (position >= 0 && position < rows.size()) {
            rows.remove(position);
            notifyItemRemoved(position);
            // Cập nhật indexes sau khi xóa
            notifyItemRangeChanged(position, rows.size() - position);
        }
    }

    /** Gọi trước submit để lấy toàn bộ data */
    public List<ChiTietTuoiPhanModel> getRows() {
        return new ArrayList<>(rows);
    }

    /** Populate existing data (khi edit nhật ký cũ) */
    public void setRows(List<ChiTietTuoiPhanModel> data) {
        rows.clear();
        if (data != null) rows.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemChiTietTuoiPhanBinding b = ItemChiTietTuoiPhanBinding.inflate(
            LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(b);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(rows.get(position), position);
    }

    @Override
    public int getItemCount() { return rows.size(); }

    // ==================== ViewHolder ====================

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemChiTietTuoiPhanBinding b;
        // TextWatcher references để detach trước khi setText (tránh double-trigger)
        private TextWatcher tenPhanWatcher, lieuLuongWatcher, donViWatcher, cachBonWatcher;

        ViewHolder(ItemChiTietTuoiPhanBinding binding) {
            super(binding.getRoot());
            this.b = binding;
        }

        void bind(ChiTietTuoiPhanModel model, int pos) {
            // Detach watchers trước khi setText để tránh infinite loop
            detachWatchers();

            b.etTenPhan.setText(model.getTenPhan());
            b.etLieuLuong.setText(model.getLieuLuong() > 0
                ? String.valueOf(model.getLieuLuong()) : "");
            b.etDonVi.setText(model.getDonVi());
            b.etCachBon.setText(model.getCachBon());

            // Re-attach watchers
            tenPhanWatcher = simpleWatcher(s -> model.setTenPhan(s.toString().trim()));
            lieuLuongWatcher = simpleWatcher(s -> {
                try { model.setLieuLuong(Double.parseDouble(s.toString())); }
                catch (NumberFormatException e) { model.setLieuLuong(0); }
            });
            donViWatcher  = simpleWatcher(s -> model.setDonVi(s.toString().trim()));
            cachBonWatcher = simpleWatcher(s -> model.setCachBon(s.toString().trim()));

            b.etTenPhan.addTextChangedListener(tenPhanWatcher);
            b.etLieuLuong.addTextChangedListener(lieuLuongWatcher);
            b.etDonVi.addTextChangedListener(donViWatcher);
            b.etCachBon.addTextChangedListener(cachBonWatcher);

            // Số thứ tự
            b.tvRowNumber.setText(String.valueOf(pos + 1));

            // Nút xóa row
            b.btnRemove.setOnClickListener(v -> {
                int adapterPos = getAdapterPosition();
                if (adapterPos != RecyclerView.NO_ID) removeRow(adapterPos);
            });
        }

        private void detachWatchers() {
            if (tenPhanWatcher  != null) b.etTenPhan.removeTextChangedListener(tenPhanWatcher);
            if (lieuLuongWatcher != null) b.etLieuLuong.removeTextChangedListener(lieuLuongWatcher);
            if (donViWatcher    != null) b.etDonVi.removeTextChangedListener(donViWatcher);
            if (cachBonWatcher  != null) b.etCachBon.removeTextChangedListener(cachBonWatcher);
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
