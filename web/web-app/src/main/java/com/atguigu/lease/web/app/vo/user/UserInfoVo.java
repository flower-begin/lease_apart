package com.atguigu.lease.web.app.vo.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "用户基本信息")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoVo {

    @Schema(description = "用户昵称")
    private String nickname;

    @Schema(description = "用户头像")
    private String avatarUrl;

    @Schema(description = "手机号（脱敏前的完整号码，仅返回给本人）")
    private String phone;

    public UserInfoVo(String nickname, String avatarUrl) {
        this.nickname = nickname;
        this.avatarUrl = avatarUrl;
    }
}
