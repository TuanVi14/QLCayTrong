// File: app/src/main/java/com/project/qlcaytrong/data/local/entity/CayTrongEntity.java
package com.project.qlcaytrong.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(
    tableName = CayTrongEntity.TABLE_NAME,
    foreignKeys = @ForeignKey(
        entity = ManhDatEntity.class,
        parentColumns = "id",
        childColumns = "manh_dat_id",
        onDelete = ForeignKey.CASCADE
    ),
    indices = {@Index("manh_dat_id"), @Index("user_id")}
)
public class CayTrongEntity {

    public static final String TABLE_NAME = "cay_trong";

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    private String id;

    @ColumnInfo(name = "manh_dat_id")
    private String manhDatId;

    @ColumnInfo(name = "user_id")
    private String userId;

    @ColumnInfo(name = "loai_cay")
    private String loaiCay;

    @ColumnInfo(name = "ten_khoa_hoc")
    private String tenKhoaHoc;

    @ColumnInfo(name = "so_luong")
    private int soLuong;

    @ColumnInfo(name = "don_vi_tinh")
    private String donViTinh;

    @ColumnInfo(name = "ngay_trong")
    private long ngayTrong;

    @ColumnInfo(name = "trang_thai")
    private String trangThai;

    @ColumnInfo(name = "mo_ta")
    private String moTa;

    @ColumnInfo(name = "sync_status")
    private String syncStatus;

    // ==================== Constructor ====================

    public CayTrongEntity() {}


    public CayTrongEntity(@NonNull String id,
                          String manhDatId,
                          String userId,
                          String loaiCay,
                          String tenKhoaHoc,
                          int soLuong,
                          String donViTinh,
                          long ngayTrong,
                          String trangThai,
                          String moTa,
                          String syncStatus) {
        this.id = id;
        this.manhDatId = manhDatId;
        this.userId = userId;
        this.loaiCay = loaiCay;
        this.tenKhoaHoc = tenKhoaHoc;
        this.soLuong = soLuong;
        this.donViTinh = donViTinh;
        this.ngayTrong = ngayTrong;
        this.trangThai = trangThai;
        this.moTa = moTa;
        this.syncStatus = syncStatus;
    }

    // ==================== Getters ====================

    @NonNull
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

    // ==================== Setters ====================

    public void setId(@NonNull String id) { this.id = id; }

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
}
