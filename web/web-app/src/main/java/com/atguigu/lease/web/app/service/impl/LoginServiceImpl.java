package com.atguigu.lease.web.app.service.impl;


import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.atguigu.lease.common.constants.RedisConstant;
import com.atguigu.lease.common.exception.LeaseException;
import com.atguigu.lease.common.result.ResultCodeEnum;
import com.atguigu.lease.common.utils.JwtUtil;
import com.atguigu.lease.model.entity.UserInfo;
import com.atguigu.lease.model.enums.BaseStatus;
import com.atguigu.lease.web.app.service.LoginService;
import com.atguigu.lease.web.app.service.UserInfoService;
import com.atguigu.lease.web.app.vo.user.LoginVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.concurrent.TimeUnit;

@Service
public class LoginServiceImpl implements LoginService {
    @Autowired
    private RedisTemplate<String,Object> redisTemplate;

    @Autowired
    private Client client;

    @Override
    public void sendSms(String phone) throws Exception {
        // 1.先判断验证码在不在有效期内
        Object code = redisTemplate.opsForValue().get(RedisConstant.APP_LOGIN_PREFIX + phone);
        if (!ObjectUtils.isEmpty(code)) {
            throw new LeaseException(ResultCodeEnum.APP_SEND_SMS_TOO_OFTEN);
        }
        // 随机生成一个6位的数字
        int randomNum = (int) (Math.random() * 900000) + 100000;
        // 2.将正确的验证码存储到Redis中
        redisTemplate.opsForValue().set(RedisConstant.APP_LOGIN_PREFIX + phone, String.valueOf(randomNum), RedisConstant.APP_LOGIN_CODE_TTL_SEC, TimeUnit.SECONDS);

        // 3.不在Redis中就可以发送短信验证码
        SendSmsRequest smsRequest = new SendSmsRequest();
        smsRequest.setPhoneNumbers(phone);
        smsRequest.setSignName("阿里云短信测试");
        smsRequest.setTemplateCode("SMS_154950909");
        smsRequest.setTemplateParam("{\"code\":\"" + randomNum + "\"}\n");

        client.sendSms(smsRequest);
    }

    @Autowired
    private UserInfoService userInfoService;
    @Override
    public String login(LoginVo loginVo) {
        // 1.校验验证码
        Object code = redisTemplate.opsForValue().get(RedisConstant.APP_LOGIN_PREFIX + loginVo.getPhone());
        if (loginVo.getCode().equals(code)) {
            throw new LeaseException(ResultCodeEnum.APP_LOGIN_CODE_EXPIRED);
        }
        // 将前端传入的验证码与Redis中的验证码进行比较
        if (!code.toString().equals(loginVo.getCode())) {
            throw new LeaseException(ResultCodeEnum.APP_LOGIN_CODE_ERROR);
        }
        // 2.查询手机号是否存在
        LambdaQueryWrapper<UserInfo> userInfoLambdaQueryWrapper = new LambdaQueryWrapper<>();
        userInfoLambdaQueryWrapper.eq(UserInfo::getPhone, loginVo.getPhone());
        UserInfo userInfo = userInfoService.getOne(userInfoLambdaQueryWrapper);
        if (userInfo == null) {
            // 手机号没有注册过
            userInfo = new UserInfo();
            userInfo.setPhone(loginVo.getPhone());
            userInfo.setAvatarUrl("http://www.baidu.com/img/bd_logo.png");
            userInfo.setNickname("佚名" + loginVo.getPhone().substring(7));
            userInfo.setStatus(BaseStatus.ENABLE);
            userInfoService.save(userInfo);
        }
        // 3.检查用户是否可用
        if (userInfo.getStatus() != BaseStatus.ENABLE) {
            throw new LeaseException(ResultCodeEnum.APP_ACCOUNT_DISABLED_ERROR);
        }
        // 4.创建token并返回
        String token = JwtUtil.createToken(userInfo.getId(),userInfo.getPhone());
        return token;
    }
}
