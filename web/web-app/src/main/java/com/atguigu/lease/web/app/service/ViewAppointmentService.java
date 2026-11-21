package com.atguigu.lease.web.app.service;

import com.atguigu.lease.model.entity.ViewAppointment;
import com.atguigu.lease.web.app.vo.appointment.AppointmentDetailVo;
import com.atguigu.lease.web.app.vo.appointment.AppointmentItemVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author liubo
* @description 针对表【view_appointment(预约看房信息表)】的数据库操作Service
* @createDate 2023-07-26 11:12:39
*/
public interface ViewAppointmentService extends IService<ViewAppointment> {

    List<AppointmentItemVo> listByUserId(Long userId);

    AppointmentDetailVo getDetailById(Long id, Long userId);

    /**
     * 新增 / 修改预约。新增时会做重复预约校验，并把用户姓名、手机号补全。
     */
    void saveOrUpdateAppointment(ViewAppointment appointment, Long userId, String phone);

    /**
     * 取消预约（只能取消自己的、且还在"待看房"状态的预约）。
     */
    void cancel(Long id, Long userId);
}
