package com.atguigu.lease.web.admin.vo.system.user;

import com.atguigu.lease.web.admin.vo.system.menu.MenuVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "员工基本信息 + 角色/权限/菜单（登录后下拉，前端按此渲染菜单和按钮）")
@Data
public class SystemUserInfoVo {

    @Schema(description = "员工id")
    private Long id;

    @Schema(description = "登录账号")
    private String username;

    @Schema(description = "用户姓名")
    private String name;

    @Schema(description = "用户头像")
    private String avatarUrl;

    @Schema(description = "角色编码列表，如 [\"super_admin\"]")
    private List<String> roles;

    @Schema(description = "权限串列表，超级管理员为 [\"*\"]")
    private List<String> perms;

    @Schema(description = "可见菜单树")
    private List<MenuVo> menus;
}
