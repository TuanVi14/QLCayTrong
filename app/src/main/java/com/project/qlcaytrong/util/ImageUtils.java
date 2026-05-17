// File: app/src/main/java/com/project/qlcaytrong/util/ImageUtils.java
package com.project.qlcaytrong.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.util.Log;

import androidx.core.content.FileProvider;
import androidx.exifinterface.media.ExifInterface;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * ImageUtils — tiện ích xử lý ảnh trước khi upload Firebase Storage.
 *
 * == Tại sao cần compress ảnh? ==
 *   Camera hiện đại chụp ảnh 8-20 MB / ảnh.
 *   Firebase Storage tính phí theo dung lượng bandwidth → tốn tiền.
 *   Màn hình app không cần full-res: 1080px là đủ để hiển thị.
 *   Compress 8MB → 200-400KB (ratio ~1:20-1:40), chất lượng vẫn tốt.
 *
 * == Compress strategy ==
 *   1. inSampleSize: decode ảnh xuống kích thước nhỏ hơn (memory efficient)
 *      Không decode full bitmap rồi mới scale → tránh OutOfMemoryError
 *   2. Bitmap.compress(JPEG, 80%): nén thêm lần nữa → ~200KB
 *   3. Rotate theo EXIF: ảnh từ camera thường bị xoay 90° hoặc 270°
 *      Nếu không handle → ảnh hiển thị ngang trong app
 *
 * == FileProvider ==
 *   Android 7+: file:// URI bị chặn giữa app (StrictMode + FileUriExposedException)
 *   FileProvider tạo content:// URI tạm thời với FLAG_GRANT_READ_URI_PERMISSION
 *   → Camera app nhận được quyền ghi vào file của ta
 *
 * == Dependencies cần ==
 *   androidx.exifinterface:exifinterface:1.3.7  (đọc EXIF rotation)
 *   (thêm vào build.gradle.kts nếu chưa có)
 */
public class ImageUtils {

    private static final String TAG = "ImageUtils";

    /** Provider authority phải khớp với AndroidManifest.xml */
    public static final String AUTHORITY = "com.project.qlcaytrong.fileprovider";

    /** Kích thước tối đa sau khi resize (pixel) */
    private static final int MAX_DIMENSION = 1080;

    /** Chất lượng nén JPEG (0-100). 80 = tốt cho upload, nhỏ dung lượng */
    private static final int JPEG_QUALITY = 80;

    private ImageUtils() {}

    // ==================== FileProvider ====================

    /**
     * Tạo file tạm trong external cache và trả về content:// URI để Camera ghi vào.
     *
     * @param context Application context
     * @return content URI cho Camera intent (ACTION_IMAGE_CAPTURE → EXTRA_OUTPUT)
     * @throws IOException nếu không tạo được file
     */
    public static Uri createCameraImageUri(Context context) throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            .format(new Date());
        String fileName = "CAMERA_" + timeStamp + ".jpg";

        // external cache không cần WRITE_EXTERNAL_STORAGE (API 29+)
        File cacheDir = context.getExternalCacheDir();
        if (cacheDir == null) cacheDir = context.getCacheDir(); // fallback
        File imageFile = new File(cacheDir, fileName);

