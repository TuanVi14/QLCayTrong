// File: app/src/main/java/com/project/qlcaytrong/util/ModelMapper.java
package com.project.qlcaytrong.util;

import com.project.qlcaytrong.data.local.entity.ManhDatEntity;
import com.project.qlcaytrong.model.ManhDatModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Chuyển đổi giữa Entity (Room), Model (POJO) và Map (Firestore).
 * Giữ clean architecture: Repository dùng class này thay vì convert inline.
 */
public class ModelMapper {

    // ==================== ManhDat ====================

    /** Entity → Model */
    public static ManhDatModel toModel(ManhDatEntity entity) {
        if (entity == null) return null;
        return new ManhDatModel(
            entity.getId(),
            entity.getUserId(),
            entity.getTenManhDat(),
            entity.getDiaChi(),
            entity.getDienTich(),
            entity.getDonViDienTich(),
            entity.getMoTa(),
            entity.getNgayTao(),
            entity.getSyncStatus()
        );
    }

    /** Model → Entity */
    public static ManhDatEntity toEntity(ManhDatModel model) {
        if (model == null) return null;
        return new ManhDatEntity(
            model.getId(),
            model.getUserId(),
            model.getTenManhDat(),
            model.getDiaChi(),
            model.getDienTich(),
            model.getDonViDienTich(),
            model.getMoTa(),
            model.getNgayTao(),
            model.getSyncStatus()
        );
    }

    /** List<Entity> → List<Model> */
    public static List<ManhDatModel> toModelList(List<ManhDatEntity> entities) {
        List<ManhDatModel> models = new ArrayList<>();
        if (entities == null) return models;
        for (ManhDatEntity e : entities) {
            models.add(toModel(e));
        }
        return models;
    }

    /**
     * Firestore Map → ManhDatModel.
     * Firestore trả về Map<String, Object> khi dùng DocumentSnapshot.getData().
     */
    public static ManhDatModel fromFirestoreMap(Map<String, Object> map) {
        if (map == null) return null;
        ManhDatModel model = new ManhDatModel();
        model.setId(getStr(map, "id"));
        model.setUserId(getStr(map, "userId"));
        model.setTenManhDat(getStr(map, "tenManhDat"));
        model.setDiaChi(getStr(map, "diaChi"));
        model.setDonViDienTich(getStr(map, "donViDienTich"));
        model.setMoTa(getStr(map, "moTa"));
        model.setSyncStatus("SYNCED");

        Object dienTich = map.get("dienTich");
        if (dienTich instanceof Number) {
            model.setDienTich(((Number) dienTich).doubleValue());
        }
        Object ngayTao = map.get("ngayTao");
        if (ngayTao instanceof Number) {
            model.setNgayTao(((Number) ngayTao).longValue());
        }
        return model;
    }

    // ==================== Helpers ====================

    private static String getStr(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : "";
    }
}
