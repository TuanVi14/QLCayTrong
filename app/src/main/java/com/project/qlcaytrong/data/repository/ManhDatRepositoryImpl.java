// File: app/src/main/java/com/project/qlcaytrong/data/repository/ManhDatRepositoryImpl.java
package com.project.qlcaytrong.data.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.SetOptions;

import com.project.qlcaytrong.data.local.AppDatabase;
import com.project.qlcaytrong.data.local.entity.ManhDatEntity;
import com.project.qlcaytrong.model.ManhDatModel;
import com.project.qlcaytrong.util.AuthResult;
import com.project.qlcaytrong.util.ModelMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * ManhDatRepositoryImpl — Single Source of Truth là Room.
 *
 * Firestore path: users/{userId}/manh_dat/{manhDatId}
 *
 * Chiến lược đồng bộ:
 *  - Read  → Room (LiveData auto-update)
 *  - Write → Room trước (optimistic) → Firestore background
 *  - Sync  → Firestore → Room (merge/replace)
 */
public class ManhDatRepositoryImpl implements ManhDatRepository {

    private static final String TAG = "ManhDatRepo";

    // Firestore path constants
    private static final String COL_USERS     = "users";
    private static final String COL_MANH_DAT  = "manh_dat";

    private final AppDatabase      db;
    private final FirebaseFirestore firestore;
    private final Executor         ioExecutor;

    public ManhDatRepositoryImpl(Context context) {
        this.db        = AppDatabase.getInstance(context);
        this.firestore = FirebaseFirestore.getInstance();
        this.ioExecutor = Executors.newFixedThreadPool(2);
    }

    // ==================== GET ALL ====================

    @Override
    public LiveData<List<ManhDatModel>> getAllByUserId(String userId) {
        // Map LiveData<List<Entity>> → LiveData<List<Model>> không cần boilerplate
        return Transformations.map(
            db.manhDatDao().getAllByUserId(userId),
            ModelMapper::toModelList
        );
    }

    // ==================== CREATE ====================

    @Override
    public LiveData<AuthResult<ManhDatModel>> create(ManhDatModel model) {
        MutableLiveData<AuthResult<ManhDatModel>> result = new MutableLiveData<>();
        result.setValue(AuthResult.loading());

        // Gán id và timestamp nếu chưa có
        if (model.getId() == null || model.getId().isEmpty()) {
            model.setId(UUID.randomUUID().toString());
        }
        if (model.getNgayTao() == 0) {
            model.setNgayTao(System.currentTimeMillis());
        }
        model.setSyncStatus("PENDING");

        ManhDatEntity entity = ModelMapper.toEntity(model);

        // Bước 1: Lưu Room ngay (optimistic, UI cập nhật tức thì)
        ioExecutor.execute(() -> {
            db.manhDatDao().insert(entity);
            Log.d(TAG, "create: saved to Room id=" + entity.getId());

            // Bước 2: Push Firestore
            pushToFirestore(entity, result, false);
        });

        return result;
    }

    // ==================== UPDATE ====================

    @Override
    public LiveData<AuthResult<ManhDatModel>> update(ManhDatModel model) {
        MutableLiveData<AuthResult<ManhDatModel>> result = new MutableLiveData<>();
        result.setValue(AuthResult.loading());

        model.setSyncStatus("PENDING");
        ManhDatEntity entity = ModelMapper.toEntity(model);

        ioExecutor.execute(() -> {
            db.manhDatDao().update(entity);
            Log.d(TAG, "update: saved to Room id=" + entity.getId());
            pushToFirestore(entity, result, true);
        });

        return result;
    }

    // ==================== DELETE ====================

