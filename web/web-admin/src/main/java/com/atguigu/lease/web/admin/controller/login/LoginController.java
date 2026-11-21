package com.atguigu.lease.web.admin.controller.login;


import com.atguigu.lease.common.exception.LeaseException;
import com.atguigu.lease.common.result.Result;
import com.atguigu.lease.common.result.ResultCodeEnum;
import com.atguigu.lease.common.utils.JwtUtil;
import com.atguigu.lease.model.entity.SystemUser;
import com.atguigu.lease.model.enums.SystemUserType;
import com.atguigu.lease.web.admin.service.LoginService;
import com.atguigu.lease.web.admin.service.PermissionService;
import com.atguigu.lease.web.admin.service.SysMenuService;
import com.atguigu.lease.web.admin.service.SystemUserService;
import com.atguigu.lease.web.admin.vo.login.CaptchaVo;
import com.atguigu.lease.web.admin.vo.login.LoginVo;
import com.atguigu.lease.web.admin.vo.system.menu.MenuVo;
import com.atguigu.lease.web.admin.vo.system.user.SystemUserInfoVo;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Tag(name = "后台管理系统登录管理")
@RestController
@RequestMapping("/admin")
public class LoginController {

    @Autowired
    private LoginService loginService;

    @Autowired
    private SystemUserService systemUserService;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private SysMenuService sysMenuService;

    @Operation(summary = "获取图形验证码")
    @GetMapping("login/captcha")
    public Result<CaptchaVo> getCaptcha() {
        CaptchaVo captchaVo = loginService.getCaptcha();
        return Result.ok(captchaVo);
    }

    @Operation(summary = "登录")
    @PostMapping("login")
    public Result<String> login(@RequestBody LoginVo loginVo) {
        String token = loginService.login(loginVo);
        // 必须把 token 返回给前端，否则前端拿不到登录态
        return Result.ok(token);
    }

    @Operation(summary = "获取登陆用户个人信息（含角色/权限/菜单）")
    @GetMapping("info")
    public Result<SystemUserInfoVo> info(@RequestHeader("access_token") String token) {
        Claims claims = JwtUtil.parseToken(token);
        Object userIdClaim = claims.get("userId");
        if (userIdClaim == null) {
            throw new LeaseException(ResultCodeEnum.ADMIN_LOGIN_AUTH);
        }
        Long userId = Long.valueOf(String.valueOf(userIdClaim));
        SystemUser systemUser = systemUserService.getById(userId);
        if (systemUser == null) {
            throw new LeaseException(ResultCodeEnum.ADMIN_ACCOUNT_NOT_EXIST_ERROR);
        }

        SystemUserInfoVo systemUserInfoVo = new SystemUserInfoVo();
        systemUserInfoVo.setId(systemUser.getId());
        systemUserInfoVo.setUsername(systemUser.getUsername());
        systemUserInfoVo.setName(systemUser.getName());
        systemUserInfoVo.setAvatarUrl(systemUser.getAvatarUrl());

        // 角色 / 权限 / 菜单：RBAC 三件套，前端全靠这三个字段
        boolean superAdmin = permissionService.isSuperAdmin(userId);

        // 兜底：没有分配任何角色，但他是 system_user.type = 0（管理员）时，
        // 说明 RBAC 种子数据没跑全。此时直接给全量权限，否则管理员会被自己的权限系统
        // 锁在 403 页面里，连「角色管理」都进不去，只能改库解救。
        if (!superAdmin && permissionService.rolesOf(userId).isEmpty()
                && systemUser.getType() == SystemUserType.ADMIN) {
            log.warn("员工 [{}] 没有分配任何角色，按 system_user.type=0 降级为超级管理员；" +
                    "建议执行 sql/upgrade-02-rbac.sql 补全角色数据", systemUser.getUsername());
            superAdmin = true;
        }

        if (superAdmin) {
            systemUserInfoVo.setRoles(List.of(PermissionService.SUPER_ADMIN));
            systemUserInfoVo.setPerms(List.of(PermissionService.ALL_PERMISSION));
            // 超管直接给全量菜单树，避免以后新增菜单忘了给超管授权导致菜单缺失
            systemUserInfoVo.setMenus(sysMenuService.tree());
        } else {
            systemUserInfoVo.setRoles(permissionService.roleCodesOf(userId));
            systemUserInfoVo.setPerms(new ArrayList<>(permissionService.permsOf(userId)));
            systemUserInfoVo.setMenus(sysMenuService.treeByUserId(userId));
        }

        return Result.ok(systemUserInfoVo);
    }
}
