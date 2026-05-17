// File: app/src/main/java/com/project/qlcaytrong/data/repository/CayTrongRepository.java
package com.project.qlcaytrong.data.repository;

import androidx.lifecycle.LiveData;

import com.project.qlcaytrong.data.repository.base.BaseRepository;
import com.project.qlcaytrong.model.CayTrongModel;
import com.project.qlcaytrong.util.AuthResult;

import java.util.List;

/** Contract cho CayTrong CRUD + sync operations. */
public interface CayTrongRepository extends BaseRepository<CayTrongModel> {

    /** Lấy cây trồng theo mảnh đất — LiveData từ Room */
    LiveData<List<CayTrongModel>> getAllByManhDatId(String manhDatId);

    /** Lấy 1 cây trồng theo id (blocking, dùng trên IO thread) */
    CayTrongModel getById(String id);
}
