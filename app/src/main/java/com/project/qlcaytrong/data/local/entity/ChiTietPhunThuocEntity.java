// File: app/src/main/java/com/project/qlcaytrong/data/local/entity/ChiTietPhunThuocEntity.java
package com.project.qlcaytrong.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(
    tableName = ChiTietPhunThuocEntity.TABLE_NAME,
    foreignKeys = @ForeignKey(
        entity = NhatKyEntity.class,
        parentColumns = "id",
        childColumns = "nhat_ky_id",
        onDelete = ForeignKey.CASCADE
    ),
    indices = {@Index("nhat_ky_id"), @Index("user_id")}
)
public class ChiTietPhunThuocEntity {

    public static final String TABLE_NAME = "chi_tiet_phun_thuoc";

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    private String id;

    @ColumnInfo(name = "nhat_ky_id")
    private String nhatKyId;

    @ColumnInfo(name = "user_id")
    private String userId;

    @ColumnInfo(name = "ten_thuoc")
    private String tenThuoc;

    @ColumnInfo(name = "lieu_luong")
    private double lieuLuong;

    @ColumnInfo(name = "don_vi")
    private String donVi;

    @ColumnInfo(name = "ly_do_phun")
    private String lyDoPhun;

    @ColumnInfo(name = "sync_status")
    private String syncStatus;

    public ChiTietPhunThuocEntity(@NonNull String id, String nhatKyId, String userId,
                                  String tenThuoc, double lieuLuong, String donVi,
                                  String lyDoPhun, String syncStatus) {
        this.id = id; this.nhatKyId = nhatKyId; this.userId = userId;
        this.tenThuoc = tenThuoc; this.lieuLuong = lieuLuong; this.donVi = donVi;
        this.lyDoPhun = lyDoPhun; this.syncStatus = syncStatus;
    }

    @NonNull public String getId() { return id; }
    public String getNhatKyId() { return nhatKyId; }
    public String getUserId() { return userId; }
    public String getTenThuoc() { return tenThuoc; }
    public double getLieuLuong() { return lieuLuong; }
    public String getDonVi() { return donVi; }
    public String getLyDoPhun() { return lyDoPhun; }
    public String getSyncStatus() { return syncStatus; }

    public void setId(@NonNull String id) { this.id = id; }
    public void setNhatKyId(String nhatKyId) { this.nhatKyId = nhatKyId; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setTenThuoc(String tenThuoc) { this.tenThuoc = tenThuoc; }
    public void setLieuLuong(double lieuLuong) { this.lieuLuong = lieuLuong; }
    public void setDonVi(String donVi) { this.donVi = donVi; }
    public void setLyDoPhun(String lyDoPhun) { this.lyDoPhun = lyDoPhun; }
    public void setSyncStatus(String syncStatus) { this.syncStatus = syncStatus; }
}
