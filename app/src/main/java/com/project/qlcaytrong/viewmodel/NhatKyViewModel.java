// File: app/src/main/java/com/project/qlcaytrong/viewmodel/NhatKyViewModel.java
package com.project.qlcaytrong.viewmodel;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.google.firebase.auth.FirebaseAuth;
import com.project.qlcaytrong.data.local.AppDatabase;
import com.project.qlcaytrong.data.local.NhatKyTransactionHelper;
import com.project.qlcaytrong.data.local.dao.ChiTietPhunThuocDao;
import com.project.qlcaytrong.data.local.dao.ChiTietTuoiPhanDao;
import com.project.qlcaytrong.data.local.dao.NhatKyDao;
import com.project.qlcaytrong.data.local.entity.ChiTietPhunThuocEntity;
import com.project.qlcaytrong.data.local.entity.ChiTietTuoiPhanEntity;
import com.project.qlcaytrong.data.local.entity.NhatKyEntity;
import com.project.qlcaytrong.data.remote.StorageRepository;
import com.project.qlcaytrong.model.ChiTietPhunThuocModel;
import com.project.qlcaytrong.model.ChiTietTuoiPhanModel;
import com.project.qlcaytrong.model.NhatKyModel;
import com.project.qlcaytrong.model.NhatKyWithChiTiet;
import com.project.qlcaytrong.util.AuthResult;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * NhatKyViewModel — quản lý nhật ký chăm sóc.
 *
 * Đặc điểm:
 * - Dùng trực tiếp DAO thay vì qua Repository (vì cần @Transaction từ NhatKyTransactionHelper)
 * - insertWithChiTiet() là atomic: NhatKy + tất cả ChiTiet trong 1 transaction
 * - LiveData filter: byGocCay, byCayTrong, byLoai — tự động cập nhật khi Room thay đổi
 */
public class NhatKyViewModel extends AndroidViewModel {

    private final AppDatabase db;
    private final NhatKyDao nhatKyDao;
    private final ChiTietTuoiPhanDao tuoiPhanDao;
    private final ChiTietPhunThuocDao phunThuocDao;
    private final NhatKyTransactionHelper txHelper;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final String currentUserId;

    // LiveData theo context
    private LiveData<List<NhatKyModel>> currentList;
    private String filterGocCayId;
    private String filterCayTrongId;

    public NhatKyViewModel(@NonNull Application application) {
        super(application);
        db           = AppDatabase.getInstance(application);
        nhatKyDao    = db.nhatKyDao();
        tuoiPhanDao  = db.chiTietTuoiPhanDao();
        phunThuocDao = db.chiTietPhunThuocDao();
        txHelper     = new NhatKyTransactionHelper(db);
        currentUserId = getUid();
    }

    // ==================== Filter setters ====================

    public void setGocCayId(String gocCayId) {
        this.filterGocCayId = gocCayId;
        this.filterCayTrongId = null;
        currentList = Transformations.map(
            nhatKyDao.getAllByGocCayId(gocCayId), this::entitiesToModels);
    }

    public void setCayTrongId(String cayTrongId) {
        this.filterCayTrongId = cayTrongId;
        this.filterGocCayId = null;
        currentList = Transformations.map(
            nhatKyDao.getAllByCayTrongId(cayTrongId), this::entitiesToModels);
    }

    public LiveData<List<NhatKyModel>> getNhatKyList() {
        if (currentList == null) {
            currentList = Transformations.map(
                nhatKyDao.getAllByUserId(currentUserId != null ? currentUserId : ""),
                this::entitiesToModels);
        }
        return currentList;
    }

    /** Filter theo loại nhật ký trong danh sách hiện tại */
    public LiveData<List<NhatKyModel>> getByLoai(String loai) {
        return Transformations.map(
            nhatKyDao.getAllByLoaiAndUserId(loai, currentUserId != null ? currentUserId : ""),
            this::entitiesToModels);
    }

    // ==================== ChiTiet LiveData ====================

    public LiveData<List<ChiTietTuoiPhanModel>> getTuoiPhanByNhatKy(String nhatKyId) {
        return Transformations.map(
            tuoiPhanDao.getAllByNhatKyId(nhatKyId),
            entities -> {
                List<ChiTietTuoiPhanModel> list = new ArrayList<>();
                if (entities != null) for (ChiTietTuoiPhanEntity e : entities)
                    list.add(toTuoiPhanModel(e));
                return list;
            });
    }

