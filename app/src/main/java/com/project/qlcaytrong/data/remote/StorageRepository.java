// File: app/src/main/java/com/project/qlcaytrong/data/remote/StorageRepository.java
package com.project.qlcaytrong.data.remote;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.project.qlcaytrong.util.ImageUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * StorageRepository — upload/delete ảnh lên Firebase Storage.
 *
 * == Storage path structure ==
 *   users/{userId}/nhat_ky/{nhatKyId}/{timestamp}.jpg
 *
 *   - userId: tránh user A truy cập ảnh user B (kết hợp với Storage Rules)
 *   - nhatKyId: nhóm ảnh theo nhật ký, dễ xóa bulk khi xóa NhatKy
 *   - timestamp: tránh overwrite khi cùng 1 NhatKy upload nhiều ảnh
 *
 * == Upload flow ==
 *   Main thread: gọi uploadImage() → nhận callback
 *   Background: compress → putBytes() → Firebase callback (trên main thread)
 *   putBytes() là non-blocking, callback có thể là main hoặc background tùy Firebase version
 *
 * == Retry logic ==
 *   Firebase SDK tự retry khi network tạm mất (exponential backoff).
 *   Ta implement 3-retry tùy chỉnh cho trường hợp upload thất bại hoàn toàn.
 *   Sau 3 lần fail: báo lỗi → NhatKy vẫn được lưu Room với hinhAnh = null
 *
 * == Offline handling ==
 *   Firebase Storage KHÔNG offline-capable như Firestore.
 *   Khi offline: upload ngay lập tức fail (không queue tự động).
 *   Giải pháp:
 *   1. Lưu local URI vào Room.hinhAnh tạm thời (hiển thị local)
 *   2. Khi có mạng: SyncWorker detect hinhAnh starts với "file://"/"content://"
 *      → upload lên Storage → cập nhật hinhAnh với https:// URL
 *   → Đây là offline-first pattern cho ảnh (implemented trong SyncWorker)
 *
 * == Errors thường gặp với Firebase Storage Android ==
 *   1. StorageException code -13021: PERMISSION_DENIED → kiểm tra Security Rules
 *   2. StorageException code -13013: OBJECT_NOT_FOUND → path không tồn tại
 *   3. StorageException code -13000: UNAUTHENTICATED → user chưa login, token hết hạn
 *   4. OutOfMemoryError: upload file quá lớn → compress trước (xem ImageUtils)
 *   5. "SSL error" : ngày giờ thiết bị sai → auto-fix bằng network time
 *   6. putFile() vs putBytes(): putFile() với content:// URI có thể fail
 *      trên một số ROM → dùng putBytes() với byte[] an toàn hơn
 */
public class StorageRepository {

    private static final String TAG = "StorageRepository";
    private static final int MAX_RETRY = 3;

    public interface UploadCallback {
        void onProgress(int percent);
        void onSuccess(String downloadUrl);
        void onFailure(String error);
    }

    private final Context appContext;
    private final FirebaseStorage storage;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // Singleton
    private static volatile StorageRepository INSTANCE;

    public static StorageRepository getInstance(Context context) {
        if (INSTANCE == null) synchronized (StorageRepository.class) {
            if (INSTANCE == null) INSTANCE = new StorageRepository(context.getApplicationContext());
        }
        return INSTANCE;
    }

    private StorageRepository(Context context) {
        this.appContext = context;
        this.storage = FirebaseStorage.getInstance();
    }

    // ==================== Upload ====================

