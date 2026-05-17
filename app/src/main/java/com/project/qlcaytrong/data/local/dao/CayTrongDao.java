// File: app/src/main/java/com/project/qlcaytrong/data/local/dao/CayTrongDao.java
package com.project.qlcaytrong.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.project.qlcaytrong.data.local.entity.CayTrongEntity;

import java.util.List;

@Dao
public interface CayTrongDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(CayTrongEntity entity);

    @Update
    void update(CayTrongEntity entity);

    @Delete
    void delete(CayTrongEntity entity);

    @Query("SELECT * FROM cay_trong WHERE id = :id LIMIT 1")
    CayTrongEntity getById(String id);

    @Query("SELECT * FROM cay_trong ORDER BY ngay_trong DESC")
    LiveData<List<CayTrongEntity>> getAll();

    @Query("SELECT * FROM cay_trong WHERE user_id = :userId ORDER BY ngay_trong DESC")
    LiveData<List<CayTrongEntity>> getAllByUserId(String userId);

    @Query("SELECT * FROM cay_trong WHERE manh_dat_id = :manhDatId ORDER BY ngay_trong DESC")
    LiveData<List<CayTrongEntity>> getAllByManhDatId(String manhDatId);

    @Query("SELECT * FROM cay_trong WHERE sync_status = 'PENDING'")
    List<CayTrongEntity> getAllPendingSync();

    @Query("DELETE FROM cay_trong")
    void deleteAll();
}
