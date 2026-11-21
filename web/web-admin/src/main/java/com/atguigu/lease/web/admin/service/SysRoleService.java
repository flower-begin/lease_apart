package com.atguigu.lease.web.admin.service;

import com.atguigu.lease.model.entity.SysRole;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface SysRoleService extends IService<SysRole> {

    /**
     * 全部角色（按 sort 升序）
     */
    List<SysRole> listAll();

    /**
     * 新增 / 编辑角色（code 唯一）
     */
    void saveOrUpdateRole(SysRole role);

    /**
     * 删除角色：超级管理员角色、还挂着员工的角色都不允许删
     */
    void removeRole(Long roleId);

    /**
     * 角色已勾选的菜单 id
     */
    List<Long> menuIds(Long roleId);

    /**
     * 重新分配角色菜单（先清空再插入）
     */
    void assignMenus(Long roleId, List<Long> menuIds);

    /**
     * 员工拥有的角色
     */
    List<SysRole> rolesOfUser(Long userId);

    /**
     * 员工拥有的角色 id
     */
    List<Long> roleIdsOfUser(Long userId);

    /**
     * 重新分配员工角色（先清空再插入）
     */
    void assignRoles(Long userId, List<Long> roleIds);
}
