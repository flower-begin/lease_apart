package com.atguigu.lease.common.constants;

public class RedisConstant {
    public static final String ADMIN_LOGIN_PREFIX = "admin:login:";
    public static final Integer ADMIN_LOGIN_CAPTCHA_TTL_SEC = 60;  // 验证码超时时间秒
    public static final String APP_LOGIN_PREFIX = "app:login:";
    public static final Integer APP_LOGIN_CODE_RESEND_TIME_SEC = 60;  // 重新发送验证码时间间隔秒
    public static final Integer APP_LOGIN_CODE_TTL_SEC = 60 * 10;  // 验证码超时时间秒
}