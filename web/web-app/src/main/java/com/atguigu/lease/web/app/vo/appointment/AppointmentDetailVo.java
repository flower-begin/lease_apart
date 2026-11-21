package com.atguigu.lease.web.app.vo.appointment;

import com.atguigu.lease.model.entity.ViewAppointment;
import com.atguigu.lease.web.app.vo.apartment.ApartmentItemVo;
import com.atguigu.lease.web.app.vo.graph.GraphVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;


@Data
@Schema(description = "APP端预约看房详情")
public class AppointmentDetailVo extends ViewAppointment {

    @Schema(description = "公寓名称")
    private String apartmentName;

    @Schema(description = "公寓地址（省市区拼接）")
    private String apartmentAddress;

    @Schema(description = "公寓图片列表")
    private List<GraphVo> graphVoList;

    @Schema(description = "公寓基本信息")
    private ApartmentItemVo apartmentItemVo;
}
