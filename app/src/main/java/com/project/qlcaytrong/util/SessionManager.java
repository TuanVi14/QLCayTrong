// File: app/src/main/java/com/project/qlcaytrong/util/SessionManager.java
package com.project.qlcaytrong.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Quản lý session người dùng bằng SharedPreferences.
 * Lưu userId, email, hoTen sau khi login/register thành công.
 */
public class SessionManager {

    private static final String PREF_NAME    = "qlcaytrong_session";
    private static final String KEY_USER_ID  = "user_id";
    private static final String KEY_EMAIL    = "email";
    private static final String KEY_HO_TEN   = "ho_ten";
    private static final String KEY_IS_LOGIN = "is_login";

    private static volatile SessionManager INSTANCE;
    private final SharedPreferences prefs;
    private final SharedPreferences.Editor editor;

    private SessionManager(Context context) {
        prefs  = context.getApplicationContext()
                        .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    public static SessionManager getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (SessionManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new SessionManager(context);
                }
            }
        }
        return INSTANCE;
    }

    // ==================== Lưu session ====================

    public void saveSession(String userId, String email, String hoTen) {
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_EMAIL, email);
        editor.putString(KEY_HO_TEN, hoTen);
        editor.putBoolean(KEY_IS_LOGIN, true);
        editor.apply();
    }

    // ==================== Đọc session ====================

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGIN, false);
    }

    public String getUserId() {
        return prefs.getString(KEY_USER_ID, null);
    }

    public String getEmail() {
        return prefs.getString(KEY_EMAIL, null);
    }

    public String getHoTen() {
        return prefs.getString(KEY_HO_TEN, null);
    }

    // ==================== Xóa session (logout) ====================

    public void clearSession() {
        editor.clear().apply();
    }
}
