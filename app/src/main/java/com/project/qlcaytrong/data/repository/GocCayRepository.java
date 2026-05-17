// File: app/src/main/java/com/project/qlcaytrong/data/repository/GocCayRepository.java
package com.project.qlcaytrong.data.repository;

import com.project.qlcaytrong.data.repository.base.BaseRepository;
import com.project.qlcaytrong.model.GocCayModel;

import androidx.lifecycle.LiveData;
import java.util.List;

/** Contract cho GocCay CRUD + sync operations. */
public interface GocCayRepository extends BaseRepository<GocCayModel> {

    /** Lấy gốc cây theo cayTrongId — LiveData */
    LiveData<List<GocCayModel>> getAllByCayTrongId(String cayTrongId);

    /** Tìm gốc cây theo mã QR (blocking) */
    GocCayModel getByQRCode(String maQRCode);
}
