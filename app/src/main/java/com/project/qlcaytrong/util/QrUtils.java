// File: app/src/main/java/com/project/qlcaytrong/util/QrUtils.java
package com.project.qlcaytrong.util;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * QrUtils — công cụ generate và lưu QR Code.
 *
 * == Cách generate QR Bitmap từ String ==
 *   1. MultiFormatWriter.encode(content, BarcodeFormat.QR_CODE, width, height, hints)
 *      → BitMatrix (ma trận bit 0/1)
 *   2. Duyệt BitMatrix → set pixel màu đen/trắng → Bitmap
 *
 * == Dependencies cần ==
 *   com.google.zxing:core:3.5.3   (generate)
 *   com.journeyapps:zxing-android-embedded:4.3.0 { isTransitive = false } (scan UI)
 *
 * == Lỗi thường gặp ==
 *   - WriterException: content rỗng → validate trước
 *   - OutOfMemoryError: size quá lớn → dùng 512x512 là đủ
 *   - FileNotFoundException khi save: thiếu WRITE_EXTERNAL_STORAGE (API < 29)
 *     hoặc chưa request runtime permission
 */
public class QrUtils {

    private static final String TAG = "QrUtils";

    /** Màu đen (modules) */
    private static final int BLACK = 0xFF000000;
    /** Màu trắng (background) */
    private static final int WHITE = 0xFFFFFFFF;

    private QrUtils() {} // Utility class — không khởi tạo

    // ==================== Generate ====================

    /**
     * Generate QR Code Bitmap từ String content.
     *
     * @param content  Nội dung encode (maQRCode của GocCay)
     * @param sizePx   Kích thước ảnh vuông (pixel). Khuyến nghị: 512
     * @return         Bitmap QR hoặc null nếu lỗi
     */
    public static Bitmap generateQrBitmap(String content, int sizePx) {
        if (content == null || content.isEmpty()) {
            Log.e(TAG, "generateQrBitmap: content is empty");
            return null;
        }

        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        // Margin (quiet zone) — 1 = tối thiểu, để QR nhỏ hơn nhưng vẫn hợp lệ
        hints.put(EncodeHintType.MARGIN, 1);
        // Error correction level M = 15% recovery (cân bằng giữa size và robustness)
        hints.put(EncodeHintType.ERROR_CORRECTION, com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.M);

        try {
            BitMatrix bitMatrix = new MultiFormatWriter().encode(
                content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints);

            int width  = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            int[] pixels = new int[width * height];

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    pixels[y * width + x] = bitMatrix.get(x, y) ? BLACK : WHITE;
                }
            }

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
            return bitmap;

        } catch (WriterException e) {
            Log.e(TAG, "generateQrBitmap: WriterException for content=" + content, e);
            return null;
        }
    }

    // ==================== Save to Gallery ====================

    /**
     * Lưu QR Bitmap vào thư viện ảnh (MediaStore).
     * Tương thích Android 9 (API 28) đến Android 14+.
     *
     * @param context   Application context
     * @param bitmap    QR Bitmap cần lưu
     * @param fileName  Tên file (VD: "QR_GC-123456.png")
     * @return          Uri của ảnh đã lưu, hoặc null nếu thất bại
     */
    public static Uri saveQrToGallery(Context context, Bitmap bitmap, String fileName) {
        if (bitmap == null) {
            Log.e(TAG, "saveQrToGallery: bitmap is null");
            return null;
        }

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName + ".png");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        values.put(MediaStore.Images.Media.WIDTH, bitmap.getWidth());
        values.put(MediaStore.Images.Media.HEIGHT, bitmap.getHeight());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // API 29+: Relative path, không cần WRITE_EXTERNAL_STORAGE
            values.put(MediaStore.Images.Media.RELATIVE_PATH,
                Environment.DIRECTORY_PICTURES + "/QLCayTrong");
            values.put(MediaStore.Images.Media.IS_PENDING, 1);
        }

        Uri uri = context.getContentResolver()
            .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        if (uri == null) {
            Log.e(TAG, "saveQrToGallery: insert returned null URI");
            return null;
        }

        try (OutputStream out = context.getContentResolver().openOutputStream(uri)) {
            if (out == null) throw new IOException("openOutputStream returned null");
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);

            // Đánh dấu IS_PENDING = 0 (file hoàn chỉnh, hiển thị được)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear();
                values.put(MediaStore.Images.Media.IS_PENDING, 0);
                context.getContentResolver().update(uri, values, null, null);
            }

            Log.d(TAG, "saveQrToGallery: saved to " + uri);
            return uri;

        } catch (IOException e) {
            Log.e(TAG, "saveQrToGallery: IOException", e);
            // Cleanup orphan record
            context.getContentResolver().delete(uri, null, null);
            return null;
        }
    }

    // ==================== Share ====================

    /**
     * Chia sẻ QR image qua Intent.
     *
     * @param context Application context
     * @param uri     Uri ảnh đã lưu (từ saveQrToGallery)
     * @param label   Tên label (VD: mã QR của gốc cây)
     * @return        Intent share (gọi startActivity(Intent.createChooser(intent, title)))
     */
    public static Intent buildShareIntent(Context context, Uri uri, String label) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("image/png");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.putExtra(Intent.EXTRA_TEXT, "QR Code: " + label);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        return intent;
    }

    // ==================== File name helper ====================

    /**
     * Sinh tên file từ QR content.
     * VD: "GC-1716000000000-A3F2" → "QR_GC_1716000000000_A3F2"
     */
    public static String buildFileName(String maQRCode) {
        if (maQRCode == null) return "QR_UNKNOWN";
        return "QR_" + maQRCode.replace("-", "_").replace("/", "_");
    }
}
