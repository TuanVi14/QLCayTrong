// File: app/src/main/java/com/project/qlcaytrong/data/repository/NhatKyRepositoryImpl.java
package com.project.qlcaytrong.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.project.qlcaytrong.data.local.AppDatabase;
import com.project.qlcaytrong.data.local.entity.NhatKyEntity;
import com.project.qlcaytrong.data.repository.base.BaseRepositoryImpl;
import com.project.qlcaytrong.model.NhatKyModel;
import com.project.qlcaytrong.util.AuthResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class NhatKyRepositoryImpl
    extends BaseRepositoryImpl<NhatKyModel>
    implements NhatKyRepository {

    private static final String COLLECTION = "nhat_ky";
    private final AppDatabase db;

    public NhatKyRepositoryImpl(Context context) {
        super();
        this.db = AppDatabase.getInstance(context);
    }

    @Override protected String getCollectionPath() { return COLLECTION; }

    @Override
    protected Map<String, Object> buildFirestoreMap(NhatKyModel m) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", m.getId()); map.put("gocCayId", m.getGocCayId());
        map.put("cayTrongId", m.getCayTrongId()); map.put("userId", m.getUserId());
        map.put("loaiNhatKy", m.getLoaiNhatKy()); map.put("ngayThucHien", m.getNgayThucHien());
        map.put("nguoiThucHien", m.getNguoiThucHien()); map.put("hinhAnh", m.getHinhAnh());
        map.put("ghiChu", m.getGhiChu());
        return map;
    }

    @Override protected String getEntityId(NhatKyModel m) { return m.getId(); }
    @Override protected String getEntityUserId(NhatKyModel m) { return m.getUserId(); }

    @Override
    protected void insertFromRemote(Map<String, Object> data, String userId) {
        NhatKyEntity e = new NhatKyEntity(
            getStr(data, "id"), getStr(data, "gocCayId"), getStr(data, "cayTrongId"),
            userId, getStr(data, "loaiNhatKy"), getLong(data, "ngayThucHien"),
            getStr(data, "nguoiThucHien"), getStr(data, "hinhAnh"),
            getStr(data, "ghiChu"), "SYNCED"
        );
        db.nhatKyDao().insert(e);
    }

    @Override
    public LiveData<AuthResult<NhatKyModel>> insert(NhatKyModel model) {
        MutableLiveData<AuthResult<NhatKyModel>> result = new MutableLiveData<>();
        result.setValue(AuthResult.loading());
        if (model.getId() == null || model.getId().isEmpty())
            model.setId(UUID.randomUUID().toString());
        if (model.getNgayThucHien() == 0) model.setNgayThucHien(System.currentTimeMillis());
        model.setSyncStatus("PENDING");

        ioExecutor.execute(() -> {
            db.nhatKyDao().insert(toEntity(model));
            pushToFirestore(model, result,
                () -> { model.setSyncStatus("SYNCED"); db.nhatKyDao().update(toEntity(model)); },
                () -> { model.setSyncStatus("FAILED"); db.nhatKyDao().update(toEntity(model)); }
            );
        });
        return result;
    }

    @Override
    public LiveData<AuthResult<NhatKyModel>> update(NhatKyModel model) {
        MutableLiveData<AuthResult<NhatKyModel>> result = new MutableLiveData<>();
        result.setValue(AuthResult.loading());
        model.setSyncStatus("PENDING");
        ioExecutor.execute(() -> {
            db.nhatKyDao().update(toEntity(model));
            pushToFirestore(model, result,
                () -> { model.setSyncStatus("SYNCED"); db.nhatKyDao().update(toEntity(model)); },
                () -> { model.setSyncStatus("FAILED"); db.nhatKyDao().update(toEntity(model)); }
            );
        });
        return result;
    }

    @Override
    public LiveData<AuthResult<Void>> delete(NhatKyModel model) {
        MutableLiveData<AuthResult<Void>> result = new MutableLiveData<>();
        result.setValue(AuthResult.loading());
        ioExecutor.execute(() -> {
            db.nhatKyDao().delete(toEntity(model));
            deleteFromFirestore(model.getUserId(), model.getId(), result);
        });
        return result;
    }

    @Override
    public LiveData<List<NhatKyModel>> getAllByUserId(String userId) {
        return Transformations.map(db.nhatKyDao().getAllByUserId(userId),
            entities -> toModelList(entities));
    }

    @Override
    public LiveData<List<NhatKyModel>> getAllByGocCayId(String gocCayId) {
        return Transformations.map(db.nhatKyDao().getAllByGocCayId(gocCayId),
            entities -> toModelList(entities));
    }

    @Override
    public LiveData<List<NhatKyModel>> getAllByCayTrongId(String cayTrongId) {
        return Transformations.map(db.nhatKyDao().getAllByCayTrongId(cayTrongId),
            entities -> toModelList(entities));
    }

    @Override
    public LiveData<List<NhatKyModel>> getAllByLoai(String loaiNhatKy, String userId) {
        return Transformations.map(db.nhatKyDao().getAllByLoai(loaiNhatKy, userId),
            entities -> toModelList(entities));
    }

    @Override
    public LiveData<AuthResult<Integer>> syncToFirestore(String userId) {
        MutableLiveData<AuthResult<Integer>> result = new MutableLiveData<>();
        result.setValue(AuthResult.loading());
        ioExecutor.execute(() -> {
            List<NhatKyEntity> pending = db.nhatKyDao().getAllPendingSync();
            List<NhatKyEntity> mine = new ArrayList<>();
            for (NhatKyEntity e : pending) if (userId.equals(e.getUserId())) mine.add(e);
            if (mine.isEmpty()) { result.postValue(AuthResult.success(0)); return; }
            final int[] count = {0};
            for (NhatKyEntity e : mine) {
                pushToFirestore(toModel(e), new MutableLiveData<>(),
                    () -> { e.setSyncStatus("SYNCED"); db.nhatKyDao().update(e);
                            if (++count[0] == mine.size()) result.postValue(AuthResult.success(count[0])); },
                    () -> { e.setSyncStatus("FAILED"); db.nhatKyDao().update(e);
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
            NhatKyEntity e = db.nhatKyDao().getById(docId);
            return e != null ? e.getSyncStatus() : null;
        });
        return result;
    }

    private List<NhatKyModel> toModelList(List<NhatKyEntity> entities) {
        List<NhatKyModel> list = new ArrayList<>();
        if (entities != null) for (NhatKyEntity e : entities) list.add(toModel(e));
        return list;
    }

    private NhatKyEntity toEntity(NhatKyModel m) {
        return new NhatKyEntity(m.getId(), m.getGocCayId(), m.getCayTrongId(),
            m.getUserId(), m.getLoaiNhatKy(), m.getNgayThucHien(),
            m.getNguoiThucHien(), m.getHinhAnh(), m.getGhiChu(), m.getSyncStatus());
    }

    private NhatKyModel toModel(NhatKyEntity e) {
        return new NhatKyModel(e.getId(), e.getGocCayId(), e.getCayTrongId(),
            e.getUserId(), e.getLoaiNhatKy(), e.getNgayThucHien(),
            e.getNguoiThucHien(), e.getHinhAnh(), e.getGhiChu(), e.getSyncStatus());
    }
}
