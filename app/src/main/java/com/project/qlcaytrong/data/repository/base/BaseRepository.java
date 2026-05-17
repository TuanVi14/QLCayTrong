// File: app/src/main/java/com/project/qlcaytrong/data/repository/base/BaseRepository.java
package com.project.qlcaytrong.data.repository.base;

import androidx.lifecycle.LiveData;

import com.project.qlcaytrong.util.AuthResult;

import java.util.List;

/**
 * Generic contract cho mọi Repository trong hệ thống.
 *
 * <T> = Model POJO (ví dụ CayTrongModel, GocCayModel…)
 *
 * Nguyên tắc Offline-First:
 * - Mọi write đều đi vào Room trước → UI phản hồi tức thì
 * - Firestore được push ở background → không block UI
 * - Nếu Firestore lỗi → giữ PENDING → SyncWorker retry
 *
 * Tại sao Offline-First?
 * - Nông nghiệp thường ở vùng sâu, mạng kém
 * - Người dùng có thể nhập liệu offline, sync khi về vùng phủ sóng
 * - Room làm Single Source of Truth → UI không bao giờ hiển thị stale data
 */
public interface BaseRepository<T> {

    /** Thêm mới: Room (PENDING) → Firestore → SYNCED/FAILED */
    LiveData<AuthResult<T>> insert(T model);

    /** Cập nhật: Room (PENDING) → Firestore → SYNCED/FAILED */
    LiveData<AuthResult<T>> update(T model);

    /**
     * Xóa: Room xóa ngay → Firestore xóa background.
     * Không rollback Room nếu Firestore lỗi — bản ghi đã không còn local.
     */
    LiveData<AuthResult<Void>> delete(T model);

    /** Lấy tất cả theo userId — LiveData từ Room, auto-update */
    LiveData<List<T>> getAllByUserId(String userId);

    /** Đẩy tất cả bản ghi PENDING/FAILED của userId lên Firestore */
    LiveData<AuthResult<Integer>> syncToFirestore(String userId);

    /** Kéo data từ Firestore về Room (merge/replace) */
    LiveData<AuthResult<Void>> syncFromFirestore(String userId);
}
