package com.atguigu.lease.web.admin.custom.interceptors;

import com.atguigu.lease.common.exception.LeaseException;
import com.atguigu.lease.common.result.ResultCodeEnum;
import com.atguigu.lease.common.utils.JwtUtil;
import com.atguigu.lease.web.admin.custom.annotation.RequirePermission;
import com.atguigu.lease.web.admin.service.PermissionService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 接口级权限拦截器，排在 LoginInterceptor 之后（token 已经校验过存在性和合法性）。
 * <p>
 * 逻辑：方法（或所在类）上标了 @RequirePermission 才校验，
 * 没标就只要求登录。这样老接口不需要一次性全改，可以逐个加注解。
 */
@Component
public class PermissionInterceptor implements HandlerInterceptor {

    @Autowired
    private PermissionService permissionService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            // 静态资源等非控制器方法直接放行
            return true;
        }

        RequirePermission annotation = handlerMethod.getMethodAnnotation(RequirePermission.class);
        if (annotation == null) {
            annotation = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), RequirePermission.class);
        }
        if (annotation == null) {
            return true;
        }

        String token = request.getHeader("access_token");
        if (ObjectUtils.isEmpty(token)) {
            throw new LeaseException(ResultCodeEnum.ADMIN_LOGIN_AUTH);
        }
        Claims claims = JwtUtil.parseToken(token);
        Object userIdClaim = claims.get("userId");
        if (userIdClaim == null) {
            throw new LeaseException(ResultCodeEnum.ADMIN_LOGIN_AUTH);
        }
        Long userId = Long.valueOf(String.valueOf(userIdClaim));

        if (!permissionService.hasAnyPerm(userId, annotation.value())) {
            throw new LeaseException(ResultCodeEnum.ADMIN_ACCESS_FORBIDDEN);
        }
        return true;
    }
}
