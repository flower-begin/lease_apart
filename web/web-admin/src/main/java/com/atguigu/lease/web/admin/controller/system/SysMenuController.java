package com.atguigu.lease.web.admin.controller.system;

import com.atguigu.lease.common.result.Result;
import com.atguigu.lease.web.admin.custom.annotation.RequirePermission;
import com.atguigu.lease.web.admin.service.SysMenuService;
import com.atguigu.lease.web.admin.vo.system.menu.MenuVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "后台-菜单权限")
@RestController
@RequestMapping("/admin/system/menu")
public class SysMenuController {

    @Autowired
    private SysMenuService sysMenuService;

    @Operation(summary = "全量菜单树（角色分配权限时用）")
    @GetMapping("tree")
    @RequirePermission("system:role:list")
    public Result<List<MenuVo>> tree() {
        return Result.ok(sysMenuService.tree());
    }
}
