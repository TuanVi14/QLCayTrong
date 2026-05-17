// File: app/src/main/java/com/project/qlcaytrong/util/SyncStatus.java
package com.project.qlcaytrong.util;

/**
 * Trạng thái đồng bộ của mỗi bản ghi Room → Firestore.
 *
 * Vòng đời điển hình:
 *   PENDING  →  (Firestore OK)  →  SYNCED
 *   PENDING  →  (Firestore lỗi) →  FAILED
 *   FAILED   →  (SyncWorker retry)  →  PENDING  →  SYNCED
 */
public enum SyncStatus {
    /** Đã lưu local, chưa đẩy lên cloud */
    PENDING,

    /** Đã đồng bộ lên Firestore thành công */
    SYNCED,

    /** Firestore thất bại — sẽ retry bởi SyncWorker */
    FAILED;

    public static SyncStatus from(String value) {
        if (value == null) return PENDING;
        try { return SyncStatus.valueOf(value); }
        catch (IllegalArgumentException e) { return PENDING; }
    }
}
