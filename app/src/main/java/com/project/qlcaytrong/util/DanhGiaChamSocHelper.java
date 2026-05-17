// File: app/src/main/java/com/project/qlcaytrong/util/DanhGiaChamSocHelper.java
package com.project.qlcaytrong.util;

import com.project.qlcaytrong.data.local.entity.ChiTietPhunThuocEntity;
import com.project.qlcaytrong.data.local.entity.ChiTietTuoiPhanEntity;
import com.project.qlcaytrong.data.local.entity.GocCayEntity;
import com.project.qlcaytrong.data.local.entity.NhatKyEntity;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * DanhGiaChamSocHelper — thuật toán tính điểm chăm sóc cho GocCay.
 *
 * == Thuật toán (100 điểm tổng) ==
 *
 * THÀNH PHẦN 1 — Tình trạng cây (trangThai): 30 điểm
 *   "tot"        → 30 điểm (cây khỏe, không có vấn đề)
 *   "binh_thuong"→ 15 điểm (cây bình thường)
 *   "xau"        → 0 điểm  (cây yếu, cần chú ý ngay)
 *
 * THÀNH PHẦN 2 — Tưới phân 30 ngày gần nhất: 40 điểm
 *   Lý tưởng: ≥ 4 lần / 30 ngày (khoảng 1 lần/tuần)
 *   Điểm = min(soLanTuoiPhan, 4) / 4 * 40
 *   Ví dụ: 2 lần → 20 điểm; 4+ lần → 40 điểm tối đa
 *
 * THÀNH PHẦN 3 — Phun thuốc 30 ngày gần nhất: 30 điểm
 *   Lý tưởng: ≥ 2 lần / 30 ngày (phòng sâu bệnh)
 *   Điểm = min(soLanPhunThuoc, 2) / 2 * 30
 *   Ví dụ: 1 lần → 15 điểm; 2+ lần → 30 điểm tối đa
 *
 * CẢNH BÁO — Cây bị bỏ quên:
 *   Nếu không có nhật ký nào trong 7 ngày → isNeglected = true
 *   (Hiển thị badge cảnh báo trên Dashboard)
 *
 * == Xếp loại ==
 *   80-100: Tốt ✓ (xanh) — "Cây đang được chăm sóc tốt!"
 *   60-79:  Khá  (xanh lá nhạt) — "Duy trì tốt, cần thêm phân bón."
 *   40-59:  Trung bình (vàng) — "Cần chăm sóc thường xuyên hơn."
 *   0-39:   Kém (đỏ) — "Cây cần được chú ý ngay!"
 */
public class DanhGiaChamSocHelper {

    public static final int SCORE_MAX  = 100;
    private static final int WINDOW_DAYS = 30;
    private static final int IDEAL_TUOI_PHAN  = 4;  // lần/30 ngày
    private static final int IDEAL_PHUN_THUOC = 2;  // lần/30 ngày
    private static final int NEGLECT_DAYS = 7;

    public enum MucDo {
        TOT("Tốt", "#2E7D32"),
        KHA("Khá", "#689F38"),
        TRUNG_BINH("Trung bình", "#F57F17"),
        KEM("Kém", "#E53935");

        public final String label;
        public final String hexColor;
        MucDo(String l, String c) { this.label = l; this.hexColor = c; }
    }

    public static class KetQua {
        public final int score;         // 0-100
        public final MucDo mucDo;
        public final String loiKhuyen;
        public final boolean isNeglected; // chưa chăm sóc 7 ngày

        KetQua(int score, MucDo mucDo, String loiKhuyen, boolean neglected) {
            this.score = score;
            this.mucDo = mucDo;
            this.loiKhuyen = loiKhuyen;
            this.isNeglected = neglected;
        }
    }

