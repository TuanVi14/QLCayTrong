// File: app/src/main/java/com/project/qlcaytrong/data/repository/ChiTietPhunThuocRepositoryImpl.java
package com.project.qlcaytrong.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.project.qlcaytrong.data.local.AppDatabase;
import com.project.qlcaytrong.data.local.entity.ChiTietPhunThuocEntity;
import com.project.qlcaytrong.data.repository.base.BaseRepositoryImpl;
import com.project.qlcaytrong.model.ChiTietPhunThuocModel;
import com.project.qlcaytrong.util.AuthResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ChiTietPhunThuocRepositoryImpl
    extends BaseRepositoryImpl<ChiTietPhunThuocModel>
    implements ChiTietPhunThuocRepository {

    private static final String COLLECTION = "chi_tiet_phun_thuoc";
    private final AppDatabase db;

    public ChiTietPhunThuocRepositoryImpl(Context context) {
        super();
        this.db = AppDatabase.getInstance(context);
    }

    @Override protected String getCollectionPath() { return COLLECTION; }

    @Override
    protected Map<String, Object> buildFirestoreMap(ChiTietPhunThuocModel m) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", m.getId()); map.put("nhatKyId", m.getNhatKyId());
        map.put("userId", m.getUserId()); map.put("tenThuoc", m.getTenThuoc());
        map.put("lieuLuong", m.getLieuLuong()); map.put("donVi", m.getDonVi());
        map.put("lyDoPhun", m.getLyDoPhun());
        return map;
    }

    @Override protected String getEntityId(ChiTietPhunThuocModel m) { return m.getId(); }
    @Override protected String getEntityUserId(ChiTietPhunThuocModel m) { return m.getUserId(); }

    @Override
    protected void insertFromRemote(Map<String, Object> data, String userId) {
        ChiTietPhunThuocEntity e = new ChiTietPhunThuocEntity(
            getStr(data, "id"), getStr(data, "nhatKyId"), userId,
            getStr(data, "tenThuoc"), getDouble(data, "lieuLuong"),
            getStr(data, "donVi"), getStr(data, "lyDoPhun"), "SYNCED"
        );
        db.chiTietPhunThuocDao().insert(e);
    }

    @Override
    public LiveData<AuthResult<ChiTietPhunThuocModel>> insert(ChiTietPhunThuocModel model) {
        MutableLiveData<AuthResult<ChiTietPhunThuocModel>> result = new MutableLiveData<>();
        result.setValue(AuthResult.loading());
        if (model.getId() == null || model.getId().isEmpty())
            model.setId(UUID.randomUUID().toString());
        model.setSyncStatus("PENDING");
        ioExecutor.execute(() -> {
            db.chiTietPhunThuocDao().insert(toEntity(model));
            pushToFirestore(model, result,
                () -> { model.setSyncStatus("SYNCED"); db.chiTietPhunThuocDao().update(toEntity(model)); },
                () -> { model.setSyncStatus("FAILED"); db.chiTietPhunThuocDao().update(toEntity(model)); }
            );
        });
        return result;
    }

    @Override
    public LiveData<AuthResult<ChiTietPhunThuocModel>> update(ChiTietPhunThuocModel model) {
        MutableLiveData<AuthResult<ChiTietPhunThuocModel>> result = new MutableLiveData<>();
        result.setValue(AuthResult.loading());
        model.setSyncStatus("PENDING");
        ioExecutor.execute(() -> {
            db.chiTietPhunThuocDao().update(toEntity(model));
            pushToFirestore(model, result,
                () -> { model.setSyncStatus("SYNCED"); db.chiTietPhunThuocDao().update(toEntity(model)); },
                () -> { model.setSyncStatus("FAILED"); db.chiTietPhunThuocDao().update(toEntity(model)); }
            );
        });
        return result;
    }

    @Override
    public LiveData<AuthResult<Void>> delete(ChiTietPhunThuocModel model) {
        MutableLiveData<AuthResult<Void>> result = new MutableLiveData<>();
        result.setValue(AuthResult.loading());
        ioExecutor.execute(() -> {
            db.chiTietPhunThuocDao().delete(toEntity(model));
            deleteFromFirestore(model.getUserId(), model.getId(), result);
        });
        return result;
    }

    @Override
    public LiveData<List<ChiTietPhunThuocModel>> getAllByUserId(String userId) {
        return Transformations.map(db.chiTietPhunThuocDao().getAllByUserId(userId),
            entities -> toModelList(entities));
    }

    @Override
    public LiveData<List<ChiTietPhunThuocModel>> getAllByNhatKyId(String nhatKyId) {
        return Transformations.map(db.chiTietPhunThuocDao().getAllByNhatKyId(nhatKyId),
            entities -> toModelList(entities));
    }

    @Override
    public LiveData<AuthResult<Integer>> syncToFirestore(String userId) {
        MutableLiveData<AuthResult<Integer>> result = new MutableLiveData<>();
        result.setValue(AuthResult.loading());
        ioExecutor.execute(() -> {
            List<ChiTietPhunThuocEntity> pending = db.chiTietPhunThuocDao().getAllPendingSync();
            List<ChiTietPhunThuocEntity> mine = new ArrayList<>();
            for (ChiTietPhunThuocEntity e : pending) if (userId.equals(e.getUserId())) mine.add(e);
            if (mine.isEmpty()) { result.postValue(AuthResult.success(0)); return; }
            final int[] count = {0};
            for (ChiTietPhunThuocEntity e : mine) {
                pushToFirestore(toModel(e), new MutableLiveData<>(),
                    () -> { e.setSyncStatus("SYNCED"); db.chiTietPhunThuocDao().update(e);
                            if (++count[0] == mine.size()) result.postValue(AuthResult.success(count[0])); },
                    () -> { e.setSyncStatus("FAILED"); db.chiTietPhunThuocDao().update(e);
                            if (++count[0] == mine.size()) result.postValue(AuthResult.success(count[0])); }
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
            ChiTietPhunThuocEntity e = db.chiTietPhunThuocDao().getById(docId);
            return e != null ? e.getSyncStatus() : null;
        });
        return result;
    }

    private List<ChiTietPhunThuocModel> toModelList(List<ChiTietPhunThuocEntity> list) {
        List<ChiTietPhunThuocModel> result = new ArrayList<>();
        if (list != null) for (ChiTietPhunThuocEntity e : list) result.add(toModel(e));
        return result;
    }

    private ChiTietPhunThuocEntity toEntity(ChiTietPhunThuocModel m) {
        return new ChiTietPhunThuocEntity(m.getId(), m.getNhatKyId(), m.getUserId(),
            m.getTenThuoc(), m.getLieuLuong(), m.getDonVi(), m.getLyDoPhun(), m.getSyncStatus());
    }

    private ChiTietPhunThuocModel toModel(ChiTietPhunThuocEntity e) {
        return new ChiTietPhunThuocModel(e.getId(), e.getNhatKyId(), e.getUserId(),
            e.getTenThuoc(), e.getLieuLuong(), e.getDonVi(), e.getLyDoPhun(), e.getSyncStatus());
    }
}
