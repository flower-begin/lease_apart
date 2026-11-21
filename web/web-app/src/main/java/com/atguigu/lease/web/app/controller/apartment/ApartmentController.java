package com.atguigu.lease.web.app.controller.apartment;

import com.atguigu.lease.common.result.Result;
import com.atguigu.lease.web.app.service.ApartmentInfoService;
import com.atguigu.lease.web.app.vo.apartment.ApartmentDetailVo;
import com.atguigu.lease.web.app.vo.apartment.ApartmentItemVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@Tag(name = "公寓信息")
@RequestMapping("/app/apartment")
public class ApartmentController {

    @Autowired
    private ApartmentInfoService apartmentInfoService;

    @Operation(summary = "根据id获取公寓信息")
    @GetMapping("getDetailById")
    public Result<ApartmentDetailVo> getDetailById(@RequestParam Long id) {
        return Result.ok(apartmentInfoService.getApartmentDetailById(id));
    }

    @Operation(summary = "获取已发布的公寓列表（可限制条数，首页「热门公寓」用）")
    @GetMapping("list")
    public Result<List<ApartmentItemVo>> list(@RequestParam(required = false) Integer limit) {
        List<ApartmentItemVo> all = apartmentInfoService.listPublished();
        // 首页只要十来条做展示，不传 limit 时保持原有「全部」语义
        if (limit != null && limit > 0 && all.size() > limit) {
            return Result.ok(new ArrayList<>(all.subList(0, limit)));
        }
        return Result.ok(all);
    }

}
