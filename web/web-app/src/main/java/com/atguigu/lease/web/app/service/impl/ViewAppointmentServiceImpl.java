package com.atguigu.lease.web.app.service.impl;

import com.atguigu.lease.common.exception.LeaseException;
import com.atguigu.lease.common.result.ResultCodeEnum;
import com.atguigu.lease.model.entity.*;
import com.atguigu.lease.model.enums.AppointmentStatus;
import com.atguigu.lease.model.enums.ItemType;
import com.atguigu.lease.web.app.mapper.*;
import com.atguigu.lease.web.app.service.ViewAppointmentService;
import com.atguigu.lease.web.app.vo.apartment.ApartmentItemVo;
import com.atguigu.lease.web.app.vo.appointment.AppointmentDetailVo;
import com.atguigu.lease.web.app.vo.appointment.AppointmentItemVo;
import com.atguigu.lease.web.app.vo.graph.GraphVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * @author liubo
 * @description 针对表【view_appointment(预约看房信息表)】的数据库操作Service实现
 * @createDate 2023-07-26 11:12:39
 */
@Slf4j
@Service
public class ViewAppointmentServiceImpl extends ServiceImpl<ViewAppointmentMapper, ViewAppointment>
        implements ViewAppointmentService {

    @Autowired
    private ViewAppointmentMapper viewAppointmentMapper;

    @Autowired
    private GraphInfoMapper graphInfoMapper;

    @Autowired
    private ApartmentInfoMapper apartmentInfoMapper;

    @Autowired
    private UserInfoMapper userInfoMapper;

    @Override
    public List<AppointmentItemVo> listByUserId(Long userId) {
        List<AppointmentItemVo> list = viewAppointmentMapper.listByUserId(userId);
        if (list != null) {
            for (AppointmentItemVo item : list) {
                ViewAppointment appt = getById(item.getId());
                if (appt != null && appt.getApartmentId() != null) {
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
        if (vo == null) {
            return null;
        }
        if (vo.getApartmentId() != null) {
            vo.setGraphVoList(graphInfoMapper.customQueryList(ItemType.APARTMENT, vo.getApartmentId()));
            ApartmentInfo apartmentInfo = apartmentInfoMapper.selectById(vo.getApartmentId());
            if (apartmentInfo != null) {
                ApartmentItemVo apartmentItemVo = new ApartmentItemVo();
                BeanUtils.copyProperties(apartmentInfo, apartmentItemVo);
                vo.setApartmentItemVo(apartmentItemVo);
            }
        }
        return vo;
    }

    @Override
    public void saveOrUpdateAppointment(ViewAppointment appointment, Long userId, String phone) {
        if (appointment.getApartmentId() == null) {
            throw new LeaseException("请选择要预约的公寓", ResultCodeEnum.PARAM_ERROR.getCode());
        }
        if (appointment.getAppointmentTime() == null) {
            throw new LeaseException("请选择预约看房时间", ResultCodeEnum.PARAM_ERROR.getCode());
        }

        // 姓名、手机号没填就拿账号信息兜底
        if (!StringUtils.hasText(appointment.getName()) || !StringUtils.hasText(appointment.getPhone())) {
            UserInfo userInfo = userInfoMapper.selectById(userId);
            if (userInfo != null && StringUtils.hasText(userInfo.getPhone())) {
                if (!StringUtils.hasText(appointment.getPhone())) {
                    appointment.setPhone(userInfo.getPhone());
                }
                if (!StringUtils.hasText(appointment.getName())) {
                    appointment.setName(userInfo.getNickname());
                }
            }
        }
        if (!StringUtils.hasText(appointment.getPhone())) {
            appointment.setPhone(phone);
        }

        appointment.setUserId(userId);

        if (appointment.getId() == null) {
            // 新增：同一公寓只允许存在一条"待看房"的预约
            LambdaQueryWrapper<ViewAppointment> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ViewAppointment::getUserId, userId);
            wrapper.eq(ViewAppointment::getApartmentId, appointment.getApartmentId());
            wrapper.eq(ViewAppointment::getAppointmentStatus, AppointmentStatus.WAITING);
            ViewAppointment exists = getOne(wrapper, false);
            if (exists != null) {
                // 重复预约就把时间/备注刷新到原记录上，而不是再插一条
                exists.setAppointmentTime(appointment.getAppointmentTime());
                exists.setName(appointment.getName());
                exists.setPhone(appointment.getPhone());
                exists.setAdditionalInfo(appointment.getAdditionalInfo());
                updateById(exists);
                return;
            }
            appointment.setAppointmentStatus(AppointmentStatus.WAITING);
            save(appointment);
        } else {
            // 修改：只能改自己的，且状态保持不变
            ViewAppointment old = getById(appointment.getId());
            if (old == null || !userId.equals(old.getUserId())) {
                throw new LeaseException("预约不存在", ResultCodeEnum.DATA_ERROR.getCode());
            }
            appointment.setAppointmentStatus(old.getAppointmentStatus());
            updateById(appointment);
        }
    }

    @Override
    public void cancel(Long id, Long userId) {
        LambdaUpdateWrapper<ViewAppointment> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ViewAppointment::getId, id);
        wrapper.eq(ViewAppointment::getUserId, userId);
        wrapper.eq(ViewAppointment::getAppointmentStatus, AppointmentStatus.WAITING);
        wrapper.set(ViewAppointment::getAppointmentStatus, AppointmentStatus.CANCELED);
        boolean success = update(wrapper);
        if (!success) {
            throw new LeaseException("预约不存在或当前状态不可取消", ResultCodeEnum.DATA_ERROR.getCode());
        }
    }
}
