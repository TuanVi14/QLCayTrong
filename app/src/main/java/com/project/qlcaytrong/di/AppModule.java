// File: app/src/main/java/com/project/qlcaytrong/di/AppModule.java
package com.project.qlcaytrong.di;

import android.content.Context;

import com.project.qlcaytrong.data.repository.AuthRepository;
import com.project.qlcaytrong.data.repository.AuthRepositoryImpl;
import com.project.qlcaytrong.data.repository.CayTrongRepository;
import com.project.qlcaytrong.data.repository.CayTrongRepositoryImpl;
import com.project.qlcaytrong.data.repository.ChiTietPhunThuocRepository;
import com.project.qlcaytrong.data.repository.ChiTietPhunThuocRepositoryImpl;
import com.project.qlcaytrong.data.repository.ChiTietTuoiPhanRepository;
import com.project.qlcaytrong.data.repository.ChiTietTuoiPhanRepositoryImpl;
import com.project.qlcaytrong.data.repository.GocCayRepository;
import com.project.qlcaytrong.data.repository.GocCayRepositoryImpl;
import com.project.qlcaytrong.data.repository.ManhDatRepository;
import com.project.qlcaytrong.data.repository.ManhDatRepositoryImpl;
import com.project.qlcaytrong.data.repository.NhatKyRepository;
import com.project.qlcaytrong.data.repository.NhatKyRepositoryImpl;

/**
 * AppModule — Manual Dependency Injection.
 *
 * Cung cấp singleton instance cho tất cả Repository.
 * ViewModel lấy repository qua AppModule thay vì khởi tạo trực tiếp.
 *
 * Tại sao không dùng Hilt/Dagger?
 * - Project quy mô vừa, không cần DI framework phức tạp
 * - Dễ hiểu cho team mới
 * - Có thể migrate sang Hilt sau này chỉ cần thêm annotation
 *
 * Cách dùng trong ViewModel:
 *   AppModule module = AppModule.getInstance(application);
 *   CayTrongRepository repo = module.getCayTrongRepository();
 */
public class AppModule {

    private static volatile AppModule INSTANCE;
    private final Context appContext;

    // Lazy-init singleton repositories
    private volatile AuthRepository              authRepository;
    private volatile ManhDatRepository           manhDatRepository;
    private volatile CayTrongRepository          cayTrongRepository;
    private volatile GocCayRepository            gocCayRepository;
    private volatile NhatKyRepository            nhatKyRepository;
    private volatile ChiTietTuoiPhanRepository   chiTietTuoiPhanRepository;
    private volatile ChiTietPhunThuocRepository  chiTietPhunThuocRepository;

    private AppModule(Context context) {
        this.appContext = context.getApplicationContext();
    }

    public static AppModule getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppModule.class) {
                if (INSTANCE == null) {
                    INSTANCE = new AppModule(context);
                }
            }
        }
        return INSTANCE;
    }

    // ==================== Repository providers ====================

    public AuthRepository getAuthRepository() {
        if (authRepository == null) {
            synchronized (this) {
                if (authRepository == null)
                    authRepository = new AuthRepositoryImpl(appContext);
            }
        }
        return authRepository;
    }

    public ManhDatRepository getManhDatRepository() {
        if (manhDatRepository == null) {
            synchronized (this) {
                if (manhDatRepository == null)
                    manhDatRepository = new ManhDatRepositoryImpl(appContext);
            }
        }
        return manhDatRepository;
    }

    public CayTrongRepository getCayTrongRepository() {
        if (cayTrongRepository == null) {
            synchronized (this) {
                if (cayTrongRepository == null)
                    cayTrongRepository = new CayTrongRepositoryImpl(appContext);
            }
        }
        return cayTrongRepository;
    }

    public GocCayRepository getGocCayRepository() {
        if (gocCayRepository == null) {
            synchronized (this) {
                if (gocCayRepository == null)
                    gocCayRepository = new GocCayRepositoryImpl(appContext);
            }
        }
        return gocCayRepository;
    }

    public NhatKyRepository getNhatKyRepository() {
        if (nhatKyRepository == null) {
            synchronized (this) {
                if (nhatKyRepository == null)
                    nhatKyRepository = new NhatKyRepositoryImpl(appContext);
            }
        }
        return nhatKyRepository;
    }

    public ChiTietTuoiPhanRepository getChiTietTuoiPhanRepository() {
        if (chiTietTuoiPhanRepository == null) {
            synchronized (this) {
                if (chiTietTuoiPhanRepository == null)
                    chiTietTuoiPhanRepository = new ChiTietTuoiPhanRepositoryImpl(appContext);
            }
        }
        return chiTietTuoiPhanRepository;
    }

    public ChiTietPhunThuocRepository getChiTietPhunThuocRepository() {
        if (chiTietPhunThuocRepository == null) {
            synchronized (this) {
                if (chiTietPhunThuocRepository == null)
                    chiTietPhunThuocRepository = new ChiTietPhunThuocRepositoryImpl(appContext);
            }
        }
        return chiTietPhunThuocRepository;
    }
}
