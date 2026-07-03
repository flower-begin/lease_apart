package com.atguigu.lease.web.admin.controller.apartment;


import com.atguigu.lease.common.result.Result;
import com.atguigu.lease.model.entity.LabelInfo;
import com.atguigu.lease.model.enums.ItemType;
import com.atguigu.lease.web.admin.service.LabelInfoService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "标签管理")
@RestController
@RequestMapping("/admin/label")
public class LabelController {

    @Autowired
    private LabelInfoService labelInfoService;

    /**
     * 查询所有当type = null  都是返回集合
     * 查询房间type = 2      都是返回一个集合
     * 查询公寓type = 1      都是返回一个集合
     *
     * 错误问题：
     *      前端传递参数并测试，醉胡抛出400异常
     *      原因：我的参数使用枚举接受，而前端传递的是数字，导致类型不匹配
     *
     * 解决方案：配置三个数据类型转换器
     *      1.Spring MVC的Converter
     *      2.Jackson的注解  @JsonValue
     *      3.Mybatis-plus的注解  @EnumValue
     * @param type
     * @return
     */
    @Operation(summary = "（根据类型）查询标签列表")
    @GetMapping("list")
    public Result<List<LabelInfo>> labelList(@RequestParam(required = false) ItemType type) {
        LambdaQueryWrapper<LabelInfo> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(type != null, LabelInfo::getType, type);
        List<LabelInfo> labelInfoList = labelInfoService.list(lambdaQueryWrapper);
        return Result.ok(labelInfoList);
    }

    @Operation(summary = "新增或修改标签信息")
    @PostMapping("saveOrUpdate")
    public Result saveOrUpdateLabel(@RequestBody LabelInfo labelInfo) {
        boolean flag = labelInfoService.saveOrUpdate(labelInfo);
        if (flag) {
            return Result.ok();
        } else {
            return Result.fail();
        }
    }

    @Operation(summary = "根据id删除标签信息")
    @DeleteMapping("deleteById")
    public Result deleteLabelById(@RequestParam Long id) {
        labelInfoService.removeById(id);
        return Result.ok();
    }
}
