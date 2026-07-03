package com.atguigu.lease.web.admin.controller.apartment;


import com.atguigu.lease.common.result.Result;
import com.atguigu.lease.model.entity.FeeKey;
import com.atguigu.lease.model.entity.FeeValue;
import com.atguigu.lease.web.admin.service.FeeKeyService;
import com.atguigu.lease.web.admin.service.FeeValueService;
import com.atguigu.lease.web.admin.vo.fee.FeeKeyVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@Tag(name = "房间杂费管理")
@RestController
@RequestMapping("/admin/fee")
public class FeeController {

    @Autowired
    private FeeKeyService feeKeyService;
    @Autowired
    private FeeValueService feeValueService;
    @Operation(summary = "保存或更新杂费名称")
    @PostMapping("key/saveOrUpdate")
    public Result saveOrUpdateFeeKey(@RequestBody FeeKey feeKey) {
        // 前端传入杂费对象
        boolean flag = feeKeyService.saveOrUpdate(feeKey);
        if (flag) {
            return Result.ok();
        } else {
            return Result.fail();
        }
    }

    @Operation(summary = "保存或更新杂费值")
    @PostMapping("value/saveOrUpdate")
    public Result saveOrUpdateFeeValue(@RequestBody FeeValue feeValue) {
        boolean flag = feeValueService.saveOrUpdate(feeValue);
        if (flag) {
            return Result.ok();
        } else {
            return Result.fail();
        }
    }


    @Operation(summary = "查询全部杂费名称和杂费值列表")
    @GetMapping("list")
    public Result<List<FeeKeyVo>> feeInfoList() {
        // 调用service层方法
        // 由于数据库中fee_key表的一个id就对应多个fee_value
        // 因此我们会定义一个FeeKeyVo类来继承FeeKey类，然后在加入对应查询到的fee_value列表
        // 并且前端点击后是要查询一个key的id对应的所有value，所以要用一个FeeKeyVo类来封装并返回一个列表
        List<FeeKeyVo> list = feeKeyService.feeInfoList();
        return Result.ok(list);
    }

    @Operation(summary = "根据id删除杂费名称")
    @DeleteMapping("key/deleteById")
    public Result deleteFeeKeyById(@RequestParam Long feeKeyId) {
        // 当删除主表杂费名称时，从表杂费值会级联删除
        // 先删除主表杂费名称
        boolean flag = feeKeyService.removeById(feeKeyId);
        // 再删除从表杂费值
        // 而且仅当主表删除成功后才能删除从表
        if (flag) {
            LambdaQueryWrapper<FeeValue> feeValueLambdaQueryWrapper = new LambdaQueryWrapper<>();
            // 写入条件
            feeValueLambdaQueryWrapper.eq(FeeValue::getFeeKeyId,feeKeyId);
            feeValueService.remove(feeValueLambdaQueryWrapper);
            return Result.ok();
        }
        return Result.fail();
    }

    @Operation(summary = "根据id删除杂费值")
    @DeleteMapping("value/deleteById")
    public Result deleteFeeValueById(@RequestParam Long id) {
        feeValueService.removeById(id);
        return Result.ok();
    }
}
