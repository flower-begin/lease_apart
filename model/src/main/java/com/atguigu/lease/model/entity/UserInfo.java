package com.atguigu.lease.model.entity;

import com.atguigu.lease.model.enums.BaseStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "用户信息表")
@TableName(value = "user_info")
@Data
public class UserInfo extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "登录账号（账号密码登录用，唯一）")
    @TableField(value = "username")
    private String username;

    @Schema(description = "手机号码")
    @TableField(value = "phone")
    private String phone;

    @Schema(description = "密码")
    @TableField(value = "password")
    private String password;

    @Schema(description = "角色：admin-管理员 tenant-租客（决定 C 端能看到哪些页面）")
    @TableField(value = "role_code")
    private String roleCode;

    @Schema(description = "头像url")
    @TableField(value = "avatar_url")
    private String avatarUrl;

    @Schema(description = "昵称")
    @TableField(value = "nickname")
    private String nickname;

    @Schema(description = "账号状态")
    @TableField(value = "status")
    private BaseStatus status;


}