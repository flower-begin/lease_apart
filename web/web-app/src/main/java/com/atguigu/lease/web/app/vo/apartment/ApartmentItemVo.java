package com.atguigu.lease.web.app.vo.apartment;


import com.atguigu.lease.model.entity.ApartmentInfo;
import com.atguigu.lease.model.entity.LabelInfo;
import com.atguigu.lease.web.app.vo.graph.GraphVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "App端公寓信息")
public class ApartmentItemVo extends ApartmentInfo {

    private List<LabelInfo> labelInfoList;

    /**
     * 统一用 GraphVo（只有 name + url），和其它 VO 保持一致，
     * 避免同名字段在 ApartmentItemVo 里是 List&lt;GraphInfo&gt;、在 ApartmentDetailVo 里是 List&lt;GraphVo&gt; 造成误用。
     */
    private List<GraphVo> graphVoList;

    private BigDecimal minRent;
}
