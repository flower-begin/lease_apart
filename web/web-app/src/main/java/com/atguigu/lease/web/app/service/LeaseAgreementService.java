package com.atguigu.lease.web.app.service;

import com.atguigu.lease.model.entity.LeaseAgreement;
import com.atguigu.lease.model.enums.LeaseStatus;
import com.atguigu.lease.web.app.vo.agreement.AgreementDetailVo;
import com.atguigu.lease.web.app.vo.agreement.AgreementItemVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
* @author liubo
* @description 针对表【lease_agreement(租约信息表)】的数据库操作Service
* @createDate 2023-07-26 11:12:39
*/
public interface LeaseAgreementService extends IService<LeaseAgreement> {

    List<AgreementItemVo> listByPhone(String phone);

    AgreementDetailVo getDetailById(Long id, String phone);

    /**
     * 变更租约状态（确认签约 / 申请退租 / 撤销退租 / 确认续约 ...），会校验状态流转是否合法。
     */
    void updateStatusById(Long id, String phone, LeaseStatus leaseStatus);

    /**
     * 签约（新增租约）。会校验房间归属、房间是否已被占用、支付方式与租期是否属于该房间，
     * 并把状态置为"签约待确认"。
     */
    void sign(LeaseAgreement agreement, String phone);

    /**
     * 续约：更新租期区间与租金，并把状态置为"续约待确认"。
     */
    void renew(Long id, String phone, Date newStartDate, Date newEndDate,
               Long newLeaseTermId, BigDecimal newRent, Long newPaymentTypeId);
}
