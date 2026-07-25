package com.atguigu.lease.web.app.vo.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "AI推荐请求")
public class RecommendRequestVo {

    @Schema(description = "用户偏好描述", example = "靠近市中心，两室一厅，预算3000以内")
    private String preference;

    @Schema(description = "城市ID")
    private Long cityId;

    @Schema(description = "最高租金")
    private BigDecimal maxRent;
}
