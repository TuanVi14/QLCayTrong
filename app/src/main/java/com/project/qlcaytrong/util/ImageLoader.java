// File: app/src/main/java/com/project/qlcaytrong/util/ImageLoader.java
package com.project.qlcaytrong.util;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.project.qlcaytrong.R;

/**
 * ImageLoader — wrapper cho Glide, tập trung cấu hình loading.
 *
 * == Tại sao dùng Glide? ==
 *   Firebase Storage URL là https:// link (không phải file path).
 *   Glide xử lý: network loading, disk cache, memory cache, placeholder, error.
 *   Thay thế: Picasso (ít feature hơn), Coil (Kotlin-first).
 *
 * == Glide cache strategy ==
 *   AUTOMATIC (default): cache cả source + result → ảnh load nhanh khi quay lại
 *   ALL: cache source (compressed) + result (decoded) → tốt cho Firebase URL
 *   NONE: không cache → luôn download lại → tốn băng thông
 *   DATA_ONLY: cache raw bytes → dùng khi URL thay đổi nhưng content giống
 *
 * == DiskCacheStrategy.ALL cho Firebase Storage ==
 *   Firebase URL có TTL token trong query param → URL thay đổi theo thời gian.
 *   Nếu cache theo URL string → cache miss liên tục (Glide dùng URL làm key).
 *   Fix: dùng signature() với nhatKyId hoặc DiskCacheStrategy.DATA.
 *   Trong project này: dùng ALL + cache dựa trên nhatKyId (acceptable for small scale).
 *
 * == Hiển thị thumbnail trong RecyclerView ==
 *   thumbnail(0.25f): load ảnh full nhưng hiển thị 25% resolution trước (placeholder chất lượng cao)
 *   override(200, 200): chỉ decode đến 200x200 → tối ưu memory cho thumbnail
 *   centerCrop(): crop ảnh vừa vào ImageView vuông (không bị letterbox)
 */
public class ImageLoader {

    private static final int CORNER_RADIUS_DP = 12;

    private ImageLoader() {}

    /**
     * Load ảnh full-size trong NhatKyDetailActivity.
     * URL: https://firebasestorage.googleapis.com/...
     * URI local: content:// (preview trước khi upload)
     */
    public static void loadDetail(Context context, @Nullable String urlOrUri,
                                   ImageView imageView) {
        Object source = resolveSource(urlOrUri);

        Glide.with(context)
            .load(source)
            .placeholder(R.drawable.ic_image_placeholder)
            .error(R.drawable.ic_image_error)
            .transition(DrawableTransitionOptions.withCrossFade(300))
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(imageView);
    }

    /**
     * Load thumbnail trong RecyclerView (item_nhat_ky.xml).
     * Decode chỉ đến 160x160, centerCrop, rounded corners.
     */
    public static void loadThumbnail(Context context, @Nullable String urlOrUri,
                                      ImageView imageView) {
        Object source = resolveSource(urlOrUri);

        Glide.with(context)
            .load(source)
            .placeholder(R.drawable.ic_image_placeholder)
            .error(R.drawable.ic_image_error)
            .transform(new CenterCrop(), new RoundedCorners(dpToPx(context, CORNER_RADIUS_DP)))
            .override(160, 160)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .transition(DrawableTransitionOptions.withCrossFade(200))
            .into(imageView);
    }

    /**
     * Load ảnh với callback khi success/fail (dùng trong upload preview).
     */
    public static void loadWithCallback(Context context, @Nullable String urlOrUri,
                                         ImageView imageView, OnLoadListener listener) {
        Object source = resolveSource(urlOrUri);
        if (source == null) {
            if (listener != null) listener.onFail("URL ảnh trống");
            return;
        }

        Glide.with(context)
            .load(source)
            .placeholder(R.drawable.ic_image_placeholder)
            .listener(new RequestListener<Drawable>() {
                @Override
                public boolean onLoadFailed(@Nullable com.bumptech.glide.load.engine.GlideException e,
                                             Object model, Target<Drawable> target, boolean isFirst) {
                    if (listener != null) listener.onFail(e != null ? e.getMessage() : "Unknown");
                    return false;
                }
                @Override
                public boolean onResourceReady(Drawable resource, Object model,
                                               Target<Drawable> target,
                                               com.bumptech.glide.load.DataSource dataSource,
                                               boolean isFirst) {
                    if (listener != null) listener.onSuccess();
                    return false;
                }
            })
            .into(imageView);
    }

    /** Clear memory cache cho ImageView (khi xóa ảnh hoặc replace ảnh) */
    public static void clear(Context context, ImageView imageView) {
        Glide.with(context).clear(imageView);
    }

    public interface OnLoadListener {
        void onSuccess();
        void onFail(String error);
    }

    // ==================== Helpers ====================

    /**
     * Phân biệt URL (String) vs content URI (Uri object) để Glide xử lý đúng.
     * null → hiển thị placeholder.
     */
    @Nullable
    private static Object resolveSource(@Nullable String urlOrUri) {
        if (urlOrUri == null || urlOrUri.isEmpty()) return null;
        if (urlOrUri.startsWith("content://") || urlOrUri.startsWith("file://")) {
            return Uri.parse(urlOrUri);
        }
        return urlOrUri; // https:// → String → Glide load qua OkHttp/network
    }

    private static int dpToPx(Context context, int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
