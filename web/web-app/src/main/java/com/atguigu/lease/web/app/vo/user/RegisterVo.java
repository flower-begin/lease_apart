package com.atguigu.lease.web.app.vo.user;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "APP端注册实体")
public class RegisterVo {

    @Schema(description = "登录账号，3~20 位字母、数字或下划线", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @Schema(description = "密码，6~20 位", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @Schema(description = "手机号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String phone;

    @Schema(description = "昵称，不填默认用账号")
    private String nickname;
}
