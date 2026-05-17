// File: app/src/main/java/com/project/qlcaytrong/data/local/entity/NguoiDungEntity.java
package com.project.qlcaytrong.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(tableName = NguoiDungEntity.TABLE_NAME)
public class NguoiDungEntity {

    public static final String TABLE_NAME = "nguoi_dung";

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    private String id;

    @ColumnInfo(name = "ho_ten")
    private String hoTen;

    @ColumnInfo(name = "email")
    private String email;

    @ColumnInfo(name = "so_dien_thoai")
    private String soDienThoai;

    @ColumnInfo(name = "ngay_tao")
    private long ngayTao;

    @ColumnInfo(name = "sync_status")
    private String syncStatus;

    // ==================== Constructor ====================

    public NguoiDungEntity(@NonNull String id,
                           String hoTen,
                           String email,
                           String soDienThoai,
                           long ngayTao,
                           String syncStatus) {
        this.id = id;
        this.hoTen = hoTen;
        this.email = email;
        this.soDienThoai = soDienThoai;
        this.ngayTao = ngayTao;
        this.syncStatus = syncStatus;
    }

    // ==================== Getters ====================

    @NonNull
    public String getId() { return id; }

    public String getHoTen() { return hoTen; }

    public String getEmail() { return email; }

    public String getSoDienThoai() { return soDienThoai; }

    public long getNgayTao() { return ngayTao; }

    public String getSyncStatus() { return syncStatus; }

    // ==================== Setters ====================

    public void setId(@NonNull String id) { this.id = id; }

    public void setHoTen(String hoTen) { this.hoTen = hoTen; }

    public void setEmail(String email) { this.email = email; }

    public void setSoDienThoai(String soDienThoai) { this.soDienThoai = soDienThoai; }

    public void setNgayTao(long ngayTao) { this.ngayTao = ngayTao; }

    public void setSyncStatus(String syncStatus) { this.syncStatus = syncStatus; }
}
