// File: app/src/main/java/com/project/qlcaytrong/model/GocCayModel.java
package com.project.qlcaytrong.model;

public class GocCayModel {
    private String id;
    private String cayTrongId;
    private String userId;
    private String maQRCode;
    private String viTri;
    private String trangThai;
    private long ngayTrong;
    private String ghiChu;
    private String syncStatus;

    public GocCayModel() {}

    public GocCayModel(String id, String cayTrongId, String userId, String maQRCode,
                       String viTri, String trangThai, long ngayTrong,
                       String ghiChu, String syncStatus) {
        this.id = id; this.cayTrongId = cayTrongId; this.userId = userId;
        this.maQRCode = maQRCode; this.viTri = viTri; this.trangThai = trangThai;
        this.ngayTrong = ngayTrong; this.ghiChu = ghiChu; this.syncStatus = syncStatus;
    }

    public String getId() { return id; }
    public String getCayTrongId() { return cayTrongId; }
    public String getUserId() { return userId; }
    public String getMaQRCode() { return maQRCode; }
    public String getViTri() { return viTri; }
    public String getTrangThai() { return trangThai; }
    public long getNgayTrong() { return ngayTrong; }
    public String getGhiChu() { return ghiChu; }
    public String getSyncStatus() { return syncStatus; }

    public void setId(String id) { this.id = id; }
    public void setCayTrongId(String cayTrongId) { this.cayTrongId = cayTrongId; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setMaQRCode(String maQRCode) { this.maQRCode = maQRCode; }
    public void setViTri(String viTri) { this.viTri = viTri; }
    public void setTrangThai(String trangThai) { this.trangThai = trangThai; }
    public void setNgayTrong(long ngayTrong) { this.ngayTrong = ngayTrong; }
    public void setGhiChu(String ghiChu) { this.ghiChu = ghiChu; }
    public void setSyncStatus(String syncStatus) { this.syncStatus = syncStatus; }

    public boolean isPending() { return "PENDING".equals(syncStatus); }
}
