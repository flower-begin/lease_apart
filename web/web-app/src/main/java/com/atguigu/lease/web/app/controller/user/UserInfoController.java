package com.atguigu.lease.web.app.controller.user;

import com.atguigu.lease.common.context.LoginUserContext;
import com.atguigu.lease.common.exception.LeaseException;
import com.atguigu.lease.common.result.Result;
import com.atguigu.lease.common.result.ResultCodeEnum;
import com.atguigu.lease.model.entity.UserInfo;
import com.atguigu.lease.web.app.service.FileService;
import com.atguigu.lease.web.app.service.UserInfoService;
import com.atguigu.lease.web.app.vo.user.UserInfoVo;
import com.atguigu.lease.web.app.vo.user.UserSubmitVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Tag(name = "用户信息")
@RequestMapping("/app/user")
public class UserInfoController {

    @Autowired
    private UserInfoService userInfoService;

    @Autowired
    private FileService fileService;

    @Operation(summary = "获取登录用户信息")
    @GetMapping("info")
    public Result<UserInfoVo> info() {
        UserInfo user = currentUser();
        return Result.ok(new UserInfoVo(user.getNickname(), user.getAvatarUrl(), user.getPhone()));
    }

    @Operation(summary = "修改昵称和头像")
    @PutMapping("update")
    public Result<UserInfoVo> update(@RequestBody UserSubmitVo submitVo) {
        UserInfo user = currentUser();

        if (submitVo.getNickname() != null) {
            String nickname = submitVo.getNickname().trim();
            if (nickname.isEmpty() || nickname.length() > 20) {
                throw new LeaseException("昵称长度需在 1~20 个字符之间", ResultCodeEnum.PARAM_ERROR.getCode());
            }
            user.setNickname(nickname);
        }
        if (StringUtils.hasText(submitVo.getAvatarUrl())) {
            user.setAvatarUrl(submitVo.getAvatarUrl());
        }

        userInfoService.updateById(user);
        return Result.ok(new UserInfoVo(user.getNickname(), user.getAvatarUrl(), user.getPhone()));
    }

    @Operation(summary = "上传头像，返回图片 URL")
    @PostMapping("upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        return Result.ok(fileService.upload(file));
    }

    private UserInfo currentUser() {
        Long userId = LoginUserContext.getUserId();
        if (userId == null) {
            throw new LeaseException(ResultCodeEnum.APP_LOGIN_AUTH);
        }
        UserInfo user = userInfoService.getById(userId);
        if (user == null) {
            throw new LeaseException(ResultCodeEnum.APP_LOGIN_AUTH);
        }
        return user;
    }
}
