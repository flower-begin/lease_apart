package com.atguigu.lease.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class BaseEntity implements Serializable {

    @Schema(description = "主键")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "创建时间")
    @TableField(value = "create_time", fill = FieldFill.INSERT)  // 自动填入插入时间
    // 前端不想要这些字段，在转成JSON的时候忽略
    @JsonIgnore
    private Date createTime;

    @Schema(description = "更新时间")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)  // 自动填充更新时间
    // 前端不想要这些字段，在转成JSON时候可以忽略
    @JsonIgnore
    private Date updateTime;

    @Schema(description = "逻辑删除")
    @TableField("is_deleted")
    // 添加逻辑删除字段
    @TableLogic
    @JsonIgnore
    private Byte isDeleted;

}