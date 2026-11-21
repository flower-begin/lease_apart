package com.atguigu.lease.web.app.vo.user;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "APP端登录实体")
public class LoginVo {

    @Schema(description = "登录账号（账号密码登录时必填，例如 zhangsan）")
    private String username;

    @Schema(description = "密码（账号密码登录时必填，明文，服务端用 MD5 比对）")
    private String password;

    @Schema(description = "手机号（短信验证码登录时必填）")
    private String phone;

    @Schema(description = "短信验证码（短信验证码登录时必填）")
    private String code;
}
