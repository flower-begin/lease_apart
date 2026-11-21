package com.atguigu.lease.web.admin.mapper;

import com.atguigu.lease.model.entity.SysMenu;
import com.atguigu.lease.model.entity.SysRole;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 员工 ↔ 角色 关联，以及「按员工查角色/权限/菜单」的联表查询。
 * 这张表没有独立实体，全部走注解 SQL。
 */
@Mapper
public interface SysUserRoleMapper {

    @Select("SELECT r.* FROM `sys_role` r " +
            "JOIN `sys_user_role` ur ON ur.`role_id` = r.`id` " +
            "WHERE ur.`user_id` = #{userId} AND r.`is_deleted` = 0 AND r.`status` = 1 " +
            "ORDER BY r.`sort`")
    List<SysRole> selectRolesByUserId(@Param("userId") Long userId);

    @Select("SELECT DISTINCT m.`perms` FROM `sys_menu` m " +
            "JOIN `sys_role_menu` rm ON rm.`menu_id` = m.`id` " +
            "JOIN `sys_user_role` ur ON ur.`role_id` = rm.`role_id` " +
            "WHERE ur.`user_id` = #{userId} " +
            "  AND m.`perms` IS NOT NULL AND m.`perms` <> '' " +
            "  AND m.`is_deleted` = 0 AND m.`status` = 1")
    List<String> selectPermsByUserId(@Param("userId") Long userId);

    @Select("SELECT DISTINCT m.* FROM `sys_menu` m " +
            "JOIN `sys_role_menu` rm ON rm.`menu_id` = m.`id` " +
            "JOIN `sys_user_role` ur ON ur.`role_id` = rm.`role_id` " +
            "WHERE ur.`user_id` = #{userId} " +
            "  AND m.`type` IN (1, 2) AND m.`visible` = 1 " +
            "  AND m.`is_deleted` = 0 AND m.`status` = 1 " +
            "ORDER BY m.`sort`")
    List<SysMenu> selectMenusByUserId(@Param("userId") Long userId);

    @Select("SELECT `role_id` FROM `sys_user_role` WHERE `user_id` = #{userId}")
    List<Long> selectRoleIdsByUserId(@Param("userId") Long userId);

    @Delete("DELETE FROM `sys_user_role` WHERE `user_id` = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);

    @Insert("<script>" +
            "INSERT IGNORE INTO `sys_user_role` (`user_id`, `role_id`) VALUES " +
            "<foreach collection='roleIds' item='roleId' separator=','>(#{userId}, #{roleId})</foreach>" +
            "</script>")
    int insertUserRoles(@Param("userId") Long userId, @Param("roleIds") List<Long> roleIds);
}
