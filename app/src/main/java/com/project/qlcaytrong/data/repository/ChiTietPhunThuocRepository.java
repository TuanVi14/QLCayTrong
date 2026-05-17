// File: app/src/main/java/com/project/qlcaytrong/data/repository/ChiTietPhunThuocRepository.java
package com.project.qlcaytrong.data.repository;

import androidx.lifecycle.LiveData;

import com.project.qlcaytrong.data.repository.base.BaseRepository;
import com.project.qlcaytrong.model.ChiTietPhunThuocModel;

import java.util.List;

/** Contract cho ChiTietPhunThuoc CRUD + sync operations. */
public interface ChiTietPhunThuocRepository extends BaseRepository<ChiTietPhunThuocModel> {

    /** Lấy chi tiết phun thuốc theo nhật ký */
    LiveData<List<ChiTietPhunThuocModel>> getAllByNhatKyId(String nhatKyId);
}
