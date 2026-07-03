package com.atguigu.lease.web.admin.service;

import com.atguigu.lease.model.entity.PaymentType;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author liubo
* @description 针对表【payment_type(支付方式表)】的数据库操作Service
* @createDate 2023-07-24 15:48:00
*/
public interface PaymentTypeService extends IService<PaymentType> {
    // 当自己的service层继承了IService接口后自己也可以在这个接口中写入对应的抽象方法
    // 同时自己的service层的也拥有了mybatis-plus的CRUD接口，因此他的实现类可以直接调用这些方法
}
