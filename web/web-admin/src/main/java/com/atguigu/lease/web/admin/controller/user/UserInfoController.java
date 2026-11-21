package com.atguigu.lease.web.admin.controller.user;


import com.atguigu.lease.common.exception.LeaseException;
import com.atguigu.lease.common.result.Result;
import com.atguigu.lease.common.result.ResultCodeEnum;
import com.atguigu.lease.model.entity.UserInfo;
import com.atguigu.lease.model.enums.BaseStatus;
import com.atguigu.lease.web.admin.custom.annotation.RequirePermission;
import com.atguigu.lease.web.admin.service.UserInfoService;
import com.atguigu.lease.web.admin.vo.user.UserInfoQueryVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户管理（所有注册用户）")
@RestController
@RequestMapping("/admin/user")
public class UserInfoController {

    /** C 端角色：管理员 —— 可以进后台，也能在移动端/网页版看到全部页面 */
    public static final String ROLE_ADMIN = "admin";
    /** C 端角色：租客 —— 只能看自己的页面 */
    public static final String ROLE_TENANT = "tenant";

    @Autowired
    private UserInfoService userInfoService;

    @Operation(summary = "分页查询用户信息（所有注册用户）")
    @GetMapping("page")
    @RequirePermission("user:list")
    public Result<IPage<UserInfo>> pageUserInfo(@RequestParam long current, @RequestParam long size, UserInfoQueryVo queryVo) {
        IPage<UserInfo> page = new Page<>(current, size);
        LambdaQueryWrapper<UserInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(queryVo.getStatus() != null, UserInfo::getStatus, queryVo.getStatus());
        wrapper.eq(StringUtils.hasText(queryVo.getRoleCode()), UserInfo::getRoleCode, queryVo.getRoleCode());
        if (StringUtils.hasText(queryVo.getPhone())) {
            wrapper.like(UserInfo::getPhone, queryVo.getPhone());
        }
        // 关键词同时匹配「登录账号」和「昵称」，方便直接搜 zhangsan / 张三
        if (StringUtils.hasText(queryVo.getKeyword())) {
            String keyword = queryVo.getKeyword().trim();
            wrapper.and(w -> w.like(UserInfo::getUsername, keyword).or().like(UserInfo::getNickname, keyword));
        }
        wrapper.orderByDesc(UserInfo::getId);
        userInfoService.page(page, wrapper);
        // 不向前端回显密码
        if (page.getRecords() != null) {
            page.getRecords().forEach(user -> user.setPassword(null));
        }
        return Result.ok(page);
    }

    @Operation(summary = "统计：注册用户总数 / 管理员数 / 租客数")
    @GetMapping("statistics")
    @RequirePermission("user:list")
    public Result<java.util.Map<String, Object>> statistics() {
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("total", userInfoService.count());
        map.put("admin", userInfoService.count(
                new LambdaQueryWrapper<UserInfo>().eq(UserInfo::getRoleCode, ROLE_ADMIN)));
        map.put("tenant", userInfoService.count(
                new LambdaQueryWrapper<UserInfo>().eq(UserInfo::getRoleCode, ROLE_TENANT)));
        return Result.ok(map);
    }

    @Operation(summary = "根据用户id更新账号状态")
    @PostMapping("updateStatusById")
    @RequirePermission("user:save")
    public Result updateStatusById(@RequestParam Long id, @RequestParam BaseStatus status) {
        LambdaUpdateWrapper<UserInfo> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(UserInfo::getId, id);
        wrapper.set(UserInfo::getStatus, status);
        userInfoService.update(wrapper);
        return Result.ok();
    }

    @Operation(summary = "设置用户角色：admin-管理员 / tenant-租客")
    @PostMapping("updateRoleById")
    @RequirePermission("user:save")
    public Result updateRoleById(@RequestParam Long id, @RequestParam String roleCode) {
        if (!ROLE_ADMIN.equals(roleCode) && !ROLE_TENANT.equals(roleCode)) {
            throw new LeaseException("角色只能是 admin 或 tenant", ResultCodeEnum.PARAM_ERROR.getCode());
        }
        LambdaUpdateWrapper<UserInfo> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(UserInfo::getId, id);
        wrapper.set(UserInfo::getRoleCode, roleCode);
        userInfoService.update(wrapper);
        return Result.ok();
    }

    @Operation(summary = "重置用户密码为 123456")
    @PostMapping("resetPassword")
    @RequirePermission("user:save")
    public Result resetPassword(@RequestParam Long id) {
        UserInfo user = userInfoService.getById(id);
        if (user == null) {
            throw new LeaseException("用户不存在", ResultCodeEnum.FAIL.getCode());
        }
        LambdaUpdateWrapper<UserInfo> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(UserInfo::getId, id);
        // md5('123456')
        wrapper.set(UserInfo::getPassword, "e10adc3949ba59abbe56e057f20f883e");
        userInfoService.update(wrapper);
        return Result.ok();
    }
}
