// File: app/src/main/java/com/project/qlcaytrong/data/repository/ChiTietTuoiPhanRepositoryImpl.java
package com.project.qlcaytrong.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.project.qlcaytrong.data.local.AppDatabase;
import com.project.qlcaytrong.data.local.entity.ChiTietTuoiPhanEntity;
import com.project.qlcaytrong.data.repository.base.BaseRepositoryImpl;
import com.project.qlcaytrong.model.ChiTietTuoiPhanModel;
import com.project.qlcaytrong.util.AuthResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ChiTietTuoiPhanRepositoryImpl
    extends BaseRepositoryImpl<ChiTietTuoiPhanModel>
    implements ChiTietTuoiPhanRepository {

    private static final String COLLECTION = "chi_tiet_tuoi_phan";
    private final AppDatabase db;

    public ChiTietTuoiPhanRepositoryImpl(Context context) {
        super();
        this.db = AppDatabase.getInstance(context);
    }

    @Override protected String getCollectionPath() { return COLLECTION; }

    @Override
    protected Map<String, Object> buildFirestoreMap(ChiTietTuoiPhanModel m) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", m.getId()); map.put("nhatKyId", m.getNhatKyId());
        map.put("userId", m.getUserId()); map.put("tenPhan", m.getTenPhan());
        map.put("lieuLuong", m.getLieuLuong()); map.put("donVi", m.getDonVi());
        map.put("cachBon", m.getCachBon());
        return map;
    }

    @Override protected String getEntityId(ChiTietTuoiPhanModel m) { return m.getId(); }
    @Override protected String getEntityUserId(ChiTietTuoiPhanModel m) { return m.getUserId(); }

    @Override
    protected void insertFromRemote(Map<String, Object> data, String userId) {
        ChiTietTuoiPhanEntity e = new ChiTietTuoiPhanEntity(
            getStr(data, "id"), getStr(data, "nhatKyId"), userId,
            getStr(data, "tenPhan"), getDouble(data, "lieuLuong"),
            getStr(data, "donVi"), getStr(data, "cachBon"), "SYNCED"
        );
        db.chiTietTuoiPhanDao().insert(e);
    }

    @Override
    public LiveData<AuthResult<ChiTietTuoiPhanModel>> insert(ChiTietTuoiPhanModel model) {
        MutableLiveData<AuthResult<ChiTietTuoiPhanModel>> result = new MutableLiveData<>();
        result.setValue(AuthResult.loading());
        if (model.getId() == null || model.getId().isEmpty())
            model.setId(UUID.randomUUID().toString());
        model.setSyncStatus("PENDING");
        ioExecutor.execute(() -> {
            db.chiTietTuoiPhanDao().insert(toEntity(model));
            pushToFirestore(model, result,
                () -> { model.setSyncStatus("SYNCED"); db.chiTietTuoiPhanDao().update(toEntity(model)); },
                () -> { model.setSyncStatus("FAILED"); db.chiTietTuoiPhanDao().update(toEntity(model)); }
            );
        });
        return result;
    }

    @Override
    public LiveData<AuthResult<ChiTietTuoiPhanModel>> update(ChiTietTuoiPhanModel model) {
        MutableLiveData<AuthResult<ChiTietTuoiPhanModel>> result = new MutableLiveData<>();
        result.setValue(AuthResult.loading());
        model.setSyncStatus("PENDING");
        ioExecutor.execute(() -> {
            db.chiTietTuoiPhanDao().update(toEntity(model));
            pushToFirestore(model, result,
                () -> { model.setSyncStatus("SYNCED"); db.chiTietTuoiPhanDao().update(toEntity(model)); },
                () -> { model.setSyncStatus("FAILED"); db.chiTietTuoiPhanDao().update(toEntity(model)); }
            );
        });
        return result;
    }

    @Override
    public LiveData<AuthResult<Void>> delete(ChiTietTuoiPhanModel model) {
        MutableLiveData<AuthResult<Void>> result = new MutableLiveData<>();
        result.setValue(AuthResult.loading());
        ioExecutor.execute(() -> {
            db.chiTietTuoiPhanDao().delete(toEntity(model));
            deleteFromFirestore(model.getUserId(), model.getId(), result);
        });
        return result;
    }

    @Override
    public LiveData<List<ChiTietTuoiPhanModel>> getAllByUserId(String userId) {
        return Transformations.map(db.chiTietTuoiPhanDao().getAllByUserId(userId),
            entities -> toModelList(entities));
    }

    @Override
    public LiveData<List<ChiTietTuoiPhanModel>> getAllByNhatKyId(String nhatKyId) {
        return Transformations.map(db.chiTietTuoiPhanDao().getAllByNhatKyId(nhatKyId),
            entities -> toModelList(entities));
    }

    @Override
    public LiveData<AuthResult<Integer>> syncToFirestore(String userId) {
        MutableLiveData<AuthResult<Integer>> result = new MutableLiveData<>();
        result.setValue(AuthResult.loading());
        ioExecutor.execute(() -> {
            List<ChiTietTuoiPhanEntity> pending = db.chiTietTuoiPhanDao().getAllPendingSync();
            List<ChiTietTuoiPhanEntity> mine = new ArrayList<>();
            for (ChiTietTuoiPhanEntity e : pending) if (userId.equals(e.getUserId())) mine.add(e);
            if (mine.isEmpty()) { result.postValue(AuthResult.success(0)); return; }
            final int[] count = {0};
            for (ChiTietTuoiPhanEntity e : mine) {
                pushToFirestore(toModel(e), new MutableLiveData<>(),
                    () -> { e.setSyncStatus("SYNCED"); db.chiTietTuoiPhanDao().update(e);
                            if (++count[0] == mine.size()) result.postValue(AuthResult.success(count[0])); },
                    () -> { e.setSyncStatus("FAILED"); db.chiTietTuoiPhanDao().update(e);
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
            ChiTietTuoiPhanEntity e = db.chiTietTuoiPhanDao().getById(docId);
            return e != null ? e.getSyncStatus() : null;
        });
        return result;
    }

    private List<ChiTietTuoiPhanModel> toModelList(List<ChiTietTuoiPhanEntity> list) {
        List<ChiTietTuoiPhanModel> result = new ArrayList<>();
        if (list != null) for (ChiTietTuoiPhanEntity e : list) result.add(toModel(e));
        return result;
    }

    private ChiTietTuoiPhanEntity toEntity(ChiTietTuoiPhanModel m) {
        return new ChiTietTuoiPhanEntity(m.getId(), m.getNhatKyId(), m.getUserId(),
            m.getTenPhan(), m.getLieuLuong(), m.getDonVi(), m.getCachBon(), m.getSyncStatus());
    }

    private ChiTietTuoiPhanModel toModel(ChiTietTuoiPhanEntity e) {
        return new ChiTietTuoiPhanModel(e.getId(), e.getNhatKyId(), e.getUserId(),
            e.getTenPhan(), e.getLieuLuong(), e.getDonVi(), e.getCachBon(), e.getSyncStatus());
    }
}
