package com.atguigu.lease.web.admin.service;

import com.atguigu.lease.model.entity.SysRole;
import com.atguigu.lease.web.admin.mapper.SysUserRoleMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 权限计算：给定员工 id，算出他的 角色 / 权限串集合。
 * <p>
 * 不做缓存，保证在「角色管理」里改完权限后立刻生效；
 * 菜单和权限数据量都在百行级别，直接查库的代价可以接受。
 */
@Service
public class PermissionService {

    /** 超级管理员角色编码：拥有全部权限，用 "*" 表示 */
    public static final String SUPER_ADMIN = "super_admin";

    /** 通配权限 */
    public static final String ALL_PERMISSION = "*";

    @Autowired
    private SysUserRoleMapper sysUserRoleMapper;

    public List<SysRole> rolesOf(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        return sysUserRoleMapper.selectRolesByUserId(userId);
    }

    public List<String> roleCodesOf(Long userId) {
        return rolesOf(userId).stream().map(SysRole::getCode).toList();
    }

    public boolean isSuperAdmin(Long userId) {
        return roleCodesOf(userId).contains(SUPER_ADMIN);
    }

    /**
     * 权限串集合；超级管理员返回只含 "*" 的集合
     */
    public Set<String> permsOf(Long userId) {
        if (userId == null) {
            return Collections.emptySet();
        }
        if (isSuperAdmin(userId)) {
            return Set.of(ALL_PERMISSION);
        }
        List<String> perms = sysUserRoleMapper.selectPermsByUserId(userId);
        return perms == null ? Collections.emptySet() : new HashSet<>(perms);
    }

    /**
     * 是否拥有任一权限（多对多是「满足其一即可」）
     */
    public boolean hasAnyPerm(Long userId, String... perms) {
        Set<String> owned = permsOf(userId);
        if (owned.isEmpty()) {
            return false;
        }
        if (owned.contains(ALL_PERMISSION)) {
            return true;
        }
        for (String perm : perms) {
            if (owned.contains(perm)) {
                return true;
            }
        }
        return false;
    }
}
