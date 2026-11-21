package com.atguigu.lease.web.app.vo.region;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 省 / 市 / 区 树节点。
 * 供官方 H5（rentHouseH5）的 {@code GET /app/region/listAsTree} 使用，
 * 前端类型为 RegionInterface { id, name, isCheck?, children? }。
 */
@Data
@Schema(description = "省市区树节点")
public class RegionNodeVo {

    @Schema(description = "区域id")
    private Long id;

    @Schema(description = "区域名称")
    private String name;

    @Schema(description = "子级区域（区县没有子级，返回 null）")
    private List<RegionNodeVo> children;
}
