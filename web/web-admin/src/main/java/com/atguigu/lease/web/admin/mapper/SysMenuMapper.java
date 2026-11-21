package com.atguigu.lease.web.admin.mapper;

import com.atguigu.lease.model.entity.SysMenu;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 菜单/权限表通用 CRUD（树形组装在 SysMenuServiceImpl 里做）
 */
@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {
}
