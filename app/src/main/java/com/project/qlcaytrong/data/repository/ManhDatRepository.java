// File: app/src/main/java/com/project/qlcaytrong/data/repository/ManhDatRepository.java
package com.project.qlcaytrong.data.repository;

import androidx.lifecycle.LiveData;

import com.project.qlcaytrong.model.ManhDatModel;
import com.project.qlcaytrong.util.AuthResult;

import java.util.List;

/**
 * Contract cho ManhDat CRUD + sync operations.
 * ViewModel chỉ phụ thuộc vào interface này.
 */
public interface ManhDatRepository {

    /**
     * Lấy toàn bộ mảnh đất của user — LiveData từ Room (auto-update).
     * Background: kéo Firestore về và merge vào Room khi có mạng.
     */
    LiveData<List<ManhDatModel>> getAllByUserId(String userId);

    /**
     * Tạo mảnh đất mới:
     * 1. Lưu Room với syncStatus = PENDING
     * 2. Push Firestore
     * 3. Cập nhật syncStatus = SYNCED | FAILED
     */
    LiveData<AuthResult<ManhDatModel>> create(ManhDatModel model);

    /**
     * Cập nhật mảnh đất:
     * 1. Update Room với syncStatus = PENDING
     * 2. Push Firestore
     * 3. Cập nhật syncStatus = SYNCED | FAILED
     */
    LiveData<AuthResult<ManhDatModel>> update(ManhDatModel model);

    /**
     * Xóa mảnh đất:
     * 1. Xóa khỏi Room ngay lập tức (UI cập nhật tức thì)
     * 2. Xóa trên Firestore (background)
     */
    LiveData<AuthResult<Void>> delete(ManhDatModel model);

    /**
     * Kéo data từ Firestore về Room một lần (one-shot refresh).
     * Dùng khi mở app lần đầu hoặc kéo để refresh.
     */
    LiveData<AuthResult<Void>> syncFromFirestore(String userId);
}
