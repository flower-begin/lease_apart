package com.atguigu.lease.web.admin.vo.system.menu;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "菜单树节点")
@Data
public class MenuVo {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "父级id，0 为顶级")
    private Long parentId;

    @Schema(description = "菜单名称")
    private String name;

    @Schema(description = "类型：1-目录 2-菜单 3-按钮")
    private Integer type;

    @Schema(description = "前端路由路径")
    private String path;

    @Schema(description = "Element Plus 图标名")
    private String icon;

    @Schema(description = "权限标识")
    private String perms;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "子节点")
    private List<MenuVo> children = new ArrayList<>();
}
