package com.atguigu.lease.web.admin.controller.apartment;


import com.atguigu.lease.common.result.Result;
import com.atguigu.lease.model.entity.LeaseTerm;
import com.atguigu.lease.web.admin.service.LeaseTermService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "租期管理")
@RequestMapping("/admin/term")
@RestController
public class LeaseTermController {

    @Autowired
    private LeaseTermService leaseTermService;
    /**
     * 租期管理，查询全部，依然是单表查询
     * @return
     */
    @GetMapping("list")
    @Operation(summary = "查询全部租期列表")
    public Result<List<LeaseTerm>> listLeaseTerm() {
        // 由于LeaseTermService是继承了IService的,因此可以直接通过其对象调用mybatis-plus方法
        List<LeaseTerm> termList = leaseTermService.list();
        return Result.ok(termList);  // 要把从数据库中返回的值添加到ok()方法中，否则返回的是null
    }

    @PostMapping("saveOrUpdate")
    @Operation(summary = "保存或更新租期信息")
    public Result saveOrUpdate(@RequestBody LeaseTerm leaseTerm) {  // 用实体类接受前端的数据
        boolean flag = leaseTermService.saveOrUpdate(leaseTerm);
        if (flag) {
            // flag为true表示保存或更新成功
            return Result.ok();
        } else {
            // flag为false表示保存或更新失败
            return Result.fail();
        }
    }

    @DeleteMapping("deleteById")
    @Operation(summary = "根据ID删除租期")
    public Result deleteLeaseTermById(@RequestParam Long id) {
        boolean b = leaseTermService.removeById(id);
        if (b) {
            return Result.ok();
        } else {
            return Result.fail();
        }
    }
}
