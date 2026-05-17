// File: app/src/main/java/com/project/qlcaytrong/viewmodel/ManhDatViewModel.java
package com.project.qlcaytrong.viewmodel;

import android.app.Application;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.project.qlcaytrong.data.repository.ManhDatRepository;
import com.project.qlcaytrong.data.repository.ManhDatRepositoryImpl;
import com.project.qlcaytrong.model.ManhDatModel;
import com.project.qlcaytrong.util.AuthResult;

import java.util.List;

/**
 * ManhDatViewModel — xử lý logic UI cho ManhDat.
 * - Validate input trước khi gọi Repository
 * - Expose LiveData cho Activity observe
 * - Không giữ reference đến Context sau Application
 */
public class ManhDatViewModel extends AndroidViewModel {

    private final ManhDatRepository repository;
    private final String currentUserId;

    // Danh sách mảnh đất (Room LiveData — auto-update)
    private LiveData<List<ManhDatModel>> manhDatList;

    public ManhDatViewModel(@NonNull Application application) {
        super(application);
        repository    = new ManhDatRepositoryImpl(application);
        currentUserId = getCurrentUserId();
    }

    // ==================== GET ALL ====================

    public LiveData<List<ManhDatModel>> getManhDatList() {
        if (manhDatList == null && currentUserId != null) {
            manhDatList = repository.getAllByUserId(currentUserId);
        }
        if (manhDatList == null) {
            manhDatList = new MutableLiveData<>();
        }
        return manhDatList;
    }

    // ==================== CREATE ====================

    public LiveData<AuthResult<ManhDatModel>> createManhDat(String tenManhDat,
                                                             String diaChi,
                                                             String dienTichStr,
                                                             String donViDienTich,
                                                             String moTa) {
        String validationError = validateInput(tenManhDat, dienTichStr, donViDienTich);
        if (validationError != null) {
            MutableLiveData<AuthResult<ManhDatModel>> errorLd = new MutableLiveData<>();
            errorLd.setValue(AuthResult.error(validationError));
            return errorLd;
        }

        ManhDatModel model = new ManhDatModel();
        model.setUserId(currentUserId);
        model.setTenManhDat(tenManhDat.trim());
        model.setDiaChi(diaChi != null ? diaChi.trim() : "");
        model.setDienTich(parseDouble(dienTichStr));
        model.setDonViDienTich(donViDienTich.trim());
        model.setMoTa(moTa != null ? moTa.trim() : "");

        return repository.create(model);
    }

    // ==================== UPDATE ====================

    public LiveData<AuthResult<ManhDatModel>> updateManhDat(ManhDatModel existing,
                                                             String tenManhDat,
                                                             String diaChi,
                                                             String dienTichStr,
                                                             String donViDienTich,
                                                             String moTa) {
        String validationError = validateInput(tenManhDat, dienTichStr, donViDienTich);
        if (validationError != null) {
            MutableLiveData<AuthResult<ManhDatModel>> errorLd = new MutableLiveData<>();
            errorLd.setValue(AuthResult.error(validationError));
            return errorLd;
        }

        existing.setTenManhDat(tenManhDat.trim());
        existing.setDiaChi(diaChi != null ? diaChi.trim() : "");
        existing.setDienTich(parseDouble(dienTichStr));
        existing.setDonViDienTich(donViDienTich.trim());
        existing.setMoTa(moTa != null ? moTa.trim() : "");

        return repository.update(existing);
    }

    // ==================== DELETE ====================

    public LiveData<AuthResult<Void>> deleteManhDat(ManhDatModel model) {
        return repository.delete(model);
    }

    // ==================== SYNC ====================

    public LiveData<AuthResult<Void>> syncFromFirestore() {
        if (currentUserId == null) {
            MutableLiveData<AuthResult<Void>> errorLd = new MutableLiveData<>();
            errorLd.setValue(AuthResult.error("Chưa đăng nhập."));
            return errorLd;
        }
        return repository.syncFromFirestore(currentUserId);
    }

    // ==================== HELPERS ====================

    private String getCurrentUserId() {
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            return FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
        return null;
    }

    public String getCurrentUserIdPublic() {
        return currentUserId;
    }

    private String validateInput(String tenManhDat,
                                 String dienTichStr,
                                 String donViDienTich) {
        if (TextUtils.isEmpty(tenManhDat) || tenManhDat.trim().isEmpty()) {
            return "Vui lòng nhập tên mảnh đất.";
        }
        if (TextUtils.isEmpty(dienTichStr)) {
            return "Vui lòng nhập diện tích.";
        }
        try {
            double val = Double.parseDouble(dienTichStr.trim());
            if (val <= 0) return "Diện tích phải lớn hơn 0.";
        } catch (NumberFormatException e) {
            return "Diện tích không hợp lệ. Vui lòng nhập số.";
        }
        if (TextUtils.isEmpty(donViDienTich)) {
            return "Vui lòng chọn đơn vị diện tích.";
        }
        return null;
    }

    private double parseDouble(String str) {
        try { return Double.parseDouble(str.trim()); }
        catch (Exception e) { return 0.0; }
    }
}
