// File: app/src/main/java/com/project/qlcaytrong/util/AuthResult.java
package com.project.qlcaytrong.util;

/**
 * Generic wrapper để truyền trạng thái UI từ Repository → ViewModel → Activity.
 * Pattern: Loading → Success(data) | Error(message)
 */
public class AuthResult<T> {

    public enum Status { LOADING, SUCCESS, ERROR }

    public final Status status;
    public final T data;
    public final String message;

    private AuthResult(Status status, T data, String message) {
        this.status  = status;
        this.data    = data;
        this.message = message;
    }

    /** Đang xử lý — hiển thị ProgressBar */
    public static <T> AuthResult<T> loading() {
        return new AuthResult<>(Status.LOADING, null, null);
    }

    /** Thành công — data chứa kết quả (có thể null nếu void) */
    public static <T> AuthResult<T> success(T data) {
        return new AuthResult<>(Status.SUCCESS, data, null);
    }

    /** Lỗi — message chứa nội dung thông báo */
    public static <T> AuthResult<T> error(String message) {
        return new AuthResult<>(Status.ERROR, null, message);
    }

    public boolean isLoading()  { return status == Status.LOADING; }
    public boolean isSuccess()  { return status == Status.SUCCESS; }
    public boolean isError()    { return status == Status.ERROR; }
}