    public LiveData<List<ChiTietPhunThuocModel>> getPhunThuocByNhatKy(String nhatKyId) {
        return Transformations.map(
            phunThuocDao.getAllByNhatKyId(nhatKyId),
            entities -> {
                List<ChiTietPhunThuocModel> list = new ArrayList<>();
                if (entities != null) for (ChiTietPhunThuocEntity e : entities)
                    list.add(toPhunThuocModel(e));
                return list;
            });
    }

    // ==================== CRUD ====================

    /**
     * Insert NhatKy + danh sách ChiTiet trong 1 Room Transaction.
     *
     * @param loai      TUOI_PHAN, PHUN_THUOC, TINH_HINH, THU_HOACH
     * @param tuoiList  danh sách phân (null hoặc rỗng nếu loai != TUOI_PHAN)
     * @param thuocList danh sách thuốc (null hoặc rỗng nếu loai != PHUN_THUOC)
     */
    public LiveData<AuthResult<NhatKyModel>> insertWithChiTiet(
            String loai, String gocCayId, String cayTrongId,
            long ngayThucHien, String nguoiThucHien, String ghiChu,
            List<ChiTietTuoiPhanModel> tuoiList,
            List<ChiTietPhunThuocModel> thuocList) {

        MutableLiveData<AuthResult<NhatKyModel>> result = new MutableLiveData<>();

        // Validate
        String error = validateInput(loai, ngayThucHien, tuoiList, thuocList);
        if (error != null) {
            result.setValue(AuthResult.error(error));
            return result;
        }

        result.setValue(AuthResult.loading());
        executor.execute(() -> {
            try {
                String nhatKyId = UUID.randomUUID().toString();
                NhatKyEntity nhatKy = new NhatKyEntity(
                    nhatKyId, gocCayId, cayTrongId,
                    currentUserId, loai, ngayThucHien,
                    nguoiThucHien, null, ghiChu, "PENDING"
                );
                NhatKyModel model = toModel(nhatKy);

                switch (loai) {
                    case NhatKyEntity.LOAI_TUOI_PHAN:
                        txHelper.insertNhatKyWithTuoiPhan(nhatKy,
                            buildTuoiPhanEntities(tuoiList, nhatKyId));
                        break;
                    case NhatKyEntity.LOAI_PHUN_THUOC:
                        txHelper.insertNhatKyWithPhunThuoc(nhatKy,
                            buildPhunThuocEntities(thuocList, nhatKyId));
                        break;
                    default:
                        txHelper.insertNhatKySimple(nhatKy);
                }
                result.postValue(AuthResult.success(model));
            } catch (Exception e) {
                result.postValue(AuthResult.error("Lỗi lưu nhật ký: " + e.getMessage()));
            }
        });
        return result;
    }

    /**
     * Update NhatKy + ChiTiet (delete-and-replace ChiTiet cũ).
     */
    public LiveData<AuthResult<NhatKyModel>> updateWithChiTiet(
            NhatKyModel existing, String loai,
            long ngayThucHien, String nguoiThucHien, String ghiChu,
            List<ChiTietTuoiPhanModel> tuoiList,
            List<ChiTietPhunThuocModel> thuocList) {

        MutableLiveData<AuthResult<NhatKyModel>> result = new MutableLiveData<>();
        String error = validateInput(loai, ngayThucHien, tuoiList, thuocList);
        if (error != null) { result.setValue(AuthResult.error(error)); return result; }

        result.setValue(AuthResult.loading());
        executor.execute(() -> {
            try {
                existing.setLoaiNhatKy(loai);
                existing.setNgayThucHien(ngayThucHien);
                existing.setNguoiThucHien(nguoiThucHien);
                existing.setGhiChu(ghiChu);
                existing.setSyncStatus("PENDING");
                NhatKyEntity entity = toEntity(existing);

                switch (loai) {
                    case NhatKyEntity.LOAI_TUOI_PHAN:
                        txHelper.updateNhatKyWithTuoiPhan(entity,
                            buildTuoiPhanEntities(tuoiList, existing.getId()));
                        break;
                    case NhatKyEntity.LOAI_PHUN_THUOC:
                        txHelper.updateNhatKyWithPhunThuoc(entity,
                            buildPhunThuocEntities(thuocList, existing.getId()));
                        break;
                    default:
                        nhatKyDao.update(entity);
                }
                result.postValue(AuthResult.success(existing));
            } catch (Exception e) {
                result.postValue(AuthResult.error("Lỗi cập nhật: " + e.getMessage()));
            }
        });
        return result;
    }