    @Override
    public LiveData<AuthResult<Void>> delete(ManhDatModel model) {
        MutableLiveData<AuthResult<Void>> result = new MutableLiveData<>();
        result.setValue(AuthResult.loading());

        ManhDatEntity entity = ModelMapper.toEntity(model);

        // Bước 1: Xóa Room ngay → RecyclerView tự cập nhật qua LiveData
        ioExecutor.execute(() -> {
            db.manhDatDao().delete(entity);
            Log.d(TAG, "delete: removed from Room id=" + entity.getId());

            // Bước 2: Xóa Firestore (fire-and-forget, lỗi không rollback Room)
            firestore.collection(COL_USERS)
                .document(model.getUserId())
                .collection(COL_MANH_DAT)
                .document(model.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "delete: Firestore success id=" + model.getId());
                    result.postValue(AuthResult.success(null));
                })
                .addOnFailureListener(e -> {
                    // Không rollback Room — sẽ sync lại sau qua SyncWorker
                    Log.e(TAG, "delete: Firestore failed, local already deleted", e);
                    result.postValue(AuthResult.success(null)); // UI vẫn OK
                });
        });

        return result;
    }

    // ==================== SYNC FROM FIRESTORE ====================

    @Override
    public LiveData<AuthResult<Void>> syncFromFirestore(String userId) {
        MutableLiveData<AuthResult<Void>> result = new MutableLiveData<>();
        result.setValue(AuthResult.loading());

        firestore.collection(COL_USERS)
            .document(userId)
            .collection(COL_MANH_DAT)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                List<ManhDatEntity> remoteList = new ArrayList<>();
                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    Map<String, Object> data = doc.getData();
                    if (data != null) {
                        ManhDatModel remoteModel = ModelMapper.fromFirestoreMap(data);
                        if (remoteModel.getId() == null || remoteModel.getId().isEmpty()) {
                            remoteModel.setId(doc.getId());
                        }
                        remoteModel.setUserId(userId);
                        remoteList.add(ModelMapper.toEntity(remoteModel));
                    }
                }

                // Merge vào Room trên IO thread
                ioExecutor.execute(() -> {
                    for (ManhDatEntity entity : remoteList) {
                        db.manhDatDao().insert(entity); // REPLACE strategy
                    }
                    Log.d(TAG, "syncFromFirestore: merged " + remoteList.size() + " records");
                    result.postValue(AuthResult.success(null));
                });
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "syncFromFirestore failed", e);
                result.postValue(AuthResult.error(mapFirestoreError(e)));
            });

        return result;
    }

    // ==================== PRIVATE HELPERS ====================

    /**
     * Push ManhDatEntity lên Firestore, rồi cập nhật syncStatus trong Room.
     * @param isUpdate true nếu là update, false nếu là insert mới
     */
    private void pushToFirestore(ManhDatEntity entity,
                                 MutableLiveData<AuthResult<ManhDatModel>> result,
                                 boolean isUpdate) {
        Map<String, Object> data = buildFirestoreMap(entity);

        firestore.collection(COL_USERS)
            .document(entity.getUserId())
            .collection(COL_MANH_DAT)
            .document(entity.getId())
            .set(data, SetOptions.merge())
            .addOnSuccessListener(aVoid -> {
                // Cập nhật syncStatus = SYNCED trong Room
                ioExecutor.execute(() -> {
                    entity.setSyncStatus("SYNCED");
                    if (isUpdate) db.manhDatDao().update(entity);
                    else          db.manhDatDao().update(entity);
                    Log.d(TAG, "pushToFirestore: SYNCED id=" + entity.getId());
                });
                result.postValue(AuthResult.success(ModelMapper.toModel(entity)));
            })
            .addOnFailureListener(e -> {
                // Giữ PENDING — SyncWorker sẽ retry sau
                Log.e(TAG, "pushToFirestore: FAILED id=" + entity.getId(), e);
                ioExecutor.execute(() -> {
                    entity.setSyncStatus("FAILED");
                    db.manhDatDao().update(entity);
                });
                // Không crash app — trả về success với model vì local đã lưu OK
                // Chỉ báo lỗi nếu muốn hiển thị warning
                result.postValue(AuthResult.error(mapFirestoreError(e)));
            });
    }

    /** Xây dựng Map để push lên Firestore */
    private Map<String, Object> buildFirestoreMap(ManhDatEntity e) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", e.getId());
        map.put("userId", e.getUserId());
        map.put("tenManhDat", e.getTenManhDat());
        map.put("diaChi", e.getDiaChi());
        map.put("dienTich", e.getDienTich());
        map.put("donViDienTich", e.getDonViDienTich());
        map.put("moTa", e.getMoTa());
        map.put("ngayTao", e.getNgayTao());
        return map;
    }

    /** Chuyển Firestore exception → message tiếng Việt */
    private String mapFirestoreError(Exception e) {
        if (e instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException ffe = (FirebaseFirestoreException) e;
            switch (ffe.getCode()) {
                case PERMISSION_DENIED:
                    return "Không có quyền truy cập. Vui lòng đăng nhập lại.";
                case UNAVAILABLE:
                    return "Máy chủ không khả dụng. Dữ liệu đã lưu cục bộ.";
                case NOT_FOUND:
                    return "Không tìm thấy dữ liệu trên cloud.";
                case DEADLINE_EXCEEDED:
                    return "Kết nối mạng chậm. Vui lòng thử lại.";
                default:
                    break;
            }
        }
        String msg = e.getMessage();
        if (msg != null && msg.contains("NETWORK_ERROR")) {
            return "Không có kết nối mạng. Dữ liệu đã lưu cục bộ.";
        }
        return "Lỗi đồng bộ. Dữ liệu đã lưu cục bộ, sẽ đồng bộ sau.";
    }
}
