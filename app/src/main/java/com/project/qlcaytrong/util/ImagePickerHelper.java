// File: app/src/main/java/com/project/qlcaytrong/util/ImagePickerHelper.java
package com.project.qlcaytrong.util;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import androidx.activity.result.ActivityResultCaller;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.project.qlcaytrong.R;

import java.io.IOException;

/**
 * ImagePickerHelper — xử lý chọn ảnh từ Camera và Gallery với ActivityResultContracts.
 *
 * == Tại sao dùng ActivityResultContracts thay vì startActivityForResult? ==
 *   startActivityForResult() đã deprecated từ API 30.
 *   ActivityResultContracts:
 *   - Type-safe (không cần check requestCode trong onActivityResult)
 *   - Được khuyến khích trong Lifecycle 2.2.0+
 *   - Dễ compose và test
 *
 * == Permission flow ==
 *   Android 13+: READ_MEDIA_IMAGES (không cần READ_EXTERNAL_STORAGE)
 *   Android 6-12: READ_EXTERNAL_STORAGE (runtime permission)
 *   CAMERA: luôn cần runtime permission từ Android 6
 *
 * == FileProvider flow (Camera) ==
 *   1. createCameraImageUri() → tạo content:// URI tạm trong external cache
 *   2. Intent(ACTION_IMAGE_CAPTURE).putExtra(EXTRA_OUTPUT, uri) → Camera ghi vào đó
 *   3. onActivityResult → uri đã có ảnh → compress + upload
 *   KHÔNG dùng data.getData() từ result vì Camera không trả ảnh trong data.
 *
 * == Cách dùng ==
 *   Khởi tạo trong onCreate() (trước khi Activity started):
 *   ImagePickerHelper picker = new ImagePickerHelper(this, uri -> upload(uri));
 *   Gọi: picker.showPickerDialog();
 */
public class ImagePickerHelper {

    /** Callback trả về URI ảnh đã chọn (content:// hoặc URI từ FileProvider) */
    public interface OnImagePickedListener {
        void onImagePicked(Uri imageUri);
    }

    private final AppCompatActivity activity;
    private final OnImagePickedListener listener;

    // URI tạm của ảnh camera — lưu để biết khi result trả về
    private Uri cameraUri;

    // === Launchers — phải registerForActivityResult TRƯỚC khi Activity start ===
    private final ActivityResultLauncher<Uri> cameraLauncher;
    private final ActivityResultLauncher<String> galleryLauncher;
    private final ActivityResultLauncher<String[]> cameraPermLauncher;
    private final ActivityResultLauncher<String> galleryPermLauncher;

    public ImagePickerHelper(AppCompatActivity activity, OnImagePickedListener listener) {
        this.activity = activity;
        this.listener = listener;

        // Camera result: TakePicture contract trả về Boolean (success/fail)
        cameraLauncher = activity.registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            success -> {
                if (success && cameraUri != null) {
                    listener.onImagePicked(cameraUri);
                } else {
                    // User cancel hoặc camera fail
                    cameraUri = null;
                }
            });

        // Gallery result: GetContent contract trả về URI
        galleryLauncher = activity.registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) listener.onImagePicked(uri);
            });

        // Camera permission
        cameraPermLauncher = activity.registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> {
                Boolean granted = result.get(Manifest.permission.CAMERA);
                if (granted != null && granted) launchCamera();
                else showPermissionDeniedDialog(Manifest.permission.CAMERA);
            });

        // Gallery permission
        galleryPermLauncher = activity.registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            granted -> {
                if (granted) launchGallery();
                else showPermissionDeniedDialog(getGalleryPermission());
            });
    }

    // ==================== Public API ====================

    /** Hiển thị dialog chọn Camera / Gallery */
    public void showPickerDialog() {
        new MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.chon_nguon_anh)
            .setItems(new CharSequence[]{
                activity.getString(R.string.chup_anh_moi),
                activity.getString(R.string.chon_tu_thu_vien)
            }, (dialog, which) -> {
                if (which == 0) checkCameraAndLaunch();
                else checkGalleryAndLaunch();
            })
            .show();
    }

    // ==================== Camera ====================

    private void checkCameraAndLaunch() {
        // Kiểm tra thiết bị có camera không
        if (!activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            new MaterialAlertDialogBuilder(activity)
                .setTitle("Không có Camera")
                .setMessage("Thiết bị không có camera. Vui lòng chọn từ thư viện.")
                .setPositiveButton("OK", (d, w) -> d.dismiss()).show();
            return;
        }

        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else {
            cameraPermLauncher.launch(new String[]{ Manifest.permission.CAMERA });
        }
    }

    private void launchCamera() {
        try {
            cameraUri = ImageUtils.createCameraImageUri(activity);
            cameraLauncher.launch(cameraUri);
        } catch (IOException e) {
            new MaterialAlertDialogBuilder(activity)
                .setMessage("Không thể khởi động camera: " + e.getMessage())
                .setPositiveButton("OK", null).show();
        }
    }

    // ==================== Gallery ====================

    private void checkGalleryAndLaunch() {
        String perm = getGalleryPermission();
        if (ContextCompat.checkSelfPermission(activity, perm)
                == PackageManager.PERMISSION_GRANTED) {
            launchGallery();
        } else {
            if (activity.shouldShowRequestPermissionRationale(perm)) {
                new MaterialAlertDialogBuilder(activity)
                    .setTitle(R.string.camera_permission_title)
                    .setMessage("Ứng dụng cần quyền truy cập ảnh để tải lên nhật ký.")
                    .setPositiveButton("Cấp quyền", (d, w) -> galleryPermLauncher.launch(perm))
                    .setNegativeButton(R.string.huy, (d, w) -> d.dismiss())
                    .show();
            } else {
                galleryPermLauncher.launch(perm);
            }
        }
    }

    private void launchGallery() {
        galleryLauncher.launch("image/*");
    }

    // ==================== Permission helpers ====================

    /**
     * Permission đúng theo Android version:
     * API 33+: READ_MEDIA_IMAGES (granular media permission)
     * API 32-: READ_EXTERNAL_STORAGE (legacy)
     */
    private String getGalleryPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return Manifest.permission.READ_MEDIA_IMAGES;
        }
        return Manifest.permission.READ_EXTERNAL_STORAGE;
    }

    private void showPermissionDeniedDialog(String permission) {
        boolean isPermanentDenied = !activity.shouldShowRequestPermissionRationale(permission);
        String msg = isPermanentDenied
            ? activity.getString(R.string.camera_permission_denied_message)
            : "Cần cấp quyền để sử dụng tính năng này.";

        new MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.camera_permission_denied_title)
            .setMessage(msg)
            .setPositiveButton(isPermanentDenied ? activity.getString(R.string.mo_cai_dat) : "OK", (d, w) -> {
                if (isPermanentDenied) openAppSettings();
                else d.dismiss();
            })
            .setNegativeButton(R.string.huy, (d, w) -> d.dismiss())
            .show();
    }

    private void openAppSettings() {
        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(Uri.fromParts("package", activity.getPackageName(), null));
        activity.startActivity(intent);
    }
}
