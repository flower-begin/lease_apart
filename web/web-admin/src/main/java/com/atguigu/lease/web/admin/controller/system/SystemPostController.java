package com.atguigu.lease.web.admin.controller.system;

import com.atguigu.lease.common.result.Result;
import com.atguigu.lease.model.entity.SystemPost;
import com.atguigu.lease.model.enums.BaseStatus;
import com.atguigu.lease.web.admin.service.SystemPostService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@Tag(name = "后台用户岗位管理")
@RequestMapping("/admin/system/post")
public class SystemPostController {

    // 先注入service组件
    @Autowired
    private SystemPostService systemPostService;

    @Operation(summary = "分页获取岗位信息")
    @GetMapping("page")
    private Result<IPage<SystemPost>> page(@RequestParam long current, @RequestParam long size) {
        IPage<SystemPost> page = new Page<>(current, size);
        // 由于没有传入查询条件
        // 所以不需要写LambdaQueryWrapper添加条件了
        // 直接调用mybatis-plus自带的单表分页查询
        systemPostService.page(page);
        return Result.ok(page);
    }

    @Operation(summary = "保存或更新岗位信息")
    @PostMapping("saveOrUpdate")
    public Result saveOrUpdate(@RequestBody SystemPost systemPost) {
        systemPostService.saveOrUpdate(systemPost);
        return Result.ok();
    }

    @DeleteMapping("deleteById")
    @Operation(summary = "根据id删除岗位")
    public Result removeById(@RequestParam Long id) {
        systemPostService.removeById(id);
        return Result.ok();
    }

    @GetMapping("getById")
    @Operation(summary = "根据id获取岗位信息")
    public Result<SystemPost> getById(@RequestParam Long id) {
        LambdaQueryWrapper<SystemPost> systemPostLambdaQueryWrapper = new LambdaQueryWrapper<>();
        systemPostLambdaQueryWrapper.eq(SystemPost::getId, id);
        SystemPost postPost = systemPostService.getById(id);
        return Result.ok(postPost);
    }

    @Operation(summary = "获取全部岗位列表")
    @GetMapping("list")
    public Result<List<SystemPost>> list() {
        List<SystemPost> lists = systemPostService.list();
        return Result.ok(lists);
    }

    @Operation(summary = "根据岗位id修改状态")
    @PostMapping("updateStatusByPostId")
    public Result updateStatusByPostId(@RequestParam Long id, @RequestParam BaseStatus status) {
        LambdaUpdateWrapper<SystemPost> systemPostLambdaQueryWrapper = new LambdaUpdateWrapper<>();
        systemPostLambdaQueryWrapper.eq(SystemPost::getId, id);
        systemPostLambdaQueryWrapper.set(SystemPost::getStatus, status);
        systemPostService.update(systemPostLambdaQueryWrapper);
        return Result.ok();
    }
}