    /**
     * Tính điểm chăm sóc cho 1 GocCay.
     *
     * @param gocCay    entity GocCay (cần trangThai)
     * @param nhatKyList danh sách tất cả NhatKy liên kết (đã lọc theo gocCayId)
     * @param tuoiList  danh sách ChiTietTuoiPhan của các NhatKy trên
     * @param thuocList danh sách ChiTietPhunThuoc của các NhatKy trên
     */
    public static KetQua tinh(GocCayEntity gocCay,
                               List<NhatKyEntity> nhatKyList,
                               List<ChiTietTuoiPhanEntity> tuoiList,
                               List<ChiTietPhunThuocEntity> thuocList) {

        long now = System.currentTimeMillis();
        long window30 = now - TimeUnit.DAYS.toMillis(WINDOW_DAYS);
        long window7  = now - TimeUnit.DAYS.toMillis(NEGLECT_DAYS);

        // --- Thành phần 1: trangThai (30đ) ---
        int scoreTrangThai = calcTrangThai(gocCay.getTrangThai());

        // --- Đếm NhatKy trong 30 ngày ---
        int soLanTuoiPhan  = 0;
        int soLanPhunThuoc = 0;
        long latestNhatKy  = 0;

        if (nhatKyList != null) {
            for (NhatKyEntity nk : nhatKyList) {
                if (nk.getNgayThucHien() > latestNhatKy)
                    latestNhatKy = nk.getNgayThucHien();
                if (nk.getNgayThucHien() < window30) continue;

                if ("TUOI_PHAN".equals(nk.getLoaiNhatKy()))  soLanTuoiPhan++;
                if ("PHUN_THUOC".equals(nk.getLoaiNhatKy())) soLanPhunThuoc++;
            }
        }

        // --- Thành phần 2: tưới phân (40đ) ---
        int scoreTuoiPhan = (int) (Math.min(soLanTuoiPhan, IDEAL_TUOI_PHAN)
            * 40.0 / IDEAL_TUOI_PHAN);

        // --- Thành phần 3: phun thuốc (30đ) ---
        int scorePhunThuoc = (int) (Math.min(soLanPhunThuoc, IDEAL_PHUN_THUOC)
            * 30.0 / IDEAL_PHUN_THUOC);

        int total = Math.min(scoreTrangThai + scoreTuoiPhan + scorePhunThuoc, SCORE_MAX);

        // --- Xếp loại ---
        MucDo mucDo = classify(total);

        // --- Kiểm tra bỏ quên ---
        boolean neglected = (latestNhatKy == 0) || (latestNhatKy < window7);

        // --- Lời khuyên ---
        String advice = buildAdvice(mucDo, soLanTuoiPhan, soLanPhunThuoc, neglected);

        return new KetQua(total, mucDo, advice, neglected);
    }

    // ==================== Private helpers ====================

    private static int calcTrangThai(String trangThai) {
        if (trangThai == null) return 10;
        switch (trangThai.toLowerCase()) {
            case "tot":          return 30;
            case "binh_thuong":  return 15;
            case "xau":          return 0;
            default:             return 10;
        }
    }

    private static MucDo classify(int score) {
        if (score >= 80) return MucDo.TOT;
        if (score >= 60) return MucDo.KHA;
        if (score >= 40) return MucDo.TRUNG_BINH;
        return MucDo.KEM;
    }

    private static String buildAdvice(MucDo mucDo, int tuoi, int thuoc, boolean neglected) {
        if (neglected) return "⚠ Chưa có nhật ký 7 ngày. Hãy kiểm tra cây ngay!";
        switch (mucDo) {
            case TOT:         return "✓ Cây đang được chăm sóc rất tốt!";
            case KHA:
                if (tuoi < 4) return "Tốt! Có thể tăng tần suất bón phân thêm.";
                return "Duy trì tốt, xem xét phun thuốc phòng ngừa.";
            case TRUNG_BINH:
                if (tuoi == 0) return "Cây chưa được tưới phân. Hãy bón phân ngay!";
                if (thuoc == 0) return "Chưa có lịch phun thuốc phòng sâu bệnh.";
                return "Cần chăm sóc đều đặn hơn trong 30 ngày tới.";
            case KEM:
            default:          return "⚠ Cây cần được chú ý ngay! Kiểm tra tình trạng.";
        }
    }
}
