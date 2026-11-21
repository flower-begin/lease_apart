package com.atguigu.lease.web.admin.controller.system;

import com.atguigu.lease.common.result.Result;
import com.atguigu.lease.model.entity.SysRole;
import com.atguigu.lease.model.enums.BaseStatus;
import com.atguigu.lease.web.admin.custom.annotation.RequirePermission;
import com.atguigu.lease.web.admin.service.SysRoleService;
import com.atguigu.lease.web.admin.vo.system.role.RoleMenuVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "后台-角色管理")
@RestController
@RequestMapping("/admin/system/role")
public class SysRoleController {

    @Autowired
    private SysRoleService sysRoleService;

    @Operation(summary = "角色列表")
    @GetMapping("list")
    @RequirePermission("system:role:list")
    public Result<List<SysRole>> list() {
        return Result.ok(sysRoleService.listAll());
    }

    @Operation(summary = "新增或编辑角色")
    @PostMapping("saveOrUpdate")
    @RequirePermission("system:role:save")
    public Result saveOrUpdate(@RequestBody SysRole role) {
        sysRoleService.saveOrUpdateRole(role);
        return Result.ok();
    }

    @Operation(summary = "删除角色")
    @DeleteMapping("deleteById")
    @RequirePermission("system:role:delete")
    public Result deleteById(@RequestParam Long id) {
        sysRoleService.removeRole(id);
        return Result.ok();
    }

    @Operation(summary = "角色已勾选的菜单id")
    @GetMapping("menuIds")
    @RequirePermission("system:role:list")
    public Result<List<Long>> menuIds(@RequestParam Long id) {
        return Result.ok(sysRoleService.menuIds(id));
    }

    @Operation(summary = "给角色分配菜单权限")
    @PutMapping("assignMenu")
    @RequirePermission("system:role:assign")
    public Result assignMenu(@RequestBody RoleMenuVo roleMenuVo) {
        sysRoleService.assignMenus(roleMenuVo.getRoleId(), roleMenuVo.getMenuIds());
        return Result.ok();
    }

    @Operation(summary = "修改角色状态")
    @PostMapping("updateStatus")
    @RequirePermission("system:role:save")
    public Result updateStatus(@RequestParam Long id, @RequestParam BaseStatus status) {
        SysRole role = new SysRole();
        role.setId(id);
        role.setStatus(status);
        sysRoleService.updateById(role);
        return Result.ok();
    }
}
