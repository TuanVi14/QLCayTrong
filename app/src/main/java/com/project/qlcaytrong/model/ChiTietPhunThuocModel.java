// File: app/src/main/java/com/project/qlcaytrong/model/ChiTietPhunThuocModel.java
package com.project.qlcaytrong.model;

public class ChiTietPhunThuocModel {
    private String id, nhatKyId, userId, tenThuoc, donVi, lyDoPhun, syncStatus;
    private double lieuLuong;

    public ChiTietPhunThuocModel() {}

    public ChiTietPhunThuocModel(String id, String nhatKyId, String userId,
                                  String tenThuoc, double lieuLuong, String donVi,
                                  String lyDoPhun, String syncStatus) {
        this.id = id; this.nhatKyId = nhatKyId; this.userId = userId;
        this.tenThuoc = tenThuoc; this.lieuLuong = lieuLuong;
        this.donVi = donVi; this.lyDoPhun = lyDoPhun; this.syncStatus = syncStatus;
    }

    public String getId() { return id; }
    public String getNhatKyId() { return nhatKyId; }
    public String getUserId() { return userId; }
    public String getTenThuoc() { return tenThuoc; }
    public double getLieuLuong() { return lieuLuong; }
    public String getDonVi() { return donVi; }
    public String getLyDoPhun() { return lyDoPhun; }
    public String getSyncStatus() { return syncStatus; }

    public void setId(String id) { this.id = id; }
    public void setNhatKyId(String nhatKyId) { this.nhatKyId = nhatKyId; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setTenThuoc(String tenThuoc) { this.tenThuoc = tenThuoc; }
    public void setLieuLuong(double lieuLuong) { this.lieuLuong = lieuLuong; }
    public void setDonVi(String donVi) { this.donVi = donVi; }
    public void setLyDoPhun(String lyDoPhun) { this.lyDoPhun = lyDoPhun; }
    public void setSyncStatus(String syncStatus) { this.syncStatus = syncStatus; }
}
