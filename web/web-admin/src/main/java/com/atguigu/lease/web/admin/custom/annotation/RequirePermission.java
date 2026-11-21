package com.atguigu.lease.web.admin.custom.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口权限声明。
 * <p>
 * 用法：
 * <pre>
 * &#64;RequirePermission("apartment:save")
 * &#64;PostMapping("saveOrUpdate")
 * public Result saveOrUpdate(...) { ... }
 * </pre>
 * 多个值表示「满足其一即可」，例如：
 * <pre>
 * &#64;RequirePermission({"system:role:save", "system:role:assign"})
 * </pre>
 * 超管（角色 code = super_admin）自动放行。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {

    String[] value();
}
