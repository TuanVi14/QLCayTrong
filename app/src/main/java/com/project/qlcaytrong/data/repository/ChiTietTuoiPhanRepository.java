// File: app/src/main/java/com/project/qlcaytrong/data/repository/ChiTietTuoiPhanRepository.java
package com.project.qlcaytrong.data.repository;

import androidx.lifecycle.LiveData;

import com.project.qlcaytrong.data.repository.base.BaseRepository;
import com.project.qlcaytrong.model.ChiTietTuoiPhanModel;

import java.util.List;

/** Contract cho ChiTietTuoiPhan CRUD + sync operations. */
public interface ChiTietTuoiPhanRepository extends BaseRepository<ChiTietTuoiPhanModel> {

    /** Lấy chi tiết bón phân theo nhật ký */
    LiveData<List<ChiTietTuoiPhanModel>> getAllByNhatKyId(String nhatKyId);
}
