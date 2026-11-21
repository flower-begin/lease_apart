package com.atguigu.lease.web.app.controller.login;


import com.atguigu.lease.common.context.LoginUserContext;
import com.atguigu.lease.common.exception.LeaseException;
import com.atguigu.lease.common.result.Result;
import com.atguigu.lease.common.result.ResultCodeEnum;
import com.atguigu.lease.model.entity.UserInfo;
import com.atguigu.lease.web.app.service.LoginService;
import com.atguigu.lease.web.app.service.UserInfoService;
import com.atguigu.lease.web.app.vo.user.LoginVo;
import com.atguigu.lease.web.app.vo.user.UserInfoVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "登录管理")
@RequestMapping("/app/")
public class LoginController {

    @Autowired
    private LoginService loginService;

    @Autowired
    private UserInfoService userInfoService;

    @GetMapping("login/getCode")
    @Operation(summary = "获取短信验证码")
    public Result getCode(@RequestParam String phone) throws Exception {
        loginService.sendSms(phone);
        return Result.ok();
    }

    @PostMapping("login")
    @Operation(summary = "登录（账号密码 / 短信验证码，移动端默认用账号密码）")
    public Result<String> login(@RequestBody LoginVo loginVo) {
        String token = loginService.login(loginVo);
        return Result.ok(token);
    }

    /**
     * 当前登录用户信息。用户 id 由 OptionalLogin/LoginInterceptor 写入上下文，
     * 这里不再重复解析 token，也不做 Integer 强转。
     */
    @GetMapping("info")
    @Operation(summary = "获取登录用户信息")
    public Result<UserInfoVo> info() {
        Long userId = LoginUserContext.getUserId();
        if (userId == null) {
            throw new LeaseException(ResultCodeEnum.APP_LOGIN_AUTH);
        }
        UserInfo user = userInfoService.getById(userId);
        if (user == null) {
            throw new LeaseException(ResultCodeEnum.APP_LOGIN_AUTH);
        }
        return Result.ok(new UserInfoVo(user.getNickname(), user.getAvatarUrl(), user.getPhone()));
    }
}
