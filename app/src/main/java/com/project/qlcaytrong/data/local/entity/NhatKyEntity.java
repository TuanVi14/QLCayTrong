// File: app/src/main/java/com/project/qlcaytrong/data/local/entity/NhatKyEntity.java
package com.project.qlcaytrong.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

@Entity(
    tableName = NhatKyEntity.TABLE_NAME,
    foreignKeys = {
        @ForeignKey(
            entity = GocCayEntity.class,
            parentColumns = "id",
            childColumns = "goc_cay_id",
            onDelete = ForeignKey.SET_NULL
        ),
        @ForeignKey(
            entity = CayTrongEntity.class,
            parentColumns = "id",
            childColumns = "cay_trong_id",
            onDelete = ForeignKey.SET_NULL
        )
    },
    indices = {
        @Index("goc_cay_id"),
        @Index("cay_trong_id"),
        @Index("user_id")
    }
)
public class NhatKyEntity {

    public static final String TABLE_NAME = "nhat_ky";

    // Loai nhat ky constants
    public static final String LOAI_TUOI_PHAN   = "TUOI_PHAN";
    public static final String LOAI_PHUN_THUOC  = "PHUN_THUOC";
    public static final String LOAI_TINH_HINH   = "TINH_HINH";
    public static final String LOAI_THU_HOACH   = "THU_HOACH";

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    private String id;

    @Nullable
    @ColumnInfo(name = "goc_cay_id")
    private String gocCayId;

    @Nullable
    @ColumnInfo(name = "cay_trong_id")
    private String cayTrongId;

    @ColumnInfo(name = "user_id")
    private String userId;

    @ColumnInfo(name = "loai_nhat_ky")
    private String loaiNhatKy;

    @ColumnInfo(name = "ngay_thuc_hien")
    private long ngayThucHien;

    @ColumnInfo(name = "nguoi_thuc_hien")
    private String nguoiThucHien;

    @ColumnInfo(name = "hinh_anh")
    private String hinhAnh;

    @ColumnInfo(name = "ghi_chu")
    private String ghiChu;

    @ColumnInfo(name = "sync_status")
    private String syncStatus;

    // ==================== Constructor ====================

    public NhatKyEntity(@NonNull String id,
                        @Nullable String gocCayId,
                        @Nullable String cayTrongId,
                        String userId,
                        String loaiNhatKy,
                        long ngayThucHien,
                        String nguoiThucHien,
                        String hinhAnh,
                        String ghiChu,
                        String syncStatus) {
        this.id = id;
        this.gocCayId = gocCayId;
        this.cayTrongId = cayTrongId;
        this.userId = userId;
        this.loaiNhatKy = loaiNhatKy;
        this.ngayThucHien = ngayThucHien;
        this.nguoiThucHien = nguoiThucHien;
        this.hinhAnh = hinhAnh;
        this.ghiChu = ghiChu;
        this.syncStatus = syncStatus;
    }

    // ==================== Getters ====================

    @NonNull
    public String getId() { return id; }

    @Nullable
    public String getGocCayId() { return gocCayId; }

    @Nullable
    public String getCayTrongId() { return cayTrongId; }

    public String getUserId() { return userId; }

    public String getLoaiNhatKy() { return loaiNhatKy; }

    public long getNgayThucHien() { return ngayThucHien; }

    public String getNguoiThucHien() { return nguoiThucHien; }

    public String getHinhAnh() { return hinhAnh; }

    public String getGhiChu() { return ghiChu; }

    public String getSyncStatus() { return syncStatus; }

    // ==================== Setters ====================

    public void setId(@NonNull String id) { this.id = id; }

    public void setGocCayId(@Nullable String gocCayId) { this.gocCayId = gocCayId; }

    public void setCayTrongId(@Nullable String cayTrongId) { this.cayTrongId = cayTrongId; }

    public void setUserId(String userId) { this.userId = userId; }

    public void setLoaiNhatKy(String loaiNhatKy) { this.loaiNhatKy = loaiNhatKy; }

    public void setNgayThucHien(long ngayThucHien) { this.ngayThucHien = ngayThucHien; }

    public void setNguoiThucHien(String nguoiThucHien) { this.nguoiThucHien = nguoiThucHien; }

    public void setHinhAnh(String hinhAnh) { this.hinhAnh = hinhAnh; }

    public void setGhiChu(String ghiChu) { this.ghiChu = ghiChu; }

    public void setSyncStatus(String syncStatus) { this.syncStatus = syncStatus; }
}
