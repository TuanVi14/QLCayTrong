// File: app/src/main/java/com/project/qlcaytrong/data/repository/CayTrongRepositoryImpl.java
package com.project.qlcaytrong.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.project.qlcaytrong.data.local.AppDatabase;
import com.project.qlcaytrong.data.local.entity.CayTrongEntity;
import com.project.qlcaytrong.data.repository.base.BaseRepositoryImpl;
import com.project.qlcaytrong.model.CayTrongModel;
import com.project.qlcaytrong.util.AuthResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CayTrongRepositoryImpl
    extends BaseRepositoryImpl<CayTrongModel>
    implements CayTrongRepository {

    private static final String COLLECTION = "cay_trong";
    private final AppDatabase db;

    public CayTrongRepositoryImpl(Context context) {
        super();
        this.db = AppDatabase.getInstance(context);
    }

    // ==================== BaseRepositoryImpl abstract methods ====================

    @Override protected String getCollectionPath() { return COLLECTION; }

    @Override
    protected Map<String, Object> buildFirestoreMap(CayTrongModel m) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", m.getId()); map.put("manhDatId", m.getManhDatId());
        map.put("userId", m.getUserId()); map.put("loaiCay", m.getLoaiCay());
        map.put("tenKhoaHoc", m.getTenKhoaHoc()); map.put("soLuong", m.getSoLuong());
        map.put("donViTinh", m.getDonViTinh()); map.put("ngayTrong", m.getNgayTrong());
        map.put("trangThai", m.getTrangThai()); map.put("moTa", m.getMoTa());
        return map;
    }

    @Override protected String getEntityId(CayTrongModel m) { return m.getId(); }
    @Override protected String getEntityUserId(CayTrongModel m) { return m.getUserId(); }

    @Override
    protected void insertFromRemote(Map<String, Object> data, String userId) {
        CayTrongEntity e = new CayTrongEntity(
            getStr(data, "id"), getStr(data, "manhDatId"), userId,
            getStr(data, "loaiCay"), getStr(data, "tenKhoaHoc"),
            getInt(data, "soLuong"), getStr(data, "donViTinh"),
            getLong(data, "ngayTrong"), getStr(data, "trangThai"),
            getStr(data, "moTa"), "SYNCED"
        );
        db.cayTrongDao().insert(e);
    }

    // ==================== BaseRepository CRUD ====================

    @Override
    public LiveData<AuthResult<CayTrongModel>> insert(CayTrongModel model) {
        MutableLiveData<AuthResult<CayTrongModel>> result = new MutableLiveData<>();
        result.setValue(AuthResult.loading());
        if (model.getId() == null || model.getId().isEmpty())
            model.setId(UUID.randomUUID().toString());
        if (model.getNgayTrong() == 0) model.setNgayTrong(System.currentTimeMillis());
        model.setSyncStatus("PENDING");

        ioExecutor.execute(() -> {
            db.cayTrongDao().insert(toEntity(model));
            pushToFirestore(model, result,
                () -> { model.setSyncStatus("SYNCED"); db.cayTrongDao().update(toEntity(model)); },
                () -> { model.setSyncStatus("FAILED"); db.cayTrongDao().update(toEntity(model)); }
            );
        });
        return result;
    }

    @Override
    public LiveData<AuthResult<CayTrongModel>> update(CayTrongModel model) {
        MutableLiveData<AuthResult<CayTrongModel>> result = new MutableLiveData<>();
        result.setValue(AuthResult.loading());
        model.setSyncStatus("PENDING");

        ioExecutor.execute(() -> {
            db.cayTrongDao().update(toEntity(model));
            pushToFirestore(model, result,
                () -> { model.setSyncStatus("SYNCED"); db.cayTrongDao().update(toEntity(model)); },
                () -> { model.setSyncStatus("FAILED"); db.cayTrongDao().update(toEntity(model)); }
            );
        });
        return result;
    }

    @Override
    public LiveData<AuthResult<Void>> delete(CayTrongModel model) {
        MutableLiveData<AuthResult<Void>> result = new MutableLiveData<>();
        result.setValue(AuthResult.loading());
        ioExecutor.execute(() -> {
            db.cayTrongDao().delete(toEntity(model));
            deleteFromFirestore(model.getUserId(), model.getId(), result);
        });
        return result;
    }

    @Override
    public LiveData<List<CayTrongModel>> getAllByUserId(String userId) {
        return Transformations.map(
            db.cayTrongDao().getAllByUserId(userId),
            entities -> {
                List<CayTrongModel> list = new ArrayList<>();
                if (entities != null) for (CayTrongEntity e : entities) list.add(toModel(e));
                return list;
            }
        );
    }

    @Override
    public LiveData<List<CayTrongModel>> getAllByManhDatId(String manhDatId) {
        return Transformations.map(
            db.cayTrongDao().getAllByManhDatId(manhDatId),
            entities -> {
                List<CayTrongModel> list = new ArrayList<>();
                if (entities != null) for (CayTrongEntity e : entities) list.add(toModel(e));
                return list;
            }
        );
    }

    @Override
    public CayTrongModel getById(String id) {
        CayTrongEntity e = db.cayTrongDao().getById(id);
        return e != null ? toModel(e) : null;
    }

    @Override
    public LiveData<AuthResult<Integer>> syncToFirestore(String userId) {
        MutableLiveData<AuthResult<Integer>> result = new MutableLiveData<>();
        result.setValue(AuthResult.loading());
        ioExecutor.execute(() -> {
            List<CayTrongEntity> pending = db.cayTrongDao().getAllPendingSync();
            // Filter by userId
            List<CayTrongEntity> mine = new ArrayList<>();
            for (CayTrongEntity e : pending) {
                if (userId.equals(e.getUserId())) mine.add(e);
            }
            if (mine.isEmpty()) { result.postValue(AuthResult.success(0)); return; }
            final int[] successCount = {0};
            final int total = mine.size();
            for (CayTrongEntity e : mine) {
                CayTrongModel m = toModel(e);
                pushToFirestore(m, new MutableLiveData<>(),
                    () -> {
                        e.setSyncStatus("SYNCED"); db.cayTrongDao().update(e);
                        successCount[0]++;
                        if (successCount[0] == total)
                            result.postValue(AuthResult.success(successCount[0]));
                    },
                    () -> {
                        e.setSyncStatus("FAILED"); db.cayTrongDao().update(e);
                        successCount[0]++;
                        if (successCount[0] == total)
                            result.postValue(AuthResult.success(successCount[0]));
                    }
                );
            }
        });
        return result;
    }

    @Override
    public LiveData<AuthResult<Void>> syncFromFirestore(String userId) {
        MutableLiveData<AuthResult<Void>> result = new MutableLiveData<>();
        result.setValue(AuthResult.loading());
        fetchAndMerge(userId, result, docId -> {
            CayTrongEntity e = db.cayTrongDao().getById(docId);
            return e != null ? e.getSyncStatus() : null;
        });
        return result;
    }

    // ==================== Mappers ====================

    private CayTrongEntity toEntity(CayTrongModel m) {
        return new CayTrongEntity(m.getId(), m.getManhDatId(), m.getUserId(),
            m.getLoaiCay(), m.getTenKhoaHoc(), m.getSoLuong(), m.getDonViTinh(),
            m.getNgayTrong(), m.getTrangThai(), m.getMoTa(), m.getSyncStatus());
    }

    private CayTrongModel toModel(CayTrongEntity e) {
        return new CayTrongModel(e.getId(), e.getManhDatId(), e.getUserId(),
            e.getLoaiCay(), e.getTenKhoaHoc(), e.getSoLuong(), e.getDonViTinh(),
            e.getNgayTrong(), e.getTrangThai(), e.getMoTa(), e.getSyncStatus());
    }
}
