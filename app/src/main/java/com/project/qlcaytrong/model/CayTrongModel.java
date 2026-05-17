// File: app/src/main/java/com/project/qlcaytrong/model/CayTrongModel.java
package com.project.qlcaytrong.model;

public class CayTrongModel {
    private String id;
    private String manhDatId;
    private String userId;
    private String loaiCay;
    private String tenKhoaHoc;
    private int soLuong;
    private String donViTinh;
    private long ngayTrong;
    private String trangThai;
    private String moTa;
    private String syncStatus;

    public CayTrongModel() {}

    public CayTrongModel(String id, String manhDatId, String userId, String loaiCay,
                         String tenKhoaHoc, int soLuong, String donViTinh,
                         long ngayTrong, String trangThai, String moTa, String syncStatus) {
        this.id = id; this.manhDatId = manhDatId; this.userId = userId;
        this.loaiCay = loaiCay; this.tenKhoaHoc = tenKhoaHoc; this.soLuong = soLuong;
        this.donViTinh = donViTinh; this.ngayTrong = ngayTrong; this.trangThai = trangThai;
        this.moTa = moTa; this.syncStatus = syncStatus;
    }

    public String getId() { return id; }
    public String getManhDatId() { return manhDatId; }
    public String getUserId() { return userId; }
    public String getLoaiCay() { return loaiCay; }
    public String getTenKhoaHoc() { return tenKhoaHoc; }
    public int getSoLuong() { return soLuong; }
    public String getDonViTinh() { return donViTinh; }
    public long getNgayTrong() { return ngayTrong; }
    public String getTrangThai() { return trangThai; }
    public String getMoTa() { return moTa; }
    public String getSyncStatus() { return syncStatus; }

    public void setId(String id) { this.id = id; }
    public void setManhDatId(String manhDatId) { this.manhDatId = manhDatId; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setLoaiCay(String loaiCay) { this.loaiCay = loaiCay; }
    public void setTenKhoaHoc(String tenKhoaHoc) { this.tenKhoaHoc = tenKhoaHoc; }
    public void setSoLuong(int soLuong) { this.soLuong = soLuong; }
    public void setDonViTinh(String donViTinh) { this.donViTinh = donViTinh; }
    public void setNgayTrong(long ngayTrong) { this.ngayTrong = ngayTrong; }
    public void setTrangThai(String trangThai) { this.trangThai = trangThai; }
    public void setMoTa(String moTa) { this.moTa = moTa; }
    public void setSyncStatus(String syncStatus) { this.syncStatus = syncStatus; }

    public boolean isPending() { return "PENDING".equals(syncStatus); }
}
