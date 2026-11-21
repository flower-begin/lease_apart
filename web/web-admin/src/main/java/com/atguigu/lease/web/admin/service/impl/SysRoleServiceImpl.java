package com.atguigu.lease.web.admin.service.impl;

import com.atguigu.lease.common.exception.LeaseException;
import com.atguigu.lease.common.result.ResultCodeEnum;
import com.atguigu.lease.model.entity.SysRole;
import com.atguigu.lease.model.enums.BaseStatus;
import com.atguigu.lease.web.admin.mapper.SysRoleMapper;
import com.atguigu.lease.web.admin.mapper.SysUserRoleMapper;
import com.atguigu.lease.web.admin.service.SysRoleService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {

    /** 超级管理员角色编码，禁止删除、禁止改编码 */
    public static final String SUPER_ADMIN = "super_admin";

    @Autowired
    private SysUserRoleMapper sysUserRoleMapper;

    @Override
    public List<SysRole> listAll() {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(SysRole::getSort);
        return this.list(wrapper);
    }

    @Override
    public void saveOrUpdateRole(SysRole role) {
        if (ObjectUtils.isEmpty(role.getCode())) {
            throw new LeaseException("角色编码不能为空", ResultCodeEnum.FAIL.getCode());
        }
        // code 唯一校验（同一个 code 只能有一条）
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRole::getCode, role.getCode());
        if (role.getId() != null) {
            wrapper.ne(SysRole::getId, role.getId());
        }
        if (this.count(wrapper) > 0) {
            throw new LeaseException("角色编码 " + role.getCode() + " 已存在", ResultCodeEnum.FAIL.getCode());
        }

        if (role.getStatus() == null) {
            role.setStatus(BaseStatus.ENABLE);
        }
        if (role.getDataScope() == null) {
            role.setDataScope("ALL");
        }
        if (role.getSort() == null) {
            role.setSort(0);
        }
        this.saveOrUpdate(role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeRole(Long roleId) {
        SysRole role = this.getById(roleId);
        if (role == null) {
            return;
        }
        if (SUPER_ADMIN.equals(role.getCode())) {
            throw new LeaseException("超级管理员角色不允许删除", ResultCodeEnum.FAIL.getCode());
        }
        int userCount = baseMapper.countUserByRoleId(roleId);
        if (userCount > 0) {
            throw new LeaseException("该角色下还有 " + userCount + " 名员工，请先取消授权", ResultCodeEnum.FAIL.getCode());
        }
        baseMapper.deleteRoleMenuByRoleId(roleId);
        this.removeById(roleId);
    }

    @Override
    public List<Long> menuIds(Long roleId) {
        return baseMapper.selectMenuIdsByRoleId(roleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignMenus(Long roleId, List<Long> menuIds) {
        if (roleId == null) {
            throw new LeaseException("角色id不能为空", ResultCodeEnum.FAIL.getCode());
        }
        baseMapper.deleteRoleMenuByRoleId(roleId);

        // 超管必须保留全部权限，不允许被改小，否则会把自己锁死
        SysRole role = this.getById(roleId);
        if (role != null && SUPER_ADMIN.equals(role.getCode())) {
            baseMapper.insertRoleMenus(roleId, baseMapper.selectAllMenuIds());
            return;
        }
        if (menuIds != null && !menuIds.isEmpty()) {
            baseMapper.insertRoleMenus(roleId, menuIds);
        }
    }

    @Override
    public List<SysRole> rolesOfUser(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        return sysUserRoleMapper.selectRolesByUserId(userId);
    }

    @Override
    public List<Long> roleIdsOfUser(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        return sysUserRoleMapper.selectRoleIdsByUserId(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(Long userId, List<Long> roleIds) {
        if (userId == null) {
            throw new LeaseException("员工id不能为空", ResultCodeEnum.FAIL.getCode());
        }
        sysUserRoleMapper.deleteByUserId(userId);
        if (roleIds != null && !roleIds.isEmpty()) {
            // 过滤掉 null，避免 <foreach> 拼出 (1, NULL)
            List<Long> valid = roleIds.stream().filter(Objects::nonNull).distinct().toList();
            if (!valid.isEmpty()) {
                sysUserRoleMapper.insertUserRoles(userId, valid);
            }
        }
    }
}
