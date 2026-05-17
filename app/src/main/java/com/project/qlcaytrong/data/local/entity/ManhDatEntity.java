// File: app/src/main/java/com/project/qlcaytrong/data/local/entity/ManhDatEntity.java
package com.project.qlcaytrong.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(
    tableName = ManhDatEntity.TABLE_NAME,
    foreignKeys = @ForeignKey(
        entity = NguoiDungEntity.class,
        parentColumns = "id",
        childColumns = "user_id",
        onDelete = ForeignKey.CASCADE
    ),
    indices = {@Index("user_id")}
)
public class ManhDatEntity {

    public static final String TABLE_NAME = "manh_dat";

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    private String id;

    @ColumnInfo(name = "user_id")
    private String userId;

    @ColumnInfo(name = "ten_manh_dat")
    private String tenManhDat;

    @ColumnInfo(name = "dia_chi")
    private String diaChi;

    @ColumnInfo(name = "dien_tich")
    private double dienTich;

    @ColumnInfo(name = "don_vi_dien_tich")
    private String donViDienTich;

    @ColumnInfo(name = "mo_ta")
    private String moTa;

    @ColumnInfo(name = "ngay_tao")
    private long ngayTao;

    @ColumnInfo(name = "sync_status")
    private String syncStatus;

    // ==================== Constructor ====================

    public ManhDatEntity(@NonNull String id,
                         String userId,
                         String tenManhDat,
                         String diaChi,
                         double dienTich,
                         String donViDienTich,
                         String moTa,
                         long ngayTao,
                         String syncStatus) {
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

    @NonNull
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

    public void setId(@NonNull String id) { this.id = id; }

    public void setUserId(String userId) { this.userId = userId; }

    public void setTenManhDat(String tenManhDat) { this.tenManhDat = tenManhDat; }

    public void setDiaChi(String diaChi) { this.diaChi = diaChi; }

    public void setDienTich(double dienTich) { this.dienTich = dienTich; }

    public void setDonViDienTich(String donViDienTich) { this.donViDienTich = donViDienTich; }

    public void setMoTa(String moTa) { this.moTa = moTa; }

    public void setNgayTao(long ngayTao) { this.ngayTao = ngayTao; }

    public void setSyncStatus(String syncStatus) { this.syncStatus = syncStatus; }
}
