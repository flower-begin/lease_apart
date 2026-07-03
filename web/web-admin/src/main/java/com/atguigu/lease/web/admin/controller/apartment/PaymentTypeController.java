package com.atguigu.lease.web.admin.controller.apartment;


import com.atguigu.lease.common.result.Result;
import com.atguigu.lease.model.entity.PaymentType;
import com.atguigu.lease.web.admin.service.PaymentTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@Tag(name = "支付方式管理")
@RequestMapping("/admin/payment")  // 不一定需要写，仅仅只是提取公共路径，但是方法上的Mapping是一定要写的
@RestController  // @RestController = @ResponseBody + @Controller 表示返回数据都是JSON格式的数据
public class PaymentTypeController {

    @Autowired
    private PaymentTypeService paymentTypeService;

    /**
     * 实现思路：单表操作
     *      查询单表所有数据，没有分页
     *  实现过程：mybatis-plus的service扩展方法，查询全部支付方式集合
     *      service.list()
     *      list -> Result.ok(list)
     *
     *  问题：1.多了三个字段：createTime, updateTime, isDeleted
     *       2.逻辑删除没有真正生效（在表中添加一个数字字段0表示未删除1表示已删除  ||  使用@TableLogic注解）
     *              在BaseEntity的isDelete字段加上加入@TableLogic注解
     * @return
     */
    @Operation(summary = "查询全部支付方式列表")
    @GetMapping("list")
    public Result<List<PaymentType>> listPaymentType() {
        // 叫用service中的list方法返回一个集合，然后将集合当参数传递给Result中的ok方法
        List<PaymentType> list = paymentTypeService.list();
        return Result.ok(list);
    }

    /**
     * 支付方式保存和删除接口
     * 实现思路：单表的更新和保存
     *          有id从前端传入就是更新，没有传入就是保存
     * 实现步骤：
     *      1.导入PaymentTypeService对象
     *      2.通过这个对象调用saveOrUpdate方法
     *      3.在controller中判断并进行相应的返回
     *
     * 问题：1.逻辑删除字段没有默认值
     *          1.1 在BaseEntity中的isDeleted字段添加默认值0
     *          1.2 给数据库的is_deleted设置默认值
     *      2.创建时间和修改时间没有赋值
     *          用mybatis-plus的自动填充功能
     * @param paymentType
     * @return
     */
    @Operation(summary = "保存或更新支付方式")
    @PostMapping("saveOrUpdate")
    public Result saveOrUpdatePaymentType(@RequestBody PaymentType paymentType) {
        boolean flag = paymentTypeService.saveOrUpdate(paymentType);
        if(flag) {
            return Result.ok();
        } else {
            return Result.fail();
        }

    }

    @Operation(summary = "根据ID删除支付方式")
    @DeleteMapping("deleteById")
    public Result deletePaymentById(@RequestParam Long id) {
        paymentTypeService.removeById(id);
        return Result.ok();
    }

}
