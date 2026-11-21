package com.atguigu.lease.web.app.vo.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "用户基本信息")
@Data
@NoArgsConstructor
public class UserInfoVo {

    @Schema(description = "用户昵称")
    private String nickname;

    @Schema(description = "用户头像")
    private String avatarUrl;

    @Schema(description = "手机号（脱敏前的完整号码，仅返回给本人）")
    private String phone;

    @Schema(description = "角色：admin-管理员 tenant-租客。前端据此决定是否显示「管理后台」入口")
    private String roleCode;

    /*
     * 这里不用 @AllArgsConstructor：
     * 它生成的是全字段构造器，和下面手写的 4 参构造器签名完全一样，
     * 会直接编译报错「已在类 UserInfoVo 中定义了构造器」。
     * 保留几个显式的重载，语义更清楚，也不受以后加字段的影响。
     */

    public UserInfoVo(String nickname, String avatarUrl) {
        this.nickname = nickname;
        this.avatarUrl = avatarUrl;
    }

    public UserInfoVo(String nickname, String avatarUrl, String phone) {
        this.nickname = nickname;
        this.avatarUrl = avatarUrl;
        this.phone = phone;
    }

    public UserInfoVo(String nickname, String avatarUrl, String phone, String roleCode) {
        this.nickname = nickname;
        this.avatarUrl = avatarUrl;
        this.phone = phone;
        this.roleCode = roleCode;
    }
}
