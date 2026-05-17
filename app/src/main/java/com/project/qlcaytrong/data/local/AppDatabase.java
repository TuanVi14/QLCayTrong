// File: app/src/main/java/com/project/qlcaytrong/data/local/AppDatabase.java
package com.project.qlcaytrong.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.project.qlcaytrong.data.local.dao.CayTrongDao;
import com.project.qlcaytrong.data.local.dao.ChiTietPhunThuocDao;
import com.project.qlcaytrong.data.local.dao.ChiTietTuoiPhanDao;
import com.project.qlcaytrong.data.local.dao.GocCayDao;
import com.project.qlcaytrong.data.local.dao.ManhDatDao;
import com.project.qlcaytrong.data.local.dao.NguoiDungDao;
import com.project.qlcaytrong.data.local.dao.NhatKyDao;
import com.project.qlcaytrong.data.local.entity.CayTrongEntity;
import com.project.qlcaytrong.data.local.entity.ChiTietPhunThuocEntity;
import com.project.qlcaytrong.data.local.entity.ChiTietTuoiPhanEntity;
import com.project.qlcaytrong.data.local.entity.GocCayEntity;
import com.project.qlcaytrong.data.local.entity.ManhDatEntity;
import com.project.qlcaytrong.data.local.entity.NguoiDungEntity;
import com.project.qlcaytrong.data.local.entity.NhatKyEntity;

@Database(
    entities = {
        NguoiDungEntity.class,
        ManhDatEntity.class,
        CayTrongEntity.class,
        GocCayEntity.class,
        NhatKyEntity.class,
        ChiTietTuoiPhanEntity.class,
        ChiTietPhunThuocEntity.class
    },
    version = 1,
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static final String DATABASE_NAME = "qlcaytrong.db";

    // Volatile ensures that changes are visible to all threads immediately
    private static volatile AppDatabase INSTANCE;

    // ==================== Singleton ====================

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            DATABASE_NAME
                    ).build();
                }
            }
        }
        return INSTANCE;
    }

    // ==================== DAO Declarations ====================

    public abstract NguoiDungDao nguoiDungDao();

    public abstract ManhDatDao manhDatDao();

    public abstract CayTrongDao cayTrongDao();

    public abstract GocCayDao gocCayDao();

    public abstract NhatKyDao nhatKyDao();

    public abstract ChiTietTuoiPhanDao chiTietTuoiPhanDao();

    public abstract ChiTietPhunThuocDao chiTietPhunThuocDao();
}
