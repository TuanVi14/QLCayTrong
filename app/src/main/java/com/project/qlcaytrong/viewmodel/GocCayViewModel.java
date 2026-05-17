// File: app/src/main/java/com/project/qlcaytrong/viewmodel/GocCayViewModel.java
package com.project.qlcaytrong.viewmodel;

import android.app.Application;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.project.qlcaytrong.data.repository.GocCayRepository;
import com.project.qlcaytrong.di.AppModule;
import com.project.qlcaytrong.model.GocCayModel;
import com.project.qlcaytrong.util.AuthResult;

import java.util.List;
import java.util.UUID;

/**
 * GocCayViewModel — quản lý gốc cây trong một CayTrong.
 *
 * Đặc biệt: hỗ trợ tra cứu bằng QR Code.
 * Mỗi GocCay có 1 mã QR duy nhất được sinh khi tạo.
 */
public class GocCayViewModel extends AndroidViewModel {

    private final GocCayRepository repository;
    private final String currentUserId;

    private LiveData<List<GocCayModel>> gocCayList;
    private String currentCayTrongId;

    public GocCayViewModel(@NonNull Application application) {
        super(application);
        repository    = AppModule.getInstance(application).getGocCayRepository();
        currentUserId = getUid();
    }

    // ==================== Khởi tạo filter ====================

    public void setCayTrongId(String cayTrongId) {
        if (!cayTrongId.equals(currentCayTrongId)) {
            currentCayTrongId = cayTrongId;
            gocCayList = repository.getAllByCayTrongId(cayTrongId);
        }
    }

    // ==================== Getters ====================

    public LiveData<List<GocCayModel>> getGocCayList() {
        if (gocCayList == null) {
            gocCayList = repository.getAllByUserId(currentUserId != null ? currentUserId : "");
        }
        return gocCayList;
    }

    // ==================== QR Code lookup ====================

    /**
     * Tìm gốc cây bằng mã QR (blocking — gọi trên background thread).
     * Dùng trong QR Scanner callback.
     */
    public GocCayModel findByQRCode(String maQRCode) {
        return repository.getByQRCode(maQRCode);
    }

    // ==================== CRUD ====================

    /**
     * Thêm gốc cây mới.
     * @param maQRCode null → tự sinh UUID; không null → dùng mã được cấp (khi import)
     */
    public LiveData<AuthResult<GocCayModel>> addGocCay(
            String maQRCode, String viTri, String trangThai,
            long ngayTrong, String ghiChu) {

        String error = validateGocCay(trangThai);
        if (error != null) {
            MutableLiveData<AuthResult<GocCayModel>> ld = new MutableLiveData<>();
            ld.setValue(AuthResult.error(error));
            return ld;
        }

        // Sinh QR code nếu chưa có
        String qr = (maQRCode != null && !maQRCode.isEmpty())
            ? maQRCode
            : generateQRCode();

        GocCayModel model = new GocCayModel(
            UUID.randomUUID().toString(),
            currentCayTrongId,
            currentUserId,
            qr,
            viTri != null ? viTri.trim() : "",
            trangThai,
            ngayTrong > 0 ? ngayTrong : System.currentTimeMillis(),
            ghiChu != null ? ghiChu.trim() : "",
            "PENDING"
        );
        return repository.insert(model);
    }

    public LiveData<AuthResult<GocCayModel>> updateGocCay(GocCayModel existing,
            String viTri, String trangThai, String ghiChu) {

        existing.setViTri(viTri != null ? viTri.trim() : "");
        existing.setTrangThai(trangThai);
        existing.setGhiChu(ghiChu != null ? ghiChu.trim() : "");

        return repository.update(existing);
    }

    public LiveData<AuthResult<Void>> deleteGocCay(GocCayModel model) {
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

    // ==================== Helpers ====================

    /**
     * Sinh mã QR theo format: GC-{timestamp}-{random4chars}
     * VD: GC-1716000000000-A3F2
     */
    private String generateQRCode() {
        String uuid = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        return "GC-" + System.currentTimeMillis() + "-" + uuid.substring(0, 4);
    }

    private String validateGocCay(String trangThai) {
        if (TextUtils.isEmpty(trangThai))
            return "Vui lòng chọn trạng thái gốc cây.";
        if (currentCayTrongId == null || currentCayTrongId.isEmpty())
            return "Không xác định được cây trồng. Vui lòng thử lại.";
        return null;
    }

    private String getUid() {
        if (FirebaseAuth.getInstance().getCurrentUser() != null)
            return FirebaseAuth.getInstance().getCurrentUser().getUid();
        return null;
    }
}
