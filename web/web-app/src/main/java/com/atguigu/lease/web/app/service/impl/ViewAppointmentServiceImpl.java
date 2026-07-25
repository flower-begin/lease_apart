package com.atguigu.lease.web.app.service.impl;

import com.atguigu.lease.model.entity.*;
import com.atguigu.lease.model.enums.ItemType;
import com.atguigu.lease.web.app.mapper.*;
import com.atguigu.lease.web.app.service.ViewAppointmentService;
import com.atguigu.lease.web.app.vo.appointment.AppointmentDetailVo;
import com.atguigu.lease.web.app.vo.appointment.AppointmentItemVo;
import com.atguigu.lease.web.app.vo.graph.GraphVo;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author liubo
 * @description 针对表【view_appointment(预约看房信息表)】的数据库操作Service实现
 * @createDate 2023-07-26 11:12:39
 */
@Service
public class ViewAppointmentServiceImpl extends ServiceImpl<ViewAppointmentMapper, ViewAppointment>
        implements ViewAppointmentService {

    @Autowired
    private ViewAppointmentMapper viewAppointmentMapper;

    @Autowired
    private GraphInfoMapper graphInfoMapper;

    @Autowired
    private ApartmentInfoMapper apartmentInfoMapper;

    @Override
    public List<AppointmentItemVo> listByUserId(Long userId) {
        List<AppointmentItemVo> list = viewAppointmentMapper.listByUserId(userId);
        // Populate apartment images
        if (list != null) {
            for (AppointmentItemVo item : list) {
                // Get apartment info to find the apartmentId from view_appointment
                ViewAppointment appt = getById(item.getId());
                if (appt != null) {
                    List<GraphVo> graphVoList = graphInfoMapper.customQueryList(ItemType.APARTMENT, appt.getApartmentId());
                    item.setGraphVoList(graphVoList);
                }
            }
        }
        return list;
    }

    @Override
    public AppointmentDetailVo getDetailById(Long id, Long userId) {
        AppointmentDetailVo vo = viewAppointmentMapper.getDetailById(id, userId);
        if (vo != null) {
            List<GraphVo> graphVoList = graphInfoMapper.customQueryList(ItemType.APARTMENT, vo.getApartmentId());
            // AppointmentDetailVo doesn't have graphVoList but it has apartmentItemVo
            // We'll set the apartment name which is already queried
        }
        return vo;
    }

}
