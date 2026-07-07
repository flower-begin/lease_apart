package com.atguigu.lease.web.admin.service.impl;

import com.atguigu.lease.model.entity.LeaseAgreement;
import com.atguigu.lease.web.admin.mapper.LeaseAgreementMapper;
import com.atguigu.lease.web.admin.service.LeaseAgreementService;
import com.atguigu.lease.web.admin.vo.agreement.AgreementQueryVo;
import com.atguigu.lease.web.admin.vo.agreement.AgreementVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author liubo
 * @description 针对表【lease_agreement(租约信息表)】的数据库操作Service实现
 * @createDate 2023-07-24 15:48:00
 */
@Service
public class LeaseAgreementServiceImpl extends ServiceImpl<LeaseAgreementMapper, LeaseAgreement>
        implements LeaseAgreementService {

    @Autowired
    private LeaseAgreementMapper leaseAgreementMapper;
    @Override
    public void customPage(Page<AgreementVo> page, AgreementQueryVo queryVo) {
        leaseAgreementMapper.customQueryList(page, queryVo);
    }

    @Override
    public AgreementVo customGetById(Long id) {
        return leaseAgreementMapper.customQueryListById(id);
    }
}




