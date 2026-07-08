package com.atguigu.lease.web.admin.custom.interceptors;

import com.atguigu.lease.common.exception.LeaseException;
import com.atguigu.lease.common.result.ResultCodeEnum;
import com.atguigu.lease.common.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.ObjectUtils;
import org.springframework.web.servlet.HandlerInterceptor;

public class LoginInterceptor implements HandlerInterceptor {
    // 重写proHandle方法
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 拦截器的实现
        // 1.首先要获取http请求头终端token  access_token
        String token = request.getHeader("access_token");
        if (ObjectUtils.isEmpty(token)) {  // 进入if后说明token不存在
            // 没有token统一返回未登录
            throw new LeaseException(ResultCodeEnum.ADMIN_LOGIN_AUTH);
        }
        // 2.如果token存在，则看token有没有过时或被篡改
        // 通过JwtUtils来判断token是否过期或被篡改
        JwtUtil.parseToken(token);
        return true;
    }
}
