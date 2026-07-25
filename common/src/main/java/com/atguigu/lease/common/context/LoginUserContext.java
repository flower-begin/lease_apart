package com.atguigu.lease.common.context;

/**
 * ThreadLocal-based context holder for the current logged-in user.
 * Set by LoginInterceptor, read by controllers/services, cleared after request completion.
 */
public class LoginUserContext {

    private static final ThreadLocal<Long> USER_ID_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<String> USER_PHONE_HOLDER = new ThreadLocal<>();

    public static void setUserId(Long userId) {
        USER_ID_HOLDER.set(userId);
    }

    public static Long getUserId() {
        return USER_ID_HOLDER.get();
    }

    public static void setUserPhone(String phone) {
        USER_PHONE_HOLDER.set(phone);
    }

    public static String getUserPhone() {
        return USER_PHONE_HOLDER.get();
    }

    public static void clear() {
        USER_ID_HOLDER.remove();
        USER_PHONE_HOLDER.remove();
    }
}
