package com.atguigu.lease.model.entity;

import com.atguigu.lease.model.enums.BaseStatus;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 角色表 sys_role
 */
@TableName(value = "sys_role")
@Data
public class SysRole extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "角色名称")
    @TableField(value = "name")
    private String name;

    @Schema(description = "角色编码：super_admin/ops/manager/agent/finance/maintainer")
    @TableField(value = "code")
    private String code;

    @Schema(description = "角色描述")
    @TableField(value = "description")
    private String description;

    @Schema(description = "数据范围：ALL-全部 STORE-本门店 SELF-仅本人")
    @TableField(value = "data_scope")
    private String dataScope;

    @Schema(description = "排序")
    @TableField(value = "sort")
    private Integer sort;

    @Schema(description = "状态：0-禁用 1-正常")
    @TableField(value = "status")
    private BaseStatus status;
}
