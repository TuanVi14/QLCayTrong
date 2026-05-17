// File: app/src/main/java/com/project/qlcaytrong/model/ChiTietTuoiPhanModel.java
package com.project.qlcaytrong.model;

public class ChiTietTuoiPhanModel {
    private String id, nhatKyId, userId, tenPhan, donVi, cachBon, syncStatus;
    private double lieuLuong;

    public ChiTietTuoiPhanModel() {}

    public ChiTietTuoiPhanModel(String id, String nhatKyId, String userId,
                                 String tenPhan, double lieuLuong, String donVi,
                                 String cachBon, String syncStatus) {
        this.id = id; this.nhatKyId = nhatKyId; this.userId = userId;
        this.tenPhan = tenPhan; this.lieuLuong = lieuLuong;
        this.donVi = donVi; this.cachBon = cachBon; this.syncStatus = syncStatus;
    }

    public String getId() { return id; }
    public String getNhatKyId() { return nhatKyId; }
    public String getUserId() { return userId; }
    public String getTenPhan() { return tenPhan; }
    public double getLieuLuong() { return lieuLuong; }
    public String getDonVi() { return donVi; }
    public String getCachBon() { return cachBon; }
    public String getSyncStatus() { return syncStatus; }

    public void setId(String id) { this.id = id; }
    public void setNhatKyId(String nhatKyId) { this.nhatKyId = nhatKyId; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setTenPhan(String tenPhan) { this.tenPhan = tenPhan; }
    public void setLieuLuong(double lieuLuong) { this.lieuLuong = lieuLuong; }
    public void setDonVi(String donVi) { this.donVi = donVi; }
    public void setCachBon(String cachBon) { this.cachBon = cachBon; }
    public void setSyncStatus(String syncStatus) { this.syncStatus = syncStatus; }
}
