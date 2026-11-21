package com.atguigu.lease.web.admin.mapper;

import com.atguigu.lease.model.entity.SysRole;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    /**
     * 某个角色已勾选的菜单 id
     */
    @Select("SELECT `menu_id` FROM `sys_role_menu` WHERE `role_id` = #{roleId}")
    List<Long> selectMenuIdsByRoleId(@Param("roleId") Long roleId);

    /**
     * 清空某个角色的菜单授权（重新分配前先删）
     */
    @Delete("DELETE FROM `sys_role_menu` WHERE `role_id` = #{roleId}")
    int deleteRoleMenuByRoleId(@Param("roleId") Long roleId);

    /**
     * 批量给角色授权菜单。INSERT IGNORE 依赖 (role_id, menu_id) 唯一键，天然幂等。
     */
    @Insert("<script>" +
            "INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES " +
            "<foreach collection='menuIds' item='menuId' separator=','>(#{roleId}, #{menuId})</foreach>" +
            "</script>")
    int insertRoleMenus(@Param("roleId") Long roleId, @Param("menuIds") List<Long> menuIds);

    /**
     * 该角色下还有多少员工（删除角色前校验用）
     */
    @Select("SELECT COUNT(*) FROM `sys_user_role` WHERE `role_id` = #{roleId}")
    int countUserByRoleId(@Param("roleId") Long roleId);

    /**
     * 全部菜单 id（给超级管理员授权时用：超管永远是全量权限，不能被改小）
     */
    @Select("SELECT `id` FROM `sys_menu` WHERE `is_deleted` = 0")
    List<Long> selectAllMenuIds();
}
