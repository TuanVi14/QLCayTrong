// File: app/src/main/java/com/project/qlcaytrong/viewmodel/CayTrongViewModel.java
package com.project.qlcaytrong.viewmodel;

import android.app.Application;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.project.qlcaytrong.data.repository.CayTrongRepository;
import com.project.qlcaytrong.di.AppModule;
import com.project.qlcaytrong.model.CayTrongModel;
import com.project.qlcaytrong.util.AuthResult;

import java.util.List;
import java.util.UUID;

/**
 * CayTrongViewModel — quản lý cây trồng trong một mảnh đất.
 *
 * Nhận manhDatId từ Activity qua setManhDatId() ngay sau khởi tạo.
 */
public class CayTrongViewModel extends AndroidViewModel {

    private final CayTrongRepository repository;
    private final String currentUserId;

    // LiveData danh sách cây trồng — tự cập nhật khi Room thay đổi
    private LiveData<List<CayTrongModel>> cayTrongList;
    private String currentManhDatId;

    public CayTrongViewModel(@NonNull Application application) {
        super(application);
        repository    = AppModule.getInstance(application).getCayTrongRepository();
        currentUserId = getUid();
    }

    // ==================== Khởi tạo filter ====================

    /** Gọi ngay sau khi tạo ViewModel, trước khi observe */
    public void setManhDatId(String manhDatId) {
        if (!manhDatId.equals(currentManhDatId)) {
            currentManhDatId = manhDatId;
            cayTrongList = repository.getAllByManhDatId(manhDatId);
        }
    }

    // ==================== Getters ====================

    public LiveData<List<CayTrongModel>> getCayTrongList() {
        if (cayTrongList == null) {
            // Fallback: lấy theo userId nếu chưa set manhDatId
            cayTrongList = repository.getAllByUserId(currentUserId != null ? currentUserId : "");
        }
        return cayTrongList;
    }

    public String getCurrentUserId() { return currentUserId; }

    // ==================== CRUD ====================

    public LiveData<AuthResult<CayTrongModel>> addCayTrong(
            String loaiCay, String tenKhoaHoc, int soLuong,
            String donViTinh, long ngayTrong, String trangThai, String moTa) {

        String error = validateCayTrong(loaiCay, soLuong, donViTinh);
        if (error != null) {
            MutableLiveData<AuthResult<CayTrongModel>> ld = new MutableLiveData<>();
            ld.setValue(AuthResult.error(error));
            return ld;
        }

        CayTrongModel model = new CayTrongModel(
            UUID.randomUUID().toString(),
            currentManhDatId,
            currentUserId,
            loaiCay.trim(),
            tenKhoaHoc != null ? tenKhoaHoc.trim() : "",
            soLuong,
            donViTinh.trim(),
            ngayTrong > 0 ? ngayTrong : System.currentTimeMillis(),
            trangThai != null ? trangThai : "DANG_TRONG",
            moTa != null ? moTa.trim() : "",
            "PENDING"
        );
        return repository.insert(model);
    }

    public LiveData<AuthResult<CayTrongModel>> updateCayTrong(CayTrongModel existing,
            String loaiCay, String tenKhoaHoc, int soLuong,
            String donViTinh, long ngayTrong, String trangThai, String moTa) {

        String error = validateCayTrong(loaiCay, soLuong, donViTinh);
        if (error != null) {
            MutableLiveData<AuthResult<CayTrongModel>> ld = new MutableLiveData<>();
            ld.setValue(AuthResult.error(error));
            return ld;
        }

        existing.setLoaiCay(loaiCay.trim());
        existing.setTenKhoaHoc(tenKhoaHoc != null ? tenKhoaHoc.trim() : "");
        existing.setSoLuong(soLuong);
        existing.setDonViTinh(donViTinh.trim());
        existing.setNgayTrong(ngayTrong > 0 ? ngayTrong : existing.getNgayTrong());
        existing.setTrangThai(trangThai != null ? trangThai : existing.getTrangThai());
        existing.setMoTa(moTa != null ? moTa.trim() : "");

        return repository.update(existing);
    }

    public LiveData<AuthResult<Void>> deleteCayTrong(CayTrongModel model) {
        return repository.delete(model);
    }

    public LiveData<AuthResult<Integer>> syncToCloud() {
        if (currentUserId == null) {
            MutableLiveData<AuthResult<Integer>> ld = new MutableLiveData<>();
            ld.setValue(AuthResult.error("Chưa đăng nhập."));
            return ld;
        }
        return repository.syncToFirestore(currentUserId);
    }

    public LiveData<AuthResult<Void>> syncFromCloud() {
        if (currentUserId == null) {
            MutableLiveData<AuthResult<Void>> ld = new MutableLiveData<>();
            ld.setValue(AuthResult.error("Chưa đăng nhập."));
            return ld;
        }
        return repository.syncFromFirestore(currentUserId);
    }

    // ==================== Validation ====================

    private String validateCayTrong(String loaiCay, int soLuong, String donViTinh) {
        if (TextUtils.isEmpty(loaiCay) || loaiCay.trim().isEmpty())
            return "Vui lòng nhập loại cây.";
        if (soLuong <= 0)
            return "Số lượng phải lớn hơn 0.";
        if (TextUtils.isEmpty(donViTinh))
            return "Vui lòng nhập đơn vị tính.";
        if (currentManhDatId == null || currentManhDatId.isEmpty())
            return "Không xác định được mảnh đất. Vui lòng thử lại.";
        return null;
    }

    private String getUid() {
        if (FirebaseAuth.getInstance().getCurrentUser() != null)
            return FirebaseAuth.getInstance().getCurrentUser().getUid();
        return null;
    }
}
