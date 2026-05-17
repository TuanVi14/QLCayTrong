// File: app/src/main/java/com/project/qlcaytrong/model/NhatKyModel.java
package com.project.qlcaytrong.model;

public class NhatKyModel {
    private String id, gocCayId, cayTrongId, userId;
    private String loaiNhatKy, nguoiThucHien, hinhAnh, ghiChu, syncStatus;
    private long ngayThucHien;

    public NhatKyModel() {}

    public NhatKyModel(String id, String gocCayId, String cayTrongId, String userId,
                        String loaiNhatKy, long ngayThucHien, String nguoiThucHien,
                        String hinhAnh, String ghiChu, String syncStatus) {
        this.id = id; this.gocCayId = gocCayId; this.cayTrongId = cayTrongId;
        this.userId = userId; this.loaiNhatKy = loaiNhatKy;
        this.ngayThucHien = ngayThucHien; this.nguoiThucHien = nguoiThucHien;
        this.hinhAnh = hinhAnh; this.ghiChu = ghiChu; this.syncStatus = syncStatus;
    }

    public String getId() { return id; }
    public String getGocCayId() { return gocCayId; }
    public String getCayTrongId() { return cayTrongId; }
    public String getUserId() { return userId; }
    public String getLoaiNhatKy() { return loaiNhatKy; }
    public long getNgayThucHien() { return ngayThucHien; }
    public String getNguoiThucHien() { return nguoiThucHien; }
    public String getHinhAnh() { return hinhAnh; }
    public String getGhiChu() { return ghiChu; }
    public String getSyncStatus() { return syncStatus; }

    public void setId(String id) { this.id = id; }
    public void setGocCayId(String gocCayId) { this.gocCayId = gocCayId; }
    public void setCayTrongId(String cayTrongId) { this.cayTrongId = cayTrongId; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setLoaiNhatKy(String loaiNhatKy) { this.loaiNhatKy = loaiNhatKy; }
    public void setNgayThucHien(long ngayThucHien) { this.ngayThucHien = ngayThucHien; }
    public void setNguoiThucHien(String nguoiThucHien) { this.nguoiThucHien = nguoiThucHien; }
    public void setHinhAnh(String hinhAnh) { this.hinhAnh = hinhAnh; }
    public void setGhiChu(String ghiChu) { this.ghiChu = ghiChu; }
    public void setSyncStatus(String syncStatus) { this.syncStatus = syncStatus; }
}
