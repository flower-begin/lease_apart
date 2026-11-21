package com.atguigu.lease.model.entity;

import com.atguigu.lease.model.enums.BaseStatus;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 菜单/权限表 sys_menu
 * <p>
 * type：1-目录 2-菜单 3-按钮；perms 是接口鉴权用的权限串
 */
@TableName(value = "sys_menu")
@Data
public class SysMenu extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "父级id，0 为顶级")
    @TableField(value = "parent_id")
    private Long parentId;

    @Schema(description = "菜单名称")
    @TableField(value = "name")
    private String name;

    @Schema(description = "类型：1-目录 2-菜单 3-按钮")
    @TableField(value = "type")
    private Integer type;

    @Schema(description = "前端路由路径")
    @TableField(value = "path")
    private String path;

    @Schema(description = "前端组件路径")
    @TableField(value = "component")
    private String component;

    @Schema(description = "Element Plus 图标名")
    @TableField(value = "icon")
    private String icon;

    @Schema(description = "权限标识，如 apartment:save")
    @TableField(value = "perms")
    private String perms;

    @Schema(description = "排序")
    @TableField(value = "sort")
    private Integer sort;

    @Schema(description = "是否在菜单中显示：1-显示 0-隐藏")
    @TableField(value = "visible")
    private Integer visible;

    @Schema(description = "状态：0-禁用 1-正常")
    @TableField(value = "status")
    private BaseStatus status;
}
