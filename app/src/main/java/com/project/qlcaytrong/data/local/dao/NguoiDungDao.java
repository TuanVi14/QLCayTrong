// File: app/src/main/java/com/project/qlcaytrong/data/local/dao/NguoiDungDao.java
package com.project.qlcaytrong.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.project.qlcaytrong.data.local.entity.NguoiDungEntity;

import java.util.List;

@Dao
public interface NguoiDungDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(NguoiDungEntity entity);

    @Update
    void update(NguoiDungEntity entity);

    @Delete
    void delete(NguoiDungEntity entity);

    @Query("SELECT * FROM nguoi_dung WHERE id = :id LIMIT 1")
    NguoiDungEntity getById(String id);

    @Query("SELECT * FROM nguoi_dung ORDER BY ngay_tao DESC")
    LiveData<List<NguoiDungEntity>> getAll();

    @Query("SELECT * FROM nguoi_dung WHERE id = :userId ORDER BY ngay_tao DESC")
    LiveData<List<NguoiDungEntity>> getAllByUserId(String userId);

    @Query("SELECT * FROM nguoi_dung WHERE sync_status = 'PENDING'")
    List<NguoiDungEntity> getAllPendingSync();

    @Query("DELETE FROM nguoi_dung")
    void deleteAll();
}
