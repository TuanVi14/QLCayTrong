// File: app/src/main/java/com/project/qlcaytrong/data/local/dao/ManhDatDao.java
package com.project.qlcaytrong.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.project.qlcaytrong.data.local.entity.ManhDatEntity;

import java.util.List;

@Dao
public interface ManhDatDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ManhDatEntity entity);

    @Update
    void update(ManhDatEntity entity);

    @Delete
    void delete(ManhDatEntity entity);

    @Query("SELECT * FROM manh_dat WHERE id = :id LIMIT 1")
    ManhDatEntity getById(String id);

    @Query("SELECT * FROM manh_dat ORDER BY ngay_tao DESC")
    LiveData<List<ManhDatEntity>> getAll();

    @Query("SELECT * FROM manh_dat WHERE user_id = :userId ORDER BY ngay_tao DESC")
    LiveData<List<ManhDatEntity>> getAllByUserId(String userId);

    @Query("SELECT * FROM manh_dat WHERE sync_status = 'PENDING'")
    List<ManhDatEntity> getAllPendingSync();

    @Query("DELETE FROM manh_dat")
    void deleteAll();
}
