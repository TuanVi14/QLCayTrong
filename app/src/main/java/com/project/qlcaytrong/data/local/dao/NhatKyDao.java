// File: app/src/main/java/com/project/qlcaytrong/data/local/dao/NhatKyDao.java
package com.project.qlcaytrong.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.project.qlcaytrong.data.local.entity.NhatKyEntity;

import java.util.List;

@Dao
public interface NhatKyDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(NhatKyEntity entity);

    @Update
    void update(NhatKyEntity entity);

    @Delete
    void delete(NhatKyEntity entity);

    @Query("SELECT * FROM nhat_ky WHERE id = :id LIMIT 1")
    NhatKyEntity getById(String id);

    @Query("SELECT * FROM nhat_ky ORDER BY ngay_thuc_hien DESC")
    LiveData<List<NhatKyEntity>> getAll();

    @Query("SELECT * FROM nhat_ky WHERE user_id = :userId ORDER BY ngay_thuc_hien DESC")
    LiveData<List<NhatKyEntity>> getAllByUserId(String userId);

    @Query("SELECT * FROM nhat_ky WHERE goc_cay_id = :gocCayId ORDER BY ngay_thuc_hien DESC")
    LiveData<List<NhatKyEntity>> getAllByGocCayId(String gocCayId);

    @Query("SELECT * FROM nhat_ky WHERE cay_trong_id = :cayTrongId ORDER BY ngay_thuc_hien DESC")
    LiveData<List<NhatKyEntity>> getAllByCayTrongId(String cayTrongId);

    @Query("SELECT * FROM nhat_ky WHERE loai_nhat_ky = :loaiNhatKy AND user_id = :userId ORDER BY ngay_thuc_hien DESC")
    LiveData<List<NhatKyEntity>> getAllByLoai(String loaiNhatKy, String userId);

    @Query("SELECT * FROM nhat_ky WHERE sync_status = 'PENDING'")
    List<NhatKyEntity> getAllPendingSync();

    @Query("DELETE FROM nhat_ky")
    void deleteAll();
}
