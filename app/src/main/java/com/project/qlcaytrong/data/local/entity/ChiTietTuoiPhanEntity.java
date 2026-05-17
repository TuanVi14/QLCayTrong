// File: app/src/main/java/com/project/qlcaytrong/data/local/entity/ChiTietTuoiPhanEntity.java
package com.project.qlcaytrong.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(
    tableName = ChiTietTuoiPhanEntity.TABLE_NAME,
    foreignKeys = @ForeignKey(
        entity = NhatKyEntity.class,
        parentColumns = "id",
        childColumns = "nhat_ky_id",
        onDelete = ForeignKey.CASCADE
    ),
    indices = {@Index("nhat_ky_id"), @Index("user_id")}
)
public class ChiTietTuoiPhanEntity {

    public static final String TABLE_NAME = "chi_tiet_tuoi_phan";

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    private String id;

    @ColumnInfo(name = "nhat_ky_id")
    private String nhatKyId;

    @ColumnInfo(name = "user_id")
    private String userId;

    @ColumnInfo(name = "ten_phan")
    private String tenPhan;

    @ColumnInfo(name = "lieu_luong")
    private double lieuLuong;

    @ColumnInfo(name = "don_vi")
    private String donVi;

    @ColumnInfo(name = "cach_bon")
    private String cachBon;

    @ColumnInfo(name = "sync_status")
    private String syncStatus;

    public ChiTietTuoiPhanEntity() {}

    public ChiTietTuoiPhanEntity(@NonNull String id, String nhatKyId, String userId,
                                 String tenPhan, double lieuLuong, String donVi,
                                 String cachBon, String syncStatus) {
        this.id = id; this.nhatKyId = nhatKyId; this.userId = userId;
        this.tenPhan = tenPhan; this.lieuLuong = lieuLuong; this.donVi = donVi;
        this.cachBon = cachBon; this.syncStatus = syncStatus;
    }

    @NonNull public String getId() { return id; }
    public String getNhatKyId() { return nhatKyId; }
    public String getUserId() { return userId; }
    public String getTenPhan() { return tenPhan; }
    public double getLieuLuong() { return lieuLuong; }
    public String getDonVi() { return donVi; }
    public String getCachBon() { return cachBon; }
    public String getSyncStatus() { return syncStatus; }

    public void setId(@NonNull String id) { this.id = id; }
    public void setNhatKyId(String nhatKyId) { this.nhatKyId = nhatKyId; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setTenPhan(String tenPhan) { this.tenPhan = tenPhan; }
    public void setLieuLuong(double lieuLuong) { this.lieuLuong = lieuLuong; }
    public void setDonVi(String donVi) { this.donVi = donVi; }
    public void setCachBon(String cachBon) { this.cachBon = cachBon; }
    public void setSyncStatus(String syncStatus) { this.syncStatus = syncStatus; }
}
