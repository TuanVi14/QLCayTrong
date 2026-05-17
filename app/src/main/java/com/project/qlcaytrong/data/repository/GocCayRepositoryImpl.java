// File: app/src/main/java/com/project/qlcaytrong/data/repository/GocCayRepositoryImpl.java
package com.project.qlcaytrong.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.project.qlcaytrong.data.local.AppDatabase;
import com.project.qlcaytrong.data.local.entity.GocCayEntity;
import com.project.qlcaytrong.data.repository.base.BaseRepositoryImpl;
import com.project.qlcaytrong.model.GocCayModel;
import com.project.qlcaytrong.util.AuthResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GocCayRepositoryImpl
    extends BaseRepositoryImpl<GocCayModel>
    implements GocCayRepository {

    private static final String COLLECTION = "goc_cay";
    private final AppDatabase db;

    public GocCayRepositoryImpl(Context context) {
        super();
        this.db = AppDatabase.getInstance(context);
    }

    @Override protected String getCollectionPath() { return COLLECTION; }

    @Override
    protected Map<String, Object> buildFirestoreMap(GocCayModel m) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", m.getId()); map.put("cayTrongId", m.getCayTrongId());
        map.put("userId", m.getUserId()); map.put("maQRCode", m.getMaQRCode());
        map.put("viTri", m.getViTri()); map.put("trangThai", m.getTrangThai());
        map.put("ngayTrong", m.getNgayTrong()); map.put("ghiChu", m.getGhiChu());
        return map;
    }

    @Override protected String getEntityId(GocCayModel m) { return m.getId(); }
    @Override protected String getEntityUserId(GocCayModel m) { return m.getUserId(); }

    @Override
    protected void insertFromRemote(Map<String, Object> data, String userId) {
        GocCayEntity e = new GocCayEntity(
            getStr(data, "id"), getStr(data, "cayTrongId"), userId,
            getStr(data, "maQRCode"), getStr(data, "viTri"),
            getStr(data, "trangThai"), getLong(data, "ngayTrong"),
            getStr(data, "ghiChu"), "SYNCED"
        );
        db.gocCayDao().insert(e);
    }

    @Override
    public LiveData<AuthResult<GocCayModel>> insert(GocCayModel model) {
        MutableLiveData<AuthResult<GocCayModel>> result = new MutableLiveData<>();
        result.setValue(AuthResult.loading());
        if (model.getId() == null || model.getId().isEmpty())
            model.setId(UUID.randomUUID().toString());
        if (model.getNgayTrong() == 0) model.setNgayTrong(System.currentTimeMillis());
        model.setSyncStatus("PENDING");

        ioExecutor.execute(() -> {
            db.gocCayDao().insert(toEntity(model));
            pushToFirestore(model, result,
                () -> { model.setSyncStatus("SYNCED"); db.gocCayDao().update(toEntity(model)); },
                () -> { model.setSyncStatus("FAILED"); db.gocCayDao().update(toEntity(model)); }
            );
        });
        return result;
    }

    @Override
    public LiveData<AuthResult<GocCayModel>> update(GocCayModel model) {
        MutableLiveData<AuthResult<GocCayModel>> result = new MutableLiveData<>();
        result.setValue(AuthResult.loading());
        model.setSyncStatus("PENDING");
        ioExecutor.execute(() -> {
            db.gocCayDao().update(toEntity(model));
            pushToFirestore(model, result,
                () -> { model.setSyncStatus("SYNCED"); db.gocCayDao().update(toEntity(model)); },
                () -> { model.setSyncStatus("FAILED"); db.gocCayDao().update(toEntity(model)); }
            );
        });
        return result;
    }

    @Override
    public LiveData<AuthResult<Void>> delete(GocCayModel model) {
        MutableLiveData<AuthResult<Void>> result = new MutableLiveData<>();
        result.setValue(AuthResult.loading());
        ioExecutor.execute(() -> {
            db.gocCayDao().delete(toEntity(model));
            deleteFromFirestore(model.getUserId(), model.getId(), result);
        });
        return result;
    }

    @Override
    public LiveData<List<GocCayModel>> getAllByUserId(String userId) {
        return Transformations.map(db.gocCayDao().getAllByUserId(userId),
            entities -> {
                List<GocCayModel> list = new ArrayList<>();
                if (entities != null) for (GocCayEntity e : entities) list.add(toModel(e));
                return list;
            });
    }

    @Override
    public LiveData<List<GocCayModel>> getAllByCayTrongId(String cayTrongId) {
        return Transformations.map(db.gocCayDao().getAllByCayTrongId(cayTrongId),
            entities -> {
                List<GocCayModel> list = new ArrayList<>();
                if (entities != null) for (GocCayEntity e : entities) list.add(toModel(e));
                return list;
            });
    }

    @Override
    public GocCayModel getByQRCode(String maQRCode) {
        GocCayEntity e = db.gocCayDao().getByQRCode(maQRCode);
        return e != null ? toModel(e) : null;
    }

    @Override
    public LiveData<AuthResult<Integer>> syncToFirestore(String userId) {
        MutableLiveData<AuthResult<Integer>> result = new MutableLiveData<>();
        result.setValue(AuthResult.loading());
        ioExecutor.execute(() -> {
            List<GocCayEntity> pending = db.gocCayDao().getAllPendingSync();
            List<GocCayEntity> mine = new ArrayList<>();
            for (GocCayEntity e : pending) if (userId.equals(e.getUserId())) mine.add(e);
            if (mine.isEmpty()) { result.postValue(AuthResult.success(0)); return; }
            final int[] count = {0};
            for (GocCayEntity e : mine) {
                pushToFirestore(toModel(e), new MutableLiveData<>(),
                    () -> { e.setSyncStatus("SYNCED"); db.gocCayDao().update(e);
                            if (++count[0] == mine.size()) result.postValue(AuthResult.success(count[0])); },
                    () -> { e.setSyncStatus("FAILED"); db.gocCayDao().update(e);
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
            GocCayEntity e = db.gocCayDao().getById(docId);
            return e != null ? e.getSyncStatus() : null;
        });
        return result;
    }

    private GocCayEntity toEntity(GocCayModel m) {
        return new GocCayEntity(m.getId(), m.getCayTrongId(), m.getUserId(),
            m.getMaQRCode(), m.getViTri(), m.getTrangThai(),
            m.getNgayTrong(), m.getGhiChu(), m.getSyncStatus());
    }

    private GocCayModel toModel(GocCayEntity e) {
        return new GocCayModel(e.getId(), e.getCayTrongId(), e.getUserId(),
            e.getMaQRCode(), e.getViTri(), e.getTrangThai(),
            e.getNgayTrong(), e.getGhiChu(), e.getSyncStatus());
    }
}
