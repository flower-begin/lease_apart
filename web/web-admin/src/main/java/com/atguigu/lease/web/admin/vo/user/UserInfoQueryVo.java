package com.atguigu.lease.web.admin.vo.user;

import com.atguigu.lease.model.enums.BaseStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "用户信息查询实体")
@Data
public class UserInfoQueryVo {

    @Schema(description = "用户手机号码")
    private String phone;

    @Schema(description = "关键词：账号 / 昵称 模糊匹配")
    private String keyword;

    @Schema(description = "角色：admin-管理员 tenant-租客")
    private String roleCode;

    @Schema(description = "用户账号状态")
    private BaseStatus status;
}
