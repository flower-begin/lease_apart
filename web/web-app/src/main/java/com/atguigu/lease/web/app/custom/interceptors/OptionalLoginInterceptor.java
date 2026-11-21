package com.atguigu.lease.web.app.custom.interceptors;

import com.atguigu.lease.common.context.LoginUserContext;
import com.atguigu.lease.common.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 可选登录拦截器。
 * <p>
 * 和 {@link LoginInterceptor} 的区别：没有 token / token 失效时<b>不拦</b>，直接放行，
 * 只不过 {@link LoginUserContext} 里是空的。用于房间详情、公寓详情这类"匿名也能看，
 * 登录了要多做点事（比如记浏览历史）"的接口。
 */
@Slf4j
public class OptionalLoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String token = request.getHeader("access_token");
        if (token == null || token.isEmpty()) {
            return true;
        }
        try {
            Claims claims = JwtUtil.parseToken(token);
            Object userId = claims.get("userId");
            if (userId instanceof Number number) {
                LoginUserContext.setUserId(number.longValue());
            }
            Object username = claims.get("username");
            if (username != null) {
                LoginUserContext.setUserPhone(String.valueOf(username));
            }
        } catch (Exception e) {
            // token 过期或不合法：当作匿名处理，不打断请求
            log.debug("optional login ignored: {}", e.getMessage());
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        LoginUserContext.clear();
    }
}