    public LiveData<AuthResult<Void>> deleteNhatKy(NhatKyModel model) {
        MutableLiveData<AuthResult<Void>> result = new MutableLiveData<>();
        result.setValue(AuthResult.loading());
        executor.execute(() -> {
            try {
                // CASCADE DELETE tự xóa ChiTiet liên quan
                nhatKyDao.delete(toEntity(model));
                result.postValue(AuthResult.success(null));
            } catch (Exception e) {
                result.postValue(AuthResult.error("Xóa thất bại: " + e.getMessage()));
            }
        });
        return result;
    }

    public String getCurrentUserId() { return currentUserId; }
    public String getFilterGocCayId() { return filterGocCayId; }
    public String getFilterCayTrongId() { return filterCayTrongId; }

    // ==================== Image Upload ====================

    /**
     * Upload ảnh lên Firebase Storage, sau đó cập nhật Room.hinhAnh với URL.
     *
     * Flow:
     *   1. StorageRepository.uploadImage() compress + upload (background)
     *   2. onSuccess: updateHinhAnh() → Room.update() trên IO thread
     *   3. Cập nhật Room trước, Firestore qua SyncWorker sau
     *
     * Offline: URL = null → lưu local URI tạm → SyncWorker upload khi có mạng
     *
     * @param imageUri   URI từ Gallery hoặc Camera FileProvider
     * @param nhatKyId   ID nhật ký cần gắn ảnh
     * @return LiveData<AuthResult> với percent (loading) / URL (success) / error
     */
    public MutableLiveData<UploadState> uploadImage(Uri imageUri, String nhatKyId) {
        MutableLiveData<UploadState> liveData = new MutableLiveData<>();
        liveData.postValue(UploadState.loading(0));

        StorageRepository.getInstance(getApplication())
            .uploadImage(imageUri, nhatKyId, new StorageRepository.UploadCallback() {
                @Override
                public void onProgress(int percent) {
                    liveData.postValue(UploadState.loading(percent));
                }

                @Override
                public void onSuccess(String downloadUrl) {
                    // Cập nhật Room với URL mới trên IO thread
                    executor.execute(() -> {
                        try {
                            NhatKyEntity entity = nhatKyDao.getById(nhatKyId);
                            if (entity != null) {
                                entity.setHinhAnh(downloadUrl);
                                entity.setSyncStatus("PENDING"); // Sync Firestore sau
                                nhatKyDao.update(entity);
                            }
                        } catch (Exception e) {
                            // URL đã có trong Storage, Room update fail — log warning
                            android.util.Log.w("NhatKyVM", "updateHinhAnh fail: " + e.getMessage());
                        }
                    });
                    liveData.postValue(UploadState.success(downloadUrl));
                }

                @Override
                public void onFailure(String error) {
                    liveData.postValue(UploadState.error(error));
                }
            });
        return liveData;
    }

    /**
     * UploadState — state object cho upload progress + result.
     * Khác với AuthResult: có thêm percent field cho ProgressBar.
     */
    public static class UploadState {
        public enum Status { LOADING, SUCCESS, ERROR }

        public final Status status;
        public final int percent;   // 0-100 khi LOADING
        public final String url;    // https:// URL khi SUCCESS
        public final String error;  // error message khi ERROR

        private UploadState(Status s, int p, String url, String err) {
            this.status = s; this.percent = p; this.url = url; this.error = err;
        }
        public static UploadState loading(int p) { return new UploadState(Status.LOADING, p, null, null); }
        public static UploadState success(String url) { return new UploadState(Status.SUCCESS, 100, url, null); }
        public static UploadState error(String err) { return new UploadState(Status.ERROR, 0, null, err); }

        public boolean isLoading() { return status == Status.LOADING; }
        public boolean isSuccess() { return status == Status.SUCCESS; }
        public boolean isError()   { return status == Status.ERROR; }
    }


