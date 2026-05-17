// File: app/src/main/java/com/project/qlcaytrong/data/local/dao/GocCayDao.java
package com.project.qlcaytrong.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.project.qlcaytrong.data.local.entity.GocCayEntity;

import java.util.List;

@Dao
public interface GocCayDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(GocCayEntity entity);

    @Update
    void update(GocCayEntity entity);

    @Delete
    void delete(GocCayEntity entity);

    @Query("SELECT * FROM goc_cay WHERE id = :id LIMIT 1")
    GocCayEntity getById(String id);

    @Query("SELECT * FROM goc_cay ORDER BY ngay_trong DESC")
    LiveData<List<GocCayEntity>> getAll();

    @Query("SELECT * FROM goc_cay WHERE user_id = :userId ORDER BY ngay_trong DESC")
    LiveData<List<GocCayEntity>> getAllByUserId(String userId);

    @Query("SELECT * FROM goc_cay WHERE cay_trong_id = :cayTrongId ORDER BY ngay_trong DESC")
    LiveData<List<GocCayEntity>> getAllByCayTrongId(String cayTrongId);

    @Query("SELECT * FROM goc_cay WHERE ma_qr_code = :maQRCode LIMIT 1")
    GocCayEntity getByQRCode(String maQRCode);

    @Query("SELECT * FROM goc_cay WHERE sync_status = 'PENDING'")
    List<GocCayEntity> getAllPendingSync();

    @Query("DELETE FROM goc_cay")
    void deleteAll();
}
