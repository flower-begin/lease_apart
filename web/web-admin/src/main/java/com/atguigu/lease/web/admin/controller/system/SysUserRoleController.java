package com.atguigu.lease.web.admin.controller.system;

import com.atguigu.lease.common.result.Result;
import com.atguigu.lease.model.entity.SysRole;
import com.atguigu.lease.web.admin.custom.annotation.RequirePermission;
import com.atguigu.lease.web.admin.service.SysRoleService;
import com.atguigu.lease.web.admin.vo.system.user.UserRoleVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "后台-员工角色分配")
@RestController
@RequestMapping("/admin/system/user")
public class SysUserRoleController {

    @Autowired
    private SysRoleService sysRoleService;

    @Operation(summary = "查询员工已分配的角色")
    @GetMapping("roles")
    @RequirePermission("system:user:list")
    public Result<List<SysRole>> roles(@RequestParam Long id) {
        return Result.ok(sysRoleService.rolesOfUser(id));
    }

    @Operation(summary = "查询员工已分配的角色id")
    @GetMapping("roleIds")
    @RequirePermission("system:user:list")
    public Result<List<Long>> roleIds(@RequestParam Long id) {
        return Result.ok(sysRoleService.roleIdsOfUser(id));
    }

    @Operation(summary = "给员工分配角色（先清空再插入）")
    @PutMapping("assignRole")
    @RequirePermission("system:user:assign")
    public Result assignRole(@RequestBody UserRoleVo userRoleVo) {
        sysRoleService.assignRoles(userRoleVo.getUserId(), userRoleVo.getRoleIds());
        return Result.ok();
    }
}
