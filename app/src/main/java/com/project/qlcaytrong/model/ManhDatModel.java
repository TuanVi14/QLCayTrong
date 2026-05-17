// File: app/src/main/java/com/project/qlcaytrong/model/ManhDatModel.java
package com.project.qlcaytrong.model;

/**
 * POJO thuần - không phụ thuộc Room hay Firestore.
 * Dùng để truyền data giữa ViewModel ↔ UI.
 * Tách biệt khỏi ManhDatEntity để UI không biết Room implementation.
 */
public class ManhDatModel {

    private String id;
    private String userId;
    private String tenManhDat;
    private String diaChi;
    private double dienTich;
    private String donViDienTich;
    private String moTa;
    private long ngayTao;
    private String syncStatus;

    // ==================== Constructor rỗng (Firestore cần) ====================
    public ManhDatModel() {}

    // ==================== Constructor đầy đủ ====================
    public ManhDatModel(String id, String userId, String tenManhDat,
                        String diaChi, double dienTich, String donViDienTich,
                        String moTa, long ngayTao, String syncStatus) {
        this.id = id;
        this.userId = userId;
        this.tenManhDat = tenManhDat;
        this.diaChi = diaChi;
        this.dienTich = dienTich;
        this.donViDienTich = donViDienTich;
        this.moTa = moTa;
        this.ngayTao = ngayTao;
        this.syncStatus = syncStatus;
    }

    // ==================== Getters ====================
    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getTenManhDat() { return tenManhDat; }
    public String getDiaChi() { return diaChi; }
    public double getDienTich() { return dienTich; }
    public String getDonViDienTich() { return donViDienTich; }
    public String getMoTa() { return moTa; }
    public long getNgayTao() { return ngayTao; }
    public String getSyncStatus() { return syncStatus; }

    // ==================== Setters ====================
    public void setId(String id) { this.id = id; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setTenManhDat(String tenManhDat) { this.tenManhDat = tenManhDat; }
    public void setDiaChi(String diaChi) { this.diaChi = diaChi; }
    public void setDienTich(double dienTich) { this.dienTich = dienTich; }
    public void setDonViDienTich(String donViDienTich) { this.donViDienTich = donViDienTich; }
    public void setMoTa(String moTa) { this.moTa = moTa; }
    public void setNgayTao(long ngayTao) { this.ngayTao = ngayTao; }
    public void setSyncStatus(String syncStatus) { this.syncStatus = syncStatus; }

    // ==================== Helper ====================

    /** Hiển thị diện tích kèm đơn vị, VD: "1.5 ha" */
    public String getDienTichFormatted() {
        if (dienTich == (long) dienTich) {
            return (long) dienTich + " " + donViDienTich;
        }
        return dienTich + " " + donViDienTich;
    }

    /** True nếu chưa được sync lên cloud */
    public boolean isPending() {
        return "PENDING".equals(syncStatus);
    }
}
