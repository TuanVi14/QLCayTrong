// File: app/src/main/java/com/project/qlcaytrong/model/NhatKyWithChiTiet.java
package com.project.qlcaytrong.model;

import java.util.ArrayList;
import java.util.List;

/**
 * NhatKyWithChiTiet — aggregation POJO cho UI.
 *
 * Room không có @Relation eager-loading theo cách thủ công,
 * ta build object này ở ViewModel/Repository level.
 *
 * Sơ đồ quan hệ:
 *   NhatKy 1 ──── nhiều ChiTietTuoiPhan  (khi loai = TUOI_PHAN)
 *   NhatKy 1 ──── nhiều ChiTietPhunThuoc (khi loai = PHUN_THUOC)
 *   TINH_HINH / THU_HOACH → không có ChiTiet, chỉ dùng ghiChu
 */
public class NhatKyWithChiTiet {

    private NhatKyModel nhatKy;
    private List<ChiTietTuoiPhanModel> danhSachTuoiPhan;
    private List<ChiTietPhunThuocModel> danhSachPhunThuoc;

    public NhatKyWithChiTiet(NhatKyModel nhatKy) {
        this.nhatKy = nhatKy;
        this.danhSachTuoiPhan  = new ArrayList<>();
        this.danhSachPhunThuoc = new ArrayList<>();
    }

    public NhatKyModel getNhatKy() { return nhatKy; }
    public void setNhatKy(NhatKyModel nhatKy) { this.nhatKy = nhatKy; }

    public List<ChiTietTuoiPhanModel> getDanhSachTuoiPhan() { return danhSachTuoiPhan; }
    public void setDanhSachTuoiPhan(List<ChiTietTuoiPhanModel> list) { this.danhSachTuoiPhan = list; }

    public List<ChiTietPhunThuocModel> getDanhSachPhunThuoc() { return danhSachPhunThuoc; }
    public void setDanhSachPhunThuoc(List<ChiTietPhunThuocModel> list) { this.danhSachPhunThuoc = list; }

    /** Tổng số chi tiết của nhật ký này */
    public int getTotalChiTiet() {
        return danhSachTuoiPhan.size() + danhSachPhunThuoc.size();
    }
}
