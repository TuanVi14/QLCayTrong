// File: app/src/main/java/com/project/qlcaytrong/viewmodel/DashboardViewModel.java
package com.project.qlcaytrong.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.google.firebase.auth.FirebaseAuth;
import com.project.qlcaytrong.data.local.AppDatabase;
import com.project.qlcaytrong.data.local.entity.NhatKyEntity;
import com.project.qlcaytrong.model.ManhDatModel;
import com.project.qlcaytrong.model.NhatKyModel;
import com.project.qlcaytrong.sync.SyncManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * DashboardViewModel — tổng hợp số liệu cho trang chủ.
 *
 * DashboardStats tính toán:
 *  - totalManhDat: COUNT Room
 *  - totalCayTrong: COUNT đang hoạt động
 *  - nhatKyHomNay: COUNT ngày hôm nay
 *  - soGocCayBoQuen: COUNT gocCay không có NhatKy 7 ngày
 */
public class DashboardViewModel extends AndroidViewModel {

    public static class DashboardStats {
        public int totalManhDat;
        public int totalCayTrong;
        public int nhatKyHomNay;
        public int soGocCayBoQuen;
    }

    private final AppDatabase db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final String userId;
    private final MutableLiveData<DashboardStats> stats = new MutableLiveData<>();
    private final MutableLiveData<List<NhatKyModel>> recentNhatKy = new MutableLiveData<>();

    // Observe Room LiveData trực tiếp cho ManhDat list
    private final LiveData<List<ManhDatModel>> manhDatList;

    public DashboardViewModel(@NonNull Application app) {
        super(app);
        db = AppDatabase.getInstance(app);
        userId = FirebaseAuth.getInstance().getCurrentUser() != null
            ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        // ManhDat list map từ Room LiveData
        manhDatList = Transformations.map(
            db.manhDatDao().getAllByUserId(userId),
            entities -> {
                List<ManhDatModel> models = new ArrayList<>();
                if (entities != null) for (var e : entities) {
                    ManhDatModel m = new ManhDatModel();
                    m.setId(e.getId()); m.setTenManhDat(e.getTenManhDat());
                    m.setDiaChi(e.getDiaChi()); m.setDienTich(e.getDienTich());
                    m.setDonViDienTich(e.getDonViDienTich());
                    models.add(m);
                }
                return models;
            });

        loadStats();
        loadRecentNhatKy();
    }

    // ==================== Public ====================

    public LiveData<DashboardStats> getStats()          { return stats; }
    public LiveData<List<ManhDatModel>> getManhDatList() { return manhDatList; }
    public LiveData<List<NhatKyModel>> getRecentNhatKy() { return recentNhatKy; }

    /** SwipeRefresh: trigger sync + reload stats */
    public void refresh() {
        SyncManager.triggerImmediateSync(getApplication());
        loadStats();
        loadRecentNhatKy();
    }

    // ==================== Private ====================

    private void loadStats() {
        executor.execute(() -> {
            DashboardStats s = new DashboardStats();

            // COUNT mảnh đất
            List<?> md = db.manhDatDao().getAllPendingSync(); // không lý tưởng — sẽ cải thiện với COUNT query
            // Tạm: count qua Room query hết
            try {
                s.totalManhDat = android.database.DatabaseUtils.queryNumEntries(
                    db.getOpenHelper().getReadableDatabase(), "manh_dat",
                    "user_id = ?", new String[]{ userId });
                s.totalCayTrong = android.database.DatabaseUtils.queryNumEntries(
                    db.getOpenHelper().getReadableDatabase(), "cay_trong",
                    "user_id = ? AND trang_thai = 'DANG_TRONG'", new String[]{ userId });
            } catch (Exception e) {
                s.totalManhDat = 0; s.totalCayTrong = 0;
            }

            // Nhật ký hôm nay
            long startOfDay = getStartOfDay();
            s.nhatKyHomNay = countNhatKyToday(startOfDay);

            // Gốc cây bị bỏ quên (không có NhatKy 7 ngày)
            s.soGocCayBoQuen = countNeglectedGocCay();

            stats.postValue(s);
        });
    }

    private void loadRecentNhatKy() {
        executor.execute(() -> {
            // Lấy 3 nhật ký mới nhất từ Room
            try {
                var cursor = db.getOpenHelper().getReadableDatabase().query(
                    "SELECT * FROM nhat_ky WHERE user_id = ? ORDER BY ngay_thuc_hien DESC LIMIT 3",
                    new String[]{ userId });
                List<NhatKyModel> list = new ArrayList<>();
                while (cursor.moveToNext()) {
                    NhatKyModel m = new NhatKyModel();
                    m.setId(cursor.getString(cursor.getColumnIndexOrThrow("id")));
                    m.setLoaiNhatKy(cursor.getString(cursor.getColumnIndexOrThrow("loai_nhat_ky")));
                    m.setNgayThucHien(cursor.getLong(cursor.getColumnIndexOrThrow("ngay_thuc_hien")));
                    m.setNguoiThucHien(cursor.getString(cursor.getColumnIndexOrThrow("nguoi_thuc_hien")));
                    m.setHinhAnh(cursor.getString(cursor.getColumnIndexOrThrow("hinh_anh")));
                    m.setGhiChu(cursor.getString(cursor.getColumnIndexOrThrow("ghi_chu")));
                    list.add(m);
                }
                cursor.close();
                recentNhatKy.postValue(list);
            } catch (Exception e) {
                recentNhatKy.postValue(new ArrayList<>());
            }
        });
    }

    private int countNhatKyToday(long startOfDay) {
        try {
            return (int) android.database.DatabaseUtils.queryNumEntries(
                db.getOpenHelper().getReadableDatabase(), "nhat_ky",
                "user_id = ? AND ngay_thuc_hien >= ?",
                new String[]{ userId, String.valueOf(startOfDay) });
        } catch (Exception e) { return 0; }
    }

    private int countNeglectedGocCay() {
        long threshold = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7);
        try {
            // GocCay không có NhatKy nào sau threshold
            var cursor = db.getOpenHelper().getReadableDatabase().query(
                "SELECT COUNT(*) FROM goc_cay g WHERE g.user_id = ? " +
                "AND NOT EXISTS (SELECT 1 FROM nhat_ky nk " +
                "  WHERE nk.goc_cay_id = g.id AND nk.ngay_thuc_hien >= ?)",
                new String[]{ userId, String.valueOf(threshold) });
            int count = 0;
            if (cursor.moveToFirst()) count = cursor.getInt(0);
            cursor.close();
            return count;
        } catch (Exception e) { return 0; }
    }

    private long getStartOfDay() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }
}
