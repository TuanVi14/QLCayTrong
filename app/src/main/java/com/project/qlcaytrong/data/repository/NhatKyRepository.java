// File: app/src/main/java/com/project/qlcaytrong/data/repository/NhatKyRepository.java
package com.project.qlcaytrong.data.repository;

import androidx.lifecycle.LiveData;

import com.project.qlcaytrong.data.repository.base.BaseRepository;
import com.project.qlcaytrong.model.NhatKyModel;

import java.util.List;

/** Contract cho NhatKy CRUD + sync operations. */
public interface NhatKyRepository extends BaseRepository<NhatKyModel> {

    /** Lấy nhật ký theo gốc cây */
    LiveData<List<NhatKyModel>> getAllByGocCayId(String gocCayId);

    /** Lấy nhật ký theo cây trồng */
    LiveData<List<NhatKyModel>> getAllByCayTrongId(String cayTrongId);

    /** Lấy nhật ký theo loại (TUOI_PHAN / PHUN_THUOC…) */
    LiveData<List<NhatKyModel>> getAllByLoai(String loaiNhatKy, String userId);
}
