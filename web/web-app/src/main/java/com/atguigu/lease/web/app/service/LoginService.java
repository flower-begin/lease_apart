package com.atguigu.lease.web.app.service;

import com.atguigu.lease.web.app.vo.user.LoginVo;
import com.atguigu.lease.web.app.vo.user.RegisterVo;

public interface LoginService {


    void sendSms(String phone) throws Exception;

    String login(LoginVo loginVo);

    /**
     * 账号密码注册。
     *
     * @param registerVo 账号 / 密码 / 手机号 / 昵称
     * @return 注册成功后直接签发 token，前端可以免登录
     */
    String register(RegisterVo registerVo);
}
