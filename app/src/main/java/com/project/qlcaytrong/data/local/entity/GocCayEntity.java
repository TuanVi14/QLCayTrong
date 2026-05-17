// File: app/src/main/java/com/project/qlcaytrong/data/local/entity/GocCayEntity.java
package com.project.qlcaytrong.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(
    tableName = GocCayEntity.TABLE_NAME,
    foreignKeys = @ForeignKey(
        entity = CayTrongEntity.class,
        parentColumns = "id",
        childColumns = "cay_trong_id",
        onDelete = ForeignKey.CASCADE
    ),
    indices = {@Index("cay_trong_id"), @Index("user_id"), @Index("ma_qr_code")}
)
public class GocCayEntity {

    public static final String TABLE_NAME = "goc_cay";

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    private String id;

    @ColumnInfo(name = "cay_trong_id")
    private String cayTrongId;

    @ColumnInfo(name = "user_id")
    private String userId;

    @ColumnInfo(name = "ma_qr_code")
    private String maQRCode;

    @ColumnInfo(name = "vi_tri")
    private String viTri;

    @ColumnInfo(name = "trang_thai")
    private String trangThai;

    @ColumnInfo(name = "ngay_trong")
    private long ngayTrong;

    @ColumnInfo(name = "ghi_chu")
    private String ghiChu;

    @ColumnInfo(name = "sync_status")
    private String syncStatus;

    // ==================== Constructor ====================

    public GocCayEntity(@NonNull String id,
                        String cayTrongId,
                        String userId,
                        String maQRCode,
                        String viTri,
                        String trangThai,
                        long ngayTrong,
                        String ghiChu,
                        String syncStatus) {
        this.id = id;
        this.cayTrongId = cayTrongId;
        this.userId = userId;
        this.maQRCode = maQRCode;
        this.viTri = viTri;
        this.trangThai = trangThai;
        this.ngayTrong = ngayTrong;
        this.ghiChu = ghiChu;
        this.syncStatus = syncStatus;
    }

    // ==================== Getters ====================

    @NonNull
    public String getId() { return id; }

    public String getCayTrongId() { return cayTrongId; }

    public String getUserId() { return userId; }

    public String getMaQRCode() { return maQRCode; }

    public String getViTri() { return viTri; }

    public String getTrangThai() { return trangThai; }

    public long getNgayTrong() { return ngayTrong; }

    public String getGhiChu() { return ghiChu; }

    public String getSyncStatus() { return syncStatus; }

    // ==================== Setters ====================

    public void setId(@NonNull String id) { this.id = id; }

    public void setCayTrongId(String cayTrongId) { this.cayTrongId = cayTrongId; }

    public void setUserId(String userId) { this.userId = userId; }

    public void setMaQRCode(String maQRCode) { this.maQRCode = maQRCode; }

    public void setViTri(String viTri) { this.viTri = viTri; }

    public void setTrangThai(String trangThai) { this.trangThai = trangThai; }

    public void setNgayTrong(long ngayTrong) { this.ngayTrong = ngayTrong; }

    public void setGhiChu(String ghiChu) { this.ghiChu = ghiChu; }

    public void setSyncStatus(String syncStatus) { this.syncStatus = syncStatus; }
}
