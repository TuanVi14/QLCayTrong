// File: app/src/main/java/com/project/qlcaytrong/data/local/NhatKyTransactionHelper.java
package com.project.qlcaytrong.data.local;

import androidx.room.Transaction;

import com.project.qlcaytrong.data.local.dao.ChiTietPhunThuocDao;
import com.project.qlcaytrong.data.local.dao.ChiTietTuoiPhanDao;
import com.project.qlcaytrong.data.local.dao.NhatKyDao;
import com.project.qlcaytrong.data.local.entity.ChiTietPhunThuocEntity;
import com.project.qlcaytrong.data.local.entity.ChiTietTuoiPhanEntity;
import com.project.qlcaytrong.data.local.entity.NhatKyEntity;

import java.util.List;

/**
 * NhatKyTransactionHelper — thực hiện Room Transaction phức tạp.
 *
 * == Tại sao cần Transaction? ==
 * Insert NhatKy + nhiều ChiTiet phải là ATOMIC:
 *   - Nếu insert NhatKy OK nhưng insert ChiTiet lỗi giữa chừng
 *     → data inconsistent (NhatKy không có ChiTiet, ChiTiet mồ côi)
 *   - Với @Transaction, nếu bất kỳ bước nào lỗi → Room ROLLBACK toàn bộ
 *
 * == Flow ==
 *   BEGIN TRANSACTION
 *     INSERT INTO nhat_ky ...
 *     INSERT INTO chi_tiet_tuoi_phan ... (x N dòng)
 *     hoặc
 *     INSERT INTO chi_tiet_phun_thuoc ... (x N dòng)
 *   COMMIT   ←  tất cả OK
 *   ROLLBACK ←  nếu bất kỳ bước nào throw exception
 *
 * == Cascade Delete ==
 *   ForeignKey(onDelete = CASCADE) đã cấu hình trong ChiTietTuoiPhanEntity
 *   → Room tự xóa ChiTiet khi NhatKy bị xóa
 *   → Không cần deleteChiTiet() thủ công khi xóa NhatKy
 *
 * == Cách dùng ==
 *   Không dùng @Dao (vì class này không phải interface),
 *   thay vào đó inject 3 DAO riêng lẻ và @Transaction đặt trên method.
 *   Room sẽ wrap toàn bộ body của method trong 1 SQLite transaction.
 */
public class NhatKyTransactionHelper {

    private final AppDatabase db;
    private final NhatKyDao nhatKyDao;
    private final ChiTietTuoiPhanDao tuoiPhanDao;
    private final ChiTietPhunThuocDao phunThuocDao;

    public NhatKyTransactionHelper(AppDatabase db) {
        this.db          = db;
        this.nhatKyDao   = db.nhatKyDao();
        this.tuoiPhanDao = db.chiTietTuoiPhanDao();
        this.phunThuocDao = db.chiTietPhunThuocDao();
    }

    /**
     * Insert NhatKy + danh sách ChiTietTuoiPhan trong 1 transaction.
     * Phải gọi trên IO thread (Room không cho phép main thread).
     */
    @Transaction
    public void insertNhatKyWithTuoiPhan(NhatKyEntity nhatKy,
                                         List<ChiTietTuoiPhanEntity> chiTietList) {
        nhatKyDao.insert(nhatKy);
        for (ChiTietTuoiPhanEntity chiTiet : chiTietList) {
            chiTiet.setNhatKyId(nhatKy.getId()); // đảm bảo FK đúng
            tuoiPhanDao.insert(chiTiet);
        }
    }

    /**
     * Insert NhatKy + danh sách ChiTietPhunThuoc trong 1 transaction.
     */
    @Transaction
    public void insertNhatKyWithPhunThuoc(NhatKyEntity nhatKy,
                                           List<ChiTietPhunThuocEntity> chiTietList) {
        nhatKyDao.insert(nhatKy);
        for (ChiTietPhunThuocEntity chiTiet : chiTietList) {
            chiTiet.setNhatKyId(nhatKy.getId());
            phunThuocDao.insert(chiTiet);
        }
    }

    /**
     * Insert NhatKy đơn giản (TINH_HINH, THU_HOACH — không có ChiTiet).
     */
    @Transaction
    public void insertNhatKySimple(NhatKyEntity nhatKy) {
        nhatKyDao.insert(nhatKy);
    }

    /**
     * Update NhatKy + xóa ChiTiet cũ + insert ChiTiet mới (TUOI_PHAN).
     *
     * Tại sao xóa rồi insert lại thay vì update?
     * → Danh sách chi tiết có thể thay đổi số lượng (thêm/xóa dòng)
     *   Đơn giản nhất là delete-and-replace.
     * → ForeignKey CASCADE giúp xóa ChiTiet khi NhatKy bị xóa, nhưng
     *   ở đây ta KHÔNG xóa NhatKy, ta chỉ xóa ChiTiet theo nhatKyId.
     */
    @Transaction
    public void updateNhatKyWithTuoiPhan(NhatKyEntity nhatKy,
                                          List<ChiTietTuoiPhanEntity> newChiTietList) {
        nhatKyDao.update(nhatKy);
        tuoiPhanDao.deleteByNhatKyId(nhatKy.getId()); // xóa cũ
        for (ChiTietTuoiPhanEntity chiTiet : newChiTietList) {
            chiTiet.setNhatKyId(nhatKy.getId());
            tuoiPhanDao.insert(chiTiet);
        }
    }

    /**
     * Update NhatKy + xóa ChiTiet cũ + insert ChiTiet mới (PHUN_THUOC).
     */
    @Transaction
    public void updateNhatKyWithPhunThuoc(NhatKyEntity nhatKy,
                                           List<ChiTietPhunThuocEntity> newChiTietList) {
        nhatKyDao.update(nhatKy);
        phunThuocDao.deleteByNhatKyId(nhatKy.getId()); // xóa cũ
        for (ChiTietPhunThuocEntity chiTiet : newChiTietList) {
            chiTiet.setNhatKyId(nhatKy.getId());
            phunThuocDao.insert(chiTiet);
        }
    }
}