    /**
     * Upload ảnh lên Firebase Storage với compress + retry.
     *
     * @param imageUri   URI ảnh (content:// từ Gallery hoặc FileProvider)
     * @param nhatKyId   ID nhật ký (dùng làm phần path)
     * @param callback   UploadCallback trả về trên main thread
     */
    public void uploadImage(Uri imageUri, String nhatKyId, UploadCallback callback) {
        String userId = getCurrentUserId();
        if (userId == null) {
            callback.onFailure("Chưa đăng nhập. Vui lòng đăng nhập lại.");
            return;
        }

        // Compress trên background thread (blocking)
        executor.execute(() -> {
            callback.onProgress(0);

            // === Bước 1: Compress (IO-bound — background) ===
            byte[] compressed = ImageUtils.compressImage(appContext, imageUri);
            if (compressed == null) {
                notifyFailure(callback, "Không thể đọc ảnh. Thử chọn ảnh khác.");
                return;
            }
            callback.onProgress(10);

            // === Bước 2: Build Storage path ===
            String fileName = System.currentTimeMillis() + ".jpg";
            String path = "users/" + userId + "/nhat_ky/" + nhatKyId + "/" + fileName;
            StorageReference ref = storage.getReference().child(path);

            Log.d(TAG, "uploadImage: uploading " + compressed.length / 1024 + "KB to " + path);

            // === Bước 3: Upload với retry ===
            uploadWithRetry(ref, compressed, 1, callback);
        });
    }

    /**
     * Recursive retry: gọi lại tối đa MAX_RETRY lần nếu upload fail.
     *
     * Tại sao không dùng loop? Firebase putBytes() là async callback-based.
     * Dùng đệ quy callback sạch hơn Thread.sleep()+loop trong executor.
     */
    private void uploadWithRetry(StorageReference ref, byte[] data,
                                  int attempt, UploadCallback callback) {
        UploadTask task = ref.putBytes(data);

        task.addOnProgressListener(snapshot -> {
            long total = snapshot.getTotalByteCount();
            long transferred = snapshot.getBytesTransferred();
            int percent = total > 0 ? (int) (10 + (transferred * 85.0 / total)) : 10;
            callback.onProgress(percent);
        });

        task.addOnSuccessListener(snapshot ->
            // Lấy download URL (https://firebasestorage.googleapis.com/...)
            ref.getDownloadUrl()
                .addOnSuccessListener(uri -> {
                    callback.onProgress(100);
                    Log.d(TAG, "uploadImage: success → " + uri.toString());
                    callback.onSuccess(uri.toString());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "getDownloadUrl failed", e);
                    notifyFailure(callback, "Upload thành công nhưng không lấy được URL.");
                })
        );

        task.addOnFailureListener(e -> {
            Log.w(TAG, "Upload attempt " + attempt + " failed: " + e.getMessage());
            if (attempt < MAX_RETRY) {
                Log.d(TAG, "Retrying... (" + attempt + "/" + MAX_RETRY + ")");
                // Chờ 1.5s trước retry (rudimentary backoff)
                try { Thread.sleep(1500L * attempt); } catch (InterruptedException ignored) {}
                uploadWithRetry(ref, data, attempt + 1, callback);
            } else {
                notifyFailure(callback, "Upload thất bại sau " + MAX_RETRY + " lần thử: "
                    + e.getMessage());
            }
        });
    }

    // ==================== Delete ====================

    /**
     * Xóa ảnh trên Storage khi NhatKy bị xóa.
     * Gọi trước khi Room.delete() nếu muốn dọn dẹp storage.
     *
     * @param downloadUrl https:// URL trả về từ getDownloadUrl()
     */
    public void deleteImage(String downloadUrl, DeleteCallback callback) {
        if (downloadUrl == null || !downloadUrl.startsWith("https://")) {
            if (callback != null) callback.onComplete(true, null);
            return;
        }
        StorageReference ref = storage.getReferenceFromUrl(downloadUrl);
        ref.delete()
            .addOnSuccessListener(v -> {
                Log.d(TAG, "deleteImage: success");
                if (callback != null) callback.onComplete(true, null);
            })
            .addOnFailureListener(e -> {
                // Không critical nếu xóa Storage fail (object mất thì thôi)
                Log.w(TAG, "deleteImage failed (non-critical): " + e.getMessage());
                if (callback != null) callback.onComplete(false, e.getMessage());
            });
    }

    public interface DeleteCallback {
        void onComplete(boolean success, String error);
    }

    // ==================== Helpers ====================

    private void notifyFailure(UploadCallback callback, String message) {
        callback.onFailure(message);
    }

    private String getCurrentUserId() {
        if (FirebaseAuth.getInstance().getCurrentUser() != null)
            return FirebaseAuth.getInstance().getCurrentUser().getUid();
        return null;
    }
}