        return FileProvider.getUriForFile(context, AUTHORITY, imageFile);
    }

    // ==================== Compress ====================

    /**
     * Đọc ảnh từ URI, resize và compress thành JPEG byte array.
     *
     * Thread: phải gọi trên BACKGROUND thread (IO-bound).
     *
     * @param context Application context
     * @param uri     URI ảnh (content://, file://)
     * @return byte[] JPEG đã compress, hoặc null nếu lỗi
     */
    public static byte[] compressImage(Context context, Uri uri) {
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
            if (inputStream == null) {
                Log.e(TAG, "compressImage: openInputStream returned null for " + uri);
                return null;
            }

            // Bước 1: Đọc kích thước gốc mà không load pixel (memory efficient)
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;

            // Cần reset stream — openInputStream mới vì đã read hết
            try (InputStream probe = context.getContentResolver().openInputStream(uri)) {
                BitmapFactory.decodeStream(probe, null, opts);
            }

            // Bước 2: Tính inSampleSize để decode xuống ~MAX_DIMENSION
            opts.inSampleSize = calculateInSampleSize(opts.outWidth, opts.outHeight);
            opts.inJustDecodeBounds = false;
            opts.inPreferredConfig = Bitmap.Config.RGB_565; // tiết kiệm 50% RAM vs ARGB_8888

            Bitmap bitmap;
            try (InputStream is = context.getContentResolver().openInputStream(uri)) {
                bitmap = BitmapFactory.decodeStream(is, null, opts);
            }
            if (bitmap == null) {
                Log.e(TAG, "compressImage: decodeStream returned null");
                return null;
            }

            // Bước 3: Rotate theo EXIF (ảnh camera thường bị xoay)
            bitmap = rotateByExif(context, uri, bitmap);

            // Bước 4: Scale thêm nếu vẫn còn lớn hơn MAX_DIMENSION
            bitmap = scaleDown(bitmap);

            // Bước 5: Compress sang JPEG
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, baos);
            bitmap.recycle();

            byte[] result = baos.toByteArray();
            Log.d(TAG, "compressImage: " + result.length / 1024 + " KB");
            return result;

        } catch (IOException e) {
            Log.e(TAG, "compressImage: IOException", e);
            return null;
        }
    }

    // ==================== Private helpers ====================

    /**
     * Tính inSampleSize để ảnh decode xuống khoảng MAX_DIMENSION.
     * inSampleSize phải là lũy thừa của 2 (Room optimizes for powers of 2).
     * inSampleSize=2 → ảnh nhỏ hơn 4 lần; =4 → nhỏ hơn 16 lần.
     */
    private static int calculateInSampleSize(int width, int height) {
        int inSampleSize = 1;
        if (height > MAX_DIMENSION || width > MAX_DIMENSION) {
            int halfH = height / 2;
            int halfW = width / 2;
            while ((halfH / inSampleSize) >= MAX_DIMENSION
                && (halfW / inSampleSize) >= MAX_DIMENSION) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    /** Scale down Bitmap nếu cả 2 chiều đều vượt MAX_DIMENSION */
    private static Bitmap scaleDown(Bitmap src) {
        int w = src.getWidth();
        int h = src.getHeight();
        if (w <= MAX_DIMENSION && h <= MAX_DIMENSION) return src;

        float scale = Math.min((float) MAX_DIMENSION / w, (float) MAX_DIMENSION / h);
        int newW = Math.round(w * scale);
        int newH = Math.round(h * scale);
        Bitmap scaled = Bitmap.createScaledBitmap(src, newW, newH, true);
        src.recycle();
        return scaled;
    }

    /**
     * Đọc EXIF orientation tag và rotate bitmap cho đúng.
     *
     * == Lỗi thường gặp ==
     * Ảnh từ camera sau-phải thường EXIF = 90° hoặc 270°.
     * Nếu không rotate → ImageView hiển thị ảnh nằm ngang dù phone đứng thẳng.
     * Glide 4.x tự handle ExifInterface cho network URLs nhưng không cho local files.
     */
    private static Bitmap rotateByExif(Context context, Uri uri, Bitmap bitmap) {
        try (InputStream is = context.getContentResolver().openInputStream(uri)) {
            if (is == null) return bitmap;
            ExifInterface exif = new ExifInterface(is);
            int orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            float degrees = 0;
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:  degrees = 90; break;
                case ExifInterface.ORIENTATION_ROTATE_180: degrees = 180; break;
                case ExifInterface.ORIENTATION_ROTATE_270: degrees = 270; break;
                default: return bitmap; // No rotation needed
            }

            Matrix matrix = new Matrix();
            matrix.postRotate(degrees);
            Bitmap rotated = Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            bitmap.recycle();
            return rotated;

        } catch (IOException e) {
            Log.w(TAG, "rotateByExif: cannot read EXIF, skip rotation");
            return bitmap;
        }
    }
}
