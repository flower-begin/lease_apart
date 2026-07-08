package com.atguigu.lease.web.admin.controller.login;


import com.atguigu.lease.common.result.Result;
import com.atguigu.lease.common.utils.JwtUtil;
import com.atguigu.lease.model.entity.SystemUser;
import com.atguigu.lease.web.admin.service.LoginService;
import com.atguigu.lease.web.admin.service.SystemUserService;
import com.atguigu.lease.web.admin.vo.login.CaptchaVo;
import com.atguigu.lease.web.admin.vo.login.LoginVo;
import com.atguigu.lease.web.admin.vo.system.user.SystemUserInfoVo;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "后台管理系统登录管理")
@RestController
@RequestMapping("/admin")
public class LoginController {

    @Autowired
    private LoginService loginService;

    @Autowired
    private SystemUserService systemUserService;

    @Operation(summary = "获取图形验证码")
    @GetMapping("login/captcha")
    public Result<CaptchaVo> getCaptcha() {
        // 获取验证码
        CaptchaVo captchaVo = loginService.getCaptcha();
        return Result.ok(captchaVo);
    }

    @Operation(summary = "登录")
    @PostMapping("login")
    public Result<String> login(@RequestBody LoginVo loginVo) {
        // 前端传入登录信息
        // 调用业务层对应的方法
        String token = loginService.login(loginVo);
        return Result.ok();
    }

    @Operation(summary = "获取登陆用户个人信息")
    @GetMapping("info")
    // 前端需要传入请求头
    public Result<SystemUserInfoVo> info(@RequestHeader("access_token") String token) {
        // 返回的vo只要用户的头像和名字
        // 用前端传入的请求头解析
        Claims claims = JwtUtil.parseToken(token);
        // 用解析返回的对象拿到userId
        Long userId = Long.valueOf(claims.get("userId") + "");
        SystemUser systemUser = systemUserService.getById(userId);
        // 创建SystemUserInfoVo
        SystemUserInfoVo systemUserInfoVo = new SystemUserInfoVo();
        systemUserInfoVo.setName(systemUser.getName());
        systemUserInfoVo.setAvatarUrl(systemUser.getAvatarUrl());
        return Result.ok();
    }
}