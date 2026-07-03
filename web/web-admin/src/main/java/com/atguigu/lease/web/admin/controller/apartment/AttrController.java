package com.atguigu.lease.web.admin.controller.apartment;


import com.atguigu.lease.common.result.Result;
import com.atguigu.lease.model.entity.AttrKey;
import com.atguigu.lease.model.entity.AttrValue;
import com.atguigu.lease.web.admin.service.AttrKeyService;
import com.atguigu.lease.web.admin.service.AttrValueService;
import com.atguigu.lease.web.admin.vo.attr.AttrKeyVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@Tag(name = "房间属性管理")
@RestController
@RequestMapping("/admin/attr")
public class AttrController {

    @Autowired
    private AttrKeyService attrKeyService;
    @Autowired
    private AttrValueService attrValueService;

    @Operation(summary = "新增或更新属性名称")
    @PostMapping("key/saveOrUpdate")
    public Result saveOrUpdateAttrKey(@RequestBody AttrKey attrKey) {
        boolean flag = attrKeyService.saveOrUpdate(attrKey);
        return flag ? Result.ok() : Result.fail();
    }

    @Operation(summary = "新增或更新属性值")
    @PostMapping("value/saveOrUpdate")
    public Result saveOrUpdateAttrValue(@RequestBody AttrValue attrValue) {
        boolean flag = attrValueService.saveOrUpdate(attrValue);
        return flag ? Result.ok() : Result.fail();
    }


    @Operation(summary = "根据id删除属性名称")
    @DeleteMapping("key/deleteById")
    public Result removeAttrKeyById(@RequestParam Long attrKeyId) {
        boolean remove = attrKeyService.removeById(attrKeyId);
        if (remove) {
            // attr_key的主键既是主键也是外键
            // 级联删除从表中的对应数据不能使用removeById,要创建LambdaQueryWrapper然后去设置查询的条件
            // 再通过对应的service层对象去删除
            // attr_value不是根据自身id删除，而是根据attr_key_id删除
            LambdaQueryWrapper<AttrValue> attrValueLambdaQueryWrapper = new LambdaQueryWrapper<>();
            attrValueLambdaQueryWrapper.eq(AttrValue::getAttrKeyId, attrKeyId);
            attrValueService.remove(attrValueLambdaQueryWrapper);
            return Result.ok();
        }
        return Result.fail();
    }

    /**
     * 删除attr_key主表数据也应该删除attr_value从表数据,否则会出现孤儿数据
     * @param id
     * @return
     */
    @Operation(summary = "根据id删除属性值")
    @DeleteMapping("value/deleteById")
    public Result removeAttrValueById(@RequestParam Long id) {
        attrValueService.removeById(id);
        return Result.ok();
    }

    /**
     * 本次接口是多表查询，无法使用mybatis-plus提供的CRUD方式
     * 我们所定义的实体类无法接受或响应产生的结果，所以我们定义一个vo类，让其继承实体类，再加入一个带指定泛型的集合
     * 自定义SQL查询还需要关注逻辑删除字段
     * 使用外连接子表的条件必须放在on后面(否则会出现即使使用了外连接，但实际查询结果中仍然包含从表中没有的数据)
     *
     * 实现：先定义一个vo实体类
     *      再编写service业务逻辑
     *      再编写mapper接口和对应的xml配置文件
     * @return
     */
    @Operation(summary = "查询全部属性名称和属性值列表")
    @GetMapping("list")
    public Result<List<AttrKeyVo>> listAttrInfo() {
        List<AttrKeyVo> attrKeyVoList = attrKeyService.customList();
        return Result.ok(attrKeyVoList);
    }

}
