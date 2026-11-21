package com.atguigu.lease.web.app.vo.user;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "APP端登录实体")
public class LoginVo {

    @Schema(description = "手机号码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String phone;

    @Schema(description = "密码（账号密码登录时必填，明文，服务端用 MD5 比对）")
    private String password;

    @Schema(description = "短信验证码（验证码登录时必填，两者传其一）")
    private String code;
}