    private String validateInput(String loai, long ngay,
                                  List<ChiTietTuoiPhanModel> tuoi,
                                  List<ChiTietPhunThuocModel> thuoc) {
        if (loai == null || loai.isEmpty())
            return "Vui lòng chọn loại nhật ký.";
        if (ngay <= 0)
            return "Ngày thực hiện không hợp lệ.";
        if (NhatKyEntity.LOAI_TUOI_PHAN.equals(loai) && (tuoi == null || tuoi.isEmpty()))
            return "Vui lòng thêm ít nhất 1 chi tiết phân bón.";
        if (NhatKyEntity.LOAI_PHUN_THUOC.equals(loai) && (thuoc == null || thuoc.isEmpty()))
            return "Vui lòng thêm ít nhất 1 chi tiết thuốc.";
        return null;
    }

    // ==================== Entity builders ====================

    private List<ChiTietTuoiPhanEntity> buildTuoiPhanEntities(
            List<ChiTietTuoiPhanModel> models, String nhatKyId) {
        List<ChiTietTuoiPhanEntity> list = new ArrayList<>();
        if (models == null) return list;
        for (ChiTietTuoiPhanModel m : models) {
            if (m.getTenPhan() == null || m.getTenPhan().isEmpty()) continue; // skip empty rows
            list.add(new ChiTietTuoiPhanEntity(
                UUID.randomUUID().toString(), nhatKyId, currentUserId,
                m.getTenPhan(), m.getLieuLuong(), m.getDonVi(), m.getCachBon(), "PENDING"));
        }
        return list;
    }

    private List<ChiTietPhunThuocEntity> buildPhunThuocEntities(
            List<ChiTietPhunThuocModel> models, String nhatKyId) {
        List<ChiTietPhunThuocEntity> list = new ArrayList<>();
        if (models == null) return list;
        for (ChiTietPhunThuocModel m : models) {
            if (m.getTenThuoc() == null || m.getTenThuoc().isEmpty()) continue;
            list.add(new ChiTietPhunThuocEntity(
                UUID.randomUUID().toString(), nhatKyId, currentUserId,
                m.getTenThuoc(), m.getLieuLuong(), m.getDonVi(), m.getLyDoPhun(), "PENDING"));
        }
        return list;
    }

    // ==================== Mappers ====================

    private List<NhatKyModel> entitiesToModels(List<NhatKyEntity> entities) {
        List<NhatKyModel> list = new ArrayList<>();
        if (entities != null) for (NhatKyEntity e : entities) list.add(toModel(e));
        return list;
    }

    private NhatKyModel toModel(NhatKyEntity e) {
        return new NhatKyModel(e.getId(), e.getGocCayId(), e.getCayTrongId(),
            e.getUserId(), e.getLoaiNhatKy(), e.getNgayThucHien(),
            e.getNguoiThucHien(), e.getHinhAnh(), e.getGhiChu(), e.getSyncStatus());
    }

    private NhatKyEntity toEntity(NhatKyModel m) {
        return new NhatKyEntity(m.getId(), m.getGocCayId(), m.getCayTrongId(),
            m.getUserId(), m.getLoaiNhatKy(), m.getNgayThucHien(),
            m.getNguoiThucHien(), m.getHinhAnh(), m.getGhiChu(), m.getSyncStatus());
    }

    private ChiTietTuoiPhanModel toTuoiPhanModel(ChiTietTuoiPhanEntity e) {
        return new ChiTietTuoiPhanModel(e.getId(), e.getNhatKyId(), e.getUserId(),
            e.getTenPhan(), e.getLieuLuong(), e.getDonVi(), e.getCachBon(), e.getSyncStatus());
    }

    private ChiTietPhunThuocModel toPhunThuocModel(ChiTietPhunThuocEntity e) {
        return new ChiTietPhunThuocModel(e.getId(), e.getNhatKyId(), e.getUserId(),
            e.getTenThuoc(), e.getLieuLuong(), e.getDonVi(), e.getLyDoPhun(), e.getSyncStatus());
    }

    private String getUid() {
        if (FirebaseAuth.getInstance().getCurrentUser() != null)
            return FirebaseAuth.getInstance().getCurrentUser().getUid();
        return null;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }
}
