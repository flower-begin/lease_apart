package com.atguigu.lease.common.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * MD5 工具类。
 * <p>
 * 为什么不直接用现成的：
 * <ul>
 *   <li>{@code org.springframework.util.DigestUtils} —— Spring Framework 6.0 起
 *       删除了 {@code md5Hex(String)} / {@code md5Digest(String)} 等旧方法，
 *       在 Spring Boot 3 项目里编译会报「找不到符号 md5Hex」。</li>
 *   <li>{@code org.apache.commons.codec.digest.DigestUtils} —— 需要 commons-codec 依赖，
 *       靠传递依赖引入不稳定。</li>
 * </ul>
 * 所以这里用 JDK 自带的 {@link MessageDigest}，零依赖。
 * <p>
 * 输出统一为 <b>32 位小写</b>十六进制字符串，与
 * {@code commons-codec} 的 {@code md5Hex} 结果一致
 * （例：{@code md5Hex("123456") == "e10adc3949ba59abbe56e057f20f883e"}）。
 */
public final class Md5Util {

    private Md5Util() {
    }

    /**
     * 计算字符串的 MD5，返回 32 位小写 hex。
     *
     * @param text 原文（按 UTF-8 编码）
     * @return 32 位小写十六进制字符串；入参为 null 时返回 null
     */
    public static String md5Hex(String text) {
        if (text == null) {
            return null;
        }
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(text.getBytes(StandardCharsets.UTF_8));
            return toHex(digest);
        } catch (NoSuchAlgorithmException e) {
            // 任何 JRE 都必须支持 MD5，走不到这里
            throw new IllegalStateException("当前 JDK 不支持 MD5 算法", e);
        }
    }

    /**
     * 校验原文与已存 MD5 是否匹配（忽略大小写）。
     *
     * @param rawText    原文
     * @param encodedHex 库里存的 MD5（大小写均可）
     */
    public static boolean matches(String rawText, String encodedHex) {
        if (rawText == null || encodedHex == null) {
            return false;
        }
        return md5Hex(rawText).equalsIgnoreCase(encodedHex);
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            String hex = Integer.toHexString(b & 0xFF);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }
}
