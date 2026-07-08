package com.atguigu.lease.web.admin.service.impl;

import com.atguigu.lease.common.constants.RedisConstant;
import com.atguigu.lease.web.admin.service.LoginService;
import com.atguigu.lease.web.admin.vo.login.CaptchaVo;
import com.wf.captcha.SpecCaptcha;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class LoginServiceImpl implements LoginService {

    @Autowired
    private RedisTemplate<String,Object> redisTemplate;
    @Override
    public CaptchaVo getCaptcha() {
        // 1.先生成验证码图片以及正确的验证码
        // 参数1：宽的像素  参数2：高的像素  参数3：验证码图片中字符的数量
        SpecCaptcha specCaptcha = new SpecCaptcha(130, 48, 4);
        specCaptcha.setCharType(SpecCaptcha.TYPE_DEFAULT);
        // 2.将验证码存储到redis中
        String code = specCaptcha.text().toLowerCase();
        String key = RedisConstant.ADMIN_LOGIN_PREFIX + UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(key, code, RedisConstant.ADMIN_LOGIN_CAPTCHA_TTL_SEC, TimeUnit.SECONDS);
        // 3.将图片和key存到对应的vo中返回即可
        // 将图片转成base64字符串
        String img = specCaptcha.toBase64();
        CaptchaVo captchaVo = new CaptchaVo(img, key);
        return captchaVo;
    }
}
