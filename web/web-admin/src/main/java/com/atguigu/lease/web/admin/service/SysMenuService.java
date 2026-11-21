package com.atguigu.lease.web.admin.service;

import com.atguigu.lease.model.entity.SysMenu;
import com.atguigu.lease.web.admin.vo.system.menu.MenuVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface SysMenuService extends IService<SysMenu> {

    /**
     * 全量菜单树（给「角色分配菜单」的勾选弹窗用，超管才看得到）
     */
    List<MenuVo> tree();

    /**
     * 某个员工可见的菜单树（侧边栏渲染用）
     */
    List<MenuVo> treeByUserId(Long userId);

    /**
     * 把平铺菜单列表组装成树，父子关系按 parent_id 连
     */
    List<MenuVo> buildTree(List<SysMenu> menus);
}
