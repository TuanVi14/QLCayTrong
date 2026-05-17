// File: app/src/main/java/com/project/qlcaytrong/data/local/dao/ChiTietTuoiPhanDao.java
package com.project.qlcaytrong.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.project.qlcaytrong.data.local.entity.ChiTietTuoiPhanEntity;

import java.util.List;

@Dao
public interface ChiTietTuoiPhanDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ChiTietTuoiPhanEntity entity);

    @Update
    void update(ChiTietTuoiPhanEntity entity);

    @Delete
    void delete(ChiTietTuoiPhanEntity entity);

    @Query("SELECT * FROM chi_tiet_tuoi_phan WHERE id = :id LIMIT 1")
    ChiTietTuoiPhanEntity getById(String id);

    @Query("SELECT * FROM chi_tiet_tuoi_phan ORDER BY rowid DESC")
    LiveData<List<ChiTietTuoiPhanEntity>> getAll();

    @Query("SELECT * FROM chi_tiet_tuoi_phan WHERE user_id = :userId ORDER BY rowid DESC")
    LiveData<List<ChiTietTuoiPhanEntity>> getAllByUserId(String userId);

    @Query("SELECT * FROM chi_tiet_tuoi_phan WHERE nhat_ky_id = :nhatKyId")
    LiveData<List<ChiTietTuoiPhanEntity>> getAllByNhatKyId(String nhatKyId);

    @Query("SELECT * FROM chi_tiet_tuoi_phan WHERE nhat_ky_id = :nhatKyId")
    List<ChiTietTuoiPhanEntity> getAllByNhatKyIdSync(String nhatKyId);

    @Query("SELECT * FROM chi_tiet_tuoi_phan WHERE sync_status = 'PENDING'")
    List<ChiTietTuoiPhanEntity> getAllPendingSync();

    @Query("DELETE FROM chi_tiet_tuoi_phan WHERE nhat_ky_id = :nhatKyId")
    void deleteByNhatKyId(String nhatKyId);

    @Query("DELETE FROM chi_tiet_tuoi_phan")
    void deleteAll();
}
