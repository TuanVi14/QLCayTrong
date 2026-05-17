// File: app/src/main/java/com/project/qlcaytrong/data/repository/base/BaseRepositoryImpl.java
package com.project.qlcaytrong.data.repository.base;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.SetOptions;

import com.project.qlcaytrong.util.AuthResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * BaseRepositoryImpl — Template Method Pattern.
 *
 * Chứa toàn bộ logic sync chung:
 *   - pushToFirestore() — đẩy 1 entity lên Firestore
 *   - deleteFromFirestore() — xóa 1 doc
 *   - fetchCollectionDocs() — kéo toàn bộ subcollection
 *   - mapFirestoreError() — chuyển exception → message VN
 *
 * Subclass chỉ cần implement các abstract method:
 *   - buildFirestoreMap()
 *   - getEntityId() / getEntityUserId()
 *   - getCollectionPath()
 *
 * Firestore path convention:
 *   users/{userId}/{collectionPath}/{docId}
 *
 * Conflict resolution:
 *   Last-Write-Wins dựa trên timestamp ngayTao/ngayThucHien.
 *   Khi syncFromFirestore, nếu local PENDING thì ưu tiên local
 *   (không bị ghi đè bởi cloud version cũ hơn).
 */
public abstract class BaseRepositoryImpl<T> {

    private static final String TAG = "BaseRepo";
    private static final String COL_USERS = "users";

    protected final FirebaseFirestore firestore;
    protected final Executor ioExecutor;

    protected BaseRepositoryImpl() {
        this.firestore  = FirebaseFirestore.getInstance();
        this.ioExecutor = Executors.newFixedThreadPool(2);
    }

    // ==================== Abstract methods (subclass định nghĩa) ====================

    /** Trả về tên subcollection: "cay_trong", "goc_cay", "nhat_ky"… */
    protected abstract String getCollectionPath();

    /** Xây dựng Map<String, Object> để push lên Firestore */
    protected abstract Map<String, Object> buildFirestoreMap(T entity);

    /** Lấy id của entity */
    protected abstract String getEntityId(T entity);

    /** Lấy userId của entity */
    protected abstract String getEntityUserId(T entity);

    /**
     * Convert Firestore Map → Entity và insert/replace vào Room.
     * Được gọi trong syncFromFirestore — subclass xử lý mapping + DAO.
     */
    protected abstract void insertFromRemote(Map<String, Object> data, String userId);

    // ==================== Shared push logic ====================

    /**
     * Đẩy 1 entity lên Firestore.
     * Sau khi thành công: gọi onSuccess(entity)
     * Sau khi lỗi: gọi onFailure(entity)
     */
    protected void pushToFirestore(T entity,
                                   MutableLiveData<AuthResult<T>> result,
                                   Runnable onSuccess,
                                   Runnable onFailure) {
        String userId = getEntityUserId(entity);
        String docId  = getEntityId(entity);
        Map<String, Object> data = buildFirestoreMap(entity);

        getCollectionRef(userId)
            .document(docId)
            .set(data, SetOptions.merge())
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, getCollectionPath() + " SYNCED: " + docId);
                ioExecutor.execute(onSuccess);
                result.postValue(AuthResult.success(entity));
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, getCollectionPath() + " FAILED: " + docId, e);
                ioExecutor.execute(onFailure);
                // Không crash — local đã lưu, SyncWorker sẽ retry
                result.postValue(AuthResult.error(mapFirestoreError(e)));
            });
    }

    /**
     * Xóa document khỏi Firestore (fire-and-forget).
     * Không rollback Room nếu lỗi — local đã xóa rồi.
     */
    protected void deleteFromFirestore(String userId,
                                       String docId,
                                       MutableLiveData<AuthResult<Void>> result) {
        getCollectionRef(userId)
            .document(docId)
            .delete()
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, getCollectionPath() + " deleted on Firestore: " + docId);
                result.postValue(AuthResult.success(null));
            })
            .addOnFailureListener(e -> {
                Log.w(TAG, getCollectionPath() + " Firestore delete failed (local OK): " + docId, e);
                // Local đã xóa → trả về success để UI không bị confused
                result.postValue(AuthResult.success(null));
            });
    }

    /**
     * Kéo toàn bộ documents của subcollection từ Firestore về Room.
     * Conflict resolution: nếu local version là PENDING → bỏ qua remote version
     * (local sẽ ghi đè lên cloud ở lần sync tiếp theo).
     */
    protected void fetchAndMerge(String userId,
                                 MutableLiveData<AuthResult<Void>> result,
                                 java.util.function.Function<String, String> localSyncStatusGetter) {
        getCollectionRef(userId)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                List<Map<String, Object>> remoteDocs = new ArrayList<>();
                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    if (doc.getData() != null) remoteDocs.add(doc.getData());
                }

                ioExecutor.execute(() -> {
                    int merged = 0;
                    for (Map<String, Object> data : remoteDocs) {
                        String docId = getStr(data, "id");
                        if (docId == null || docId.isEmpty()) docId = getStr(data, "id");

                        // Conflict resolution: local PENDING → skip (local wins)
                        if (localSyncStatusGetter != null) {
                            String localStatus = localSyncStatusGetter.apply(docId);
                            if ("PENDING".equals(localStatus) || "FAILED".equals(localStatus)) {
                                Log.d(TAG, "Skip remote (local PENDING/FAILED): " + docId);
                                continue;
                            }
                        }

                        insertFromRemote(data, userId);
                        merged++;
                    }
                    Log.d(TAG, "syncFromFirestore: merged " + merged + " / " + remoteDocs.size());
                    result.postValue(AuthResult.success(null));
                });
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "syncFromFirestore failed for " + getCollectionPath(), e);
                result.postValue(AuthResult.error(mapFirestoreError(e)));
            });
    }

    // ==================== Helpers ====================

    protected CollectionReference getCollectionRef(String userId) {
        return firestore
            .collection(COL_USERS)
            .document(userId)
            .collection(getCollectionPath());
    }

    protected String mapFirestoreError(Exception e) {
        if (e instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException ffe = (FirebaseFirestoreException) e;
            switch (ffe.getCode()) {
                case PERMISSION_DENIED:
                    return "Không có quyền. Vui lòng đăng nhập lại.";
                case UNAVAILABLE:
                    return "Máy chủ không khả dụng. Dữ liệu đã lưu cục bộ.";
                case NOT_FOUND:
                    return "Tài nguyên không tìm thấy trên cloud.";
                case DEADLINE_EXCEEDED:
                    return "Kết nối chậm. Vui lòng thử lại.";
                case RESOURCE_EXHAUSTED:
                    return "Vượt giới hạn Firestore. Thử lại sau.";
                default:
                    break;
            }
        }
        String msg = e != null ? e.getMessage() : null;
        if (msg != null && (msg.contains("NETWORK_ERROR") || msg.contains("network"))) {
            return "Không có mạng. Dữ liệu đã lưu cục bộ, sẽ đồng bộ sau.";
        }
        return "Lỗi đồng bộ. Dữ liệu an toàn ở thiết bị.";
    }

    protected String getStr(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : "";
    }

    protected double getDouble(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val instanceof Number ? ((Number) val).doubleValue() : 0.0;
    }

    protected long getLong(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val instanceof Number ? ((Number) val).longValue() : 0L;
    }

    protected int getInt(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val instanceof Number ? ((Number) val).intValue() : 0;
    }
}
