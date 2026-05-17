// File: app/src/main/java/com/project/qlcaytrong/data/local/dao/ChiTietPhunThuocDao.java
package com.project.qlcaytrong.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.project.qlcaytrong.data.local.entity.ChiTietPhunThuocEntity;

import java.util.List;

@Dao
public interface ChiTietPhunThuocDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ChiTietPhunThuocEntity entity);

    @Update
    void update(ChiTietPhunThuocEntity entity);

    @Delete
    void delete(ChiTietPhunThuocEntity entity);

    @Query("SELECT * FROM chi_tiet_phun_thuoc WHERE id = :id LIMIT 1")
    ChiTietPhunThuocEntity getById(String id);

    @Query("SELECT * FROM chi_tiet_phun_thuoc ORDER BY rowid DESC")
    LiveData<List<ChiTietPhunThuocEntity>> getAll();

    @Query("SELECT * FROM chi_tiet_phun_thuoc WHERE user_id = :userId ORDER BY rowid DESC")
    LiveData<List<ChiTietPhunThuocEntity>> getAllByUserId(String userId);

    @Query("SELECT * FROM chi_tiet_phun_thuoc WHERE nhat_ky_id = :nhatKyId")
    LiveData<List<ChiTietPhunThuocEntity>> getAllByNhatKyId(String nhatKyId);

    @Query("SELECT * FROM chi_tiet_phun_thuoc WHERE sync_status = 'PENDING'")
    List<ChiTietPhunThuocEntity> getAllPendingSync();

    @Query("DELETE FROM chi_tiet_phun_thuoc")
    void deleteAll();
}
