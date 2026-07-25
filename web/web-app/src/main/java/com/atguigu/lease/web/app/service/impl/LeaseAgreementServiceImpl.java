package com.atguigu.lease.web.app.service.impl;

import com.atguigu.lease.model.entity.*;
import com.atguigu.lease.model.enums.ItemType;
import com.atguigu.lease.model.enums.LeaseStatus;
import com.atguigu.lease.web.app.mapper.*;
import com.atguigu.lease.web.app.service.LeaseAgreementService;
import com.atguigu.lease.web.app.vo.agreement.AgreementDetailVo;
import com.atguigu.lease.web.app.vo.agreement.AgreementItemVo;
import com.atguigu.lease.web.app.vo.graph.GraphVo;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author liubo
 * @description 针对表【lease_agreement(租约信息表)】的数据库操作Service实现
 * @createDate 2023-07-26 11:12:39
 */
@Service
public class LeaseAgreementServiceImpl extends ServiceImpl<LeaseAgreementMapper, LeaseAgreement>
        implements LeaseAgreementService {

    @Autowired
    private LeaseAgreementMapper leaseAgreementMapper;

    @Autowired
    private GraphInfoMapper graphInfoMapper;

    @Autowired
    private PaymentTypeMapper paymentTypeMapper;

    @Override
    public List<AgreementItemVo> listByPhone(String phone) {
        List<AgreementItemVo> list = leaseAgreementMapper.listByPhone(phone);
        if (list != null) {
            for (AgreementItemVo item : list) {
                LeaseAgreement agreement = getById(item.getId());
                if (agreement != null) {
                    List<GraphVo> graphVoList = graphInfoMapper.customQueryList(ItemType.ROOM, agreement.getRoomId());
                    item.setRoomGraphVoList(graphVoList);
                    item.setIsRenewable(agreement.getStatus() == LeaseStatus.SIGNED);
                }
            }
        }
        return list;
    }

    @Override
    public AgreementDetailVo getDetailById(Long id, String phone) {
        AgreementDetailVo vo = leaseAgreementMapper.getDetailByIdAndPhone(id, phone);
        if (vo != null) {
            List<GraphVo> apartmentGraphVoList = graphInfoMapper.customQueryList(ItemType.APARTMENT, vo.getApartmentId());
            List<GraphVo> roomGraphVoList = graphInfoMapper.customQueryList(ItemType.ROOM, vo.getRoomId());
            vo.setApartmentGraphVoList(apartmentGraphVoList);
            vo.setRoomGraphVoList(roomGraphVoList);

            if (vo.getPaymentTypeId() != null) {
                PaymentType paymentType = paymentTypeMapper.selectById(vo.getPaymentTypeId());
                if (paymentType != null) {
                    vo.setPaymentTypeName(paymentType.getName());
                }
            }
        }
        return vo;
    }

    @Override
    public void updateStatusById(Long id, String phone, LeaseStatus leaseStatus) {
        LambdaUpdateWrapper<LeaseAgreement> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(LeaseAgreement::getId, id);
        wrapper.eq(LeaseAgreement::getPhone, phone);
        wrapper.set(LeaseAgreement::getStatus, leaseStatus);
        update(wrapper);
    }

}
