package com.atguigu.lease.web.app.service.impl;

import com.atguigu.lease.common.exception.LeaseException;
import com.atguigu.lease.common.result.ResultCodeEnum;
import com.atguigu.lease.model.entity.*;
import com.atguigu.lease.model.enums.ItemType;
import com.atguigu.lease.model.enums.LeaseSourceType;
import com.atguigu.lease.model.enums.LeaseStatus;
import com.atguigu.lease.model.enums.ReleaseStatus;
import com.atguigu.lease.web.app.mapper.*;
import com.atguigu.lease.web.app.service.LeaseAgreementService;
import com.atguigu.lease.web.app.vo.agreement.AgreementDetailVo;
import com.atguigu.lease.web.app.vo.agreement.AgreementItemVo;
import com.atguigu.lease.web.app.vo.graph.GraphVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Date;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    @Autowired
    private LeaseTermMapper leaseTermMapper;

    @Autowired
    private RoomInfoMapper roomInfoMapper;

    @Autowired
    private RoomPaymentTypeMapper roomPaymentTypeMapper;

    @Autowired
    private RoomLeaseTermMapper roomLeaseTermMapper;

    /**
     * 允许的状态流转：当前状态 -> 可流转到的目标状态集合。
     * 这样前端乱传状态（比如把已退租的改成已签约）会被直接拒绝。
     */
    private static final Map<LeaseStatus, Set<LeaseStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(LeaseStatus.class);

    /** 视为"房间已被占用"的状态，与后台房间列表口径一致 */
    private static final List<LeaseStatus> OCCUPIED_STATUS =
            List.of(LeaseStatus.SIGNED, LeaseStatus.WITHDRAWING, LeaseStatus.RENEWING);

    static {
        ALLOWED_TRANSITIONS.put(LeaseStatus.SIGNING, EnumSet.of(LeaseStatus.SIGNED, LeaseStatus.CANCELED));
        ALLOWED_TRANSITIONS.put(LeaseStatus.SIGNED, EnumSet.of(LeaseStatus.WITHDRAWING, LeaseStatus.RENEWING));
        ALLOWED_TRANSITIONS.put(LeaseStatus.WITHDRAWING, EnumSet.of(LeaseStatus.SIGNED, LeaseStatus.WITHDRAWN));
        ALLOWED_TRANSITIONS.put(LeaseStatus.RENEWING, EnumSet.of(LeaseStatus.SIGNED));
        ALLOWED_TRANSITIONS.put(LeaseStatus.EXPIRED, EnumSet.of(LeaseStatus.RENEWING));
        ALLOWED_TRANSITIONS.put(LeaseStatus.CANCELED, EnumSet.noneOf(LeaseStatus.class));
        ALLOWED_TRANSITIONS.put(LeaseStatus.WITHDRAWN, EnumSet.noneOf(LeaseStatus.class));
    }

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
            if (vo.getLeaseTermId() != null) {
                LeaseTerm leaseTerm = leaseTermMapper.selectById(vo.getLeaseTermId());
                if (leaseTerm != null) {
                    vo.setLeaseTermMonthCount(leaseTerm.getMonthCount());
                    vo.setLeaseTermUnit(leaseTerm.getUnit());
                }
            }
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatusById(Long id, String phone, LeaseStatus leaseStatus) {
        if (leaseStatus == null) {
            throw new LeaseException("租约状态不能为空", ResultCodeEnum.PARAM_ERROR.getCode());
        }
        LeaseAgreement agreement = getById(id);
        if (agreement == null || !phone.equals(agreement.getPhone())) {
            throw new LeaseException("租约不存在", ResultCodeEnum.DATA_ERROR.getCode());
        }
        LeaseStatus current = agreement.getStatus();
        if (current == leaseStatus) {
            return;
        }
        Set<LeaseStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(current, EnumSet.noneOf(LeaseStatus.class));
        if (!allowed.contains(leaseStatus)) {
            throw new LeaseException(
                    "当前状态[" + current.getName() + "]不能变更为[" + leaseStatus.getName() + "]",
                    ResultCodeEnum.DATA_ERROR.getCode());
        }

        LambdaUpdateWrapper<LeaseAgreement> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(LeaseAgreement::getId, id);
        wrapper.eq(LeaseAgreement::getPhone, phone);
        wrapper.set(LeaseAgreement::getStatus, leaseStatus);
        update(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sign(LeaseAgreement agreement, String phone) {
        if (agreement.getApartmentId() == null || agreement.getRoomId() == null) {
            throw new LeaseException("请选择要签约的公寓和房间", ResultCodeEnum.PARAM_ERROR.getCode());
        }

        // 1. 房间必须存在、属于该公寓、且已发布
        RoomInfo room = roomInfoMapper.selectById(agreement.getRoomId());
        if (room == null) {
            throw new LeaseException("房间不存在", ResultCodeEnum.DATA_ERROR.getCode());
        }
        if (!room.getApartmentId().equals(agreement.getApartmentId())) {
            throw new LeaseException("房间不属于该公寓", ResultCodeEnum.DATA_ERROR.getCode());
        }
        if (room.getIsRelease() != ReleaseStatus.RELEASED) {
            throw new LeaseException("该房间未发布，暂不可签约", ResultCodeEnum.DATA_ERROR.getCode());
        }

        // 2. 房间不能已被占用
        LambdaQueryWrapper<LeaseAgreement> occupied = new LambdaQueryWrapper<>();
        occupied.eq(LeaseAgreement::getRoomId, agreement.getRoomId());
        occupied.in(LeaseAgreement::getStatus, OCCUPIED_STATUS);
        if (count(occupied) > 0) {
            throw new LeaseException("该房间已被租出", ResultCodeEnum.DATA_ERROR.getCode());
        }

        // 2.1 同一个人对同一房间不能重复提交签约申请
        LambdaQueryWrapper<LeaseAgreement> pending = new LambdaQueryWrapper<>();
        pending.eq(LeaseAgreement::getRoomId, agreement.getRoomId());
        pending.eq(LeaseAgreement::getPhone, phone);
        pending.eq(LeaseAgreement::getStatus, LeaseStatus.SIGNING);
        if (count(pending) > 0) {
            throw new LeaseException("你已提交过该房间的签约申请，请等待房东确认",
                    ResultCodeEnum.DATA_ERROR.getCode());
        }

        // 3. 支付方式 / 租期必须是该房间支持的
        if (agreement.getPaymentTypeId() != null) {
            List<RoomPaymentType> supported = roomPaymentTypeMapper.selectList(new LambdaQueryWrapper<RoomPaymentType>()
                    .eq(RoomPaymentType::getRoomId, agreement.getRoomId())
                    .eq(RoomPaymentType::getPaymentTypeId, agreement.getPaymentTypeId()));
            if (supported == null || supported.isEmpty()) {
                throw new LeaseException("该房间不支持所选支付方式", ResultCodeEnum.PARAM_ERROR.getCode());
            }
        }
        if (agreement.getLeaseTermId() != null) {
            List<RoomLeaseTerm> supported = roomLeaseTermMapper.selectList(new LambdaQueryWrapper<RoomLeaseTerm>()
                    .eq(RoomLeaseTerm::getRoomId, agreement.getRoomId())
                    .eq(RoomLeaseTerm::getLeaseTermId, agreement.getLeaseTermId()));
            if (supported == null || supported.isEmpty()) {
                throw new LeaseException("该房间不支持所选租期", ResultCodeEnum.PARAM_ERROR.getCode());
            }
        }

        // 4. 日期与金额
        if (agreement.getLeaseStartDate() == null || agreement.getLeaseEndDate() == null) {
            throw new LeaseException("请填写租期起止日期", ResultCodeEnum.PARAM_ERROR.getCode());
        }
        if (!agreement.getLeaseEndDate().after(agreement.getLeaseStartDate())) {
            throw new LeaseException("租期结束日期必须晚于开始日期", ResultCodeEnum.PARAM_ERROR.getCode());
        }
        if (agreement.getRent() == null || agreement.getRent().compareTo(BigDecimal.ZERO) <= 0) {
            agreement.setRent(room.getRent());
        }
        if (agreement.getDeposit() == null) {
            agreement.setDeposit(BigDecimal.ZERO);
        }

        // 5. 强制服务端字段，避免前端伪造
        agreement.setId(null);
        agreement.setPhone(phone);
        agreement.setStatus(LeaseStatus.SIGNING);
        agreement.setSourceType(LeaseSourceType.NEW);
        if (!StringUtils.hasText(agreement.getName())) {
            throw new LeaseException("请填写承租人姓名", ResultCodeEnum.PARAM_ERROR.getCode());
        }

        save(agreement);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void renew(Long id, String phone, Date newStartDate, Date newEndDate,
                      Long newLeaseTermId, BigDecimal newRent, Long newPaymentTypeId) {
        LeaseAgreement agreement = getById(id);
        if (agreement == null || !phone.equals(agreement.getPhone())) {
            throw new LeaseException("租约不存在", ResultCodeEnum.DATA_ERROR.getCode());
        }
        if (agreement.getStatus() != LeaseStatus.SIGNED && agreement.getStatus() != LeaseStatus.EXPIRED) {
            throw new LeaseException("只有已签约或已到期的租约可以续约", ResultCodeEnum.DATA_ERROR.getCode());
        }
        if (newEndDate == null || (newStartDate != null && !newEndDate.after(newStartDate))) {
            throw new LeaseException("续约的结束日期不合法", ResultCodeEnum.PARAM_ERROR.getCode());
        }

        LeaseAgreement update = new LeaseAgreement();
        update.setId(id);
        update.setLeaseEndDate(newEndDate);
        if (newStartDate != null) {
            update.setLeaseStartDate(newStartDate);
        }
        if (newLeaseTermId != null) {
            update.setLeaseTermId(newLeaseTermId);
        }
        if (newRent != null) {
            update.setRent(newRent);
        }
        if (newPaymentTypeId != null) {
            update.setPaymentTypeId(newPaymentTypeId);
        }
        update.setSourceType(LeaseSourceType.RENEW);
        update.setStatus(LeaseStatus.RENEWING);
        updateById(update);
    }
}
