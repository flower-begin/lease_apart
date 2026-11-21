package com.atguigu.lease.web.app.controller.agreement;


import com.atguigu.lease.common.context.LoginUserContext;
import com.atguigu.lease.common.result.Result;
import com.atguigu.lease.model.entity.LeaseAgreement;
import com.atguigu.lease.model.enums.LeaseStatus;
import com.atguigu.lease.web.app.service.LeaseAgreementService;
import com.atguigu.lease.web.app.vo.agreement.AgreementDetailVo;
import com.atguigu.lease.web.app.vo.agreement.AgreementItemVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/app/agreement")
@Tag(name = "租约信息")
public class LeaseAgreementController {

    @Autowired
    private LeaseAgreementService leaseAgreementService;

    @Operation(summary = "获取个人租约基本信息列表")
    @GetMapping("listItem")
    public Result<List<AgreementItemVo>> listItem() {
        String phone = LoginUserContext.getUserPhone();
        return Result.ok(leaseAgreementService.listByPhone(phone));
    }

    @Operation(summary = "根据id获取租约详细信息")
    @GetMapping("getDetailById")
    public Result<AgreementDetailVo> getDetailById(@RequestParam Long id) {
        String phone = LoginUserContext.getUserPhone();
        return Result.ok(leaseAgreementService.getDetailById(id, phone));
    }

    @Operation(summary = "根据id更新租约状态", description = "用于确认租约和提前退租")
    @PostMapping("updateStatusById")
    public Result updateStatusById(@RequestParam Long id, @RequestParam LeaseStatus leaseStatus) {
        String phone = LoginUserContext.getUserPhone();
        leaseAgreementService.updateStatusById(id, phone, leaseStatus);
        return Result.ok();
    }

    @Operation(summary = "续约", description = "更新租期与租金，状态变为续约待确认")
    @PostMapping("renew")
    public Result renew(@RequestParam Long id,
                        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date leaseStartDate,
                        @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date leaseEndDate,
                        @RequestParam(required = false) Long leaseTermId,
                        @RequestParam(required = false) BigDecimal rent,
                        @RequestParam(required = false) Long paymentTypeId) {
        leaseAgreementService.renew(id, LoginUserContext.getUserPhone(),
                leaseStartDate, leaseEndDate, leaseTermId, rent, paymentTypeId);
        return Result.ok();
    }

    @Operation(summary = "签约", description = "新增租约，状态为签约待确认")
    @PostMapping("saveOrUpdate")
    public Result saveOrUpdate(@RequestBody LeaseAgreement leaseAgreement) {
        // phone 一律以登录态为准，防止前端伪造
        leaseAgreementService.sign(leaseAgreement, LoginUserContext.getUserPhone());
        return Result.ok();
    }
}
