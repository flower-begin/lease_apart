package com.atguigu.lease.web.admin.controller.system;


import com.atguigu.lease.common.result.Result;
import com.atguigu.lease.model.entity.SystemUser;
import com.atguigu.lease.model.enums.BaseStatus;
import com.atguigu.lease.web.admin.service.SystemUserService;
import com.atguigu.lease.web.admin.vo.system.user.SystemUserItemVo;
import com.atguigu.lease.web.admin.vo.system.user.SystemUserQueryVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@Tag(name = "后台用户信息管理")
@RestController
@RequestMapping("/admin/system/user")
public class SystemUserController {

    @Autowired
    private SystemUserService systemUserService;

    // todo:非登录的查询不允许回显密码
    @Operation(summary = "根据条件分页查询后台用户列表")
    @GetMapping("page")
    public Result<IPage<SystemUserItemVo>> page(@RequestParam long current, @RequestParam long size, SystemUserQueryVo queryVo) {
        IPage<SystemUserItemVo> page = new Page<>(current, size);
        systemUserService.customPage(page, queryVo);
        return Result.ok(page);
    }

    @Operation(summary = "根据ID查询后台用户信息")
    @GetMapping("getById")
    public Result<SystemUserItemVo> getById(@RequestParam Long id) {
        // 多表查询，但是由于是非登录查询，不允许回显密码
        SystemUserItemVo user = systemUserService.customGetById(id);
        return Result.ok(user);
    }

    @Operation(summary = "保存或更新后台用户信息")
    @PostMapping("saveOrUpdate")
    public Result saveOrUpdate(@RequestBody SystemUser systemUser) {
        // 但是前端可以传密码也可以不传入密码
        // 传入密码：可能进行保存新用户或进行用户密码更新，要对密码进行加密
        // 不传入密码：不想修改密码，不用对密码进行加密
        if (systemUser.getPassword() != null) {
            // 前端传入密码，可能进行修改或保存
            // 先对密码进行加密操作
            systemUser.setPassword(DigestUtils.md5Hex(systemUser.getPassword()));
        }
        boolean flag = systemUserService.saveOrUpdate(systemUser);
        if (flag)
            return Result.ok();
        return Result.fail();
    }

    @Operation(summary = "判断后台用户名是否可用")
    @GetMapping("isUserNameAvailable")
    public Result<Boolean> isUsernameExists(@RequestParam String username) {
        // 即前端传入一个用户名，去数据库查找是否有对应的用户并返回一个boolean值
        LambdaQueryWrapper<SystemUser> systemUserLambdaQueryWrapper = new LambdaQueryWrapper<>();
        systemUserLambdaQueryWrapper.eq(SystemUser::getUsername, username);
        long count = systemUserService.count(systemUserLambdaQueryWrapper);
        return Result.ok(count == 0);
    }

    @DeleteMapping("deleteById")
    @Operation(summary = "根据ID删除后台用户信息")
    public Result removeById(@RequestParam Long id) {
        systemUserService.removeById(id);
        return Result.ok();
    }

    @Operation(summary = "根据ID修改后台用户状态")
    @PostMapping("updateStatusByUserId")
    public Result updateStatusByUserId(@RequestParam Long id, @RequestParam BaseStatus status) {
        LambdaUpdateWrapper<SystemUser> systemUserLambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        systemUserLambdaUpdateWrapper.eq(SystemUser::getId, id);
        systemUserLambdaUpdateWrapper.set(SystemUser::getStatus, status);
        systemUserService.update(systemUserLambdaUpdateWrapper);
        return Result.ok();
    }
}
