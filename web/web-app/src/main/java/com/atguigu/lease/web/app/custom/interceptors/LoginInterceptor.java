package com.atguigu.lease.web.app.custom.interceptors;

import com.atguigu.lease.common.context.LoginUserContext;
import com.atguigu.lease.common.exception.LeaseException;
import com.atguigu.lease.common.result.ResultCodeEnum;
import com.atguigu.lease.common.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader("access_token");
        if (token == null) {
            throw new LeaseException(ResultCodeEnum.APP_LOGIN_AUTH);
        }
        Claims claims = JwtUtil.parseToken(token);
        Long userId = ((Number) claims.get("userId")).longValue();
        String phone = (String) claims.get("username");
        LoginUserContext.setUserId(userId);
        LoginUserContext.setUserPhone(phone);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        LoginUserContext.clear();
    }
}
