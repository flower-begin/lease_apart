package com.atguigu.lease.web.admin.service.impl;

import com.atguigu.lease.common.constants.RedisConstant;
import com.atguigu.lease.common.exception.LeaseException;
import com.atguigu.lease.common.result.ResultCodeEnum;
import com.atguigu.lease.common.utils.JwtUtil;
import com.atguigu.lease.model.entity.SystemUser;
import com.atguigu.lease.model.enums.BaseStatus;
import com.atguigu.lease.web.admin.service.LoginService;
import com.atguigu.lease.web.admin.service.SystemUserService;
import com.atguigu.lease.web.admin.vo.login.CaptchaVo;
import com.atguigu.lease.web.admin.vo.login.LoginVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wf.captcha.SpecCaptcha;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class LoginServiceImpl implements LoginService {

    @Autowired
    private RedisTemplate<String,Object> redisTemplate;

    @Autowired
    private SystemUserService systemUserService;
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

    @Override
    public String login(LoginVo loginVo) {
        // 1.先判断接受的验证码是否为null，不为null继续执行
        if (ObjectUtils.isEmpty(loginVo.getCaptchaCode())){  // 进入if后说明为空
            throw new LeaseException(ResultCodeEnum.ADMIN_CAPTCHA_CODE_NOT_FOUND);
        }
        // 2.根据key判断验证码是否超时/失效
        // 根据前端登录时传进来的key去redis数据库中找到对应的验证码并赋值给redisCode
        String redisCode = (String) redisTemplate.opsForValue().get(loginVo.getCaptchaKey());
        if (ObjectUtils.isEmpty(redisCode)) {  // 进入if说明为空，数据库中没有找到对应的验证码
            throw new LeaseException(ResultCodeEnum.ADMIN_CAPTCHA_CODE_EXPIRED);
        }
        // 3.判断验证码是否正确
        // 现在既没有过期也不为空，进行判断验证码是否正确
        if (!redisCode.equalsIgnoreCase(loginVo.getCaptchaCode())) {  // 进入if说明验证码不正确
            throw new LeaseException(ResultCodeEnum.ADMIN_CAPTCHA_CODE_ERROR);
        }
        // 4.根据用户数据查询数据库
        LambdaQueryWrapper<SystemUser> systemUserLambdaQueryWrapper = new LambdaQueryWrapper<>();
        systemUserLambdaQueryWrapper.eq(SystemUser::getUsername, loginVo.getUsername());
        SystemUser systemUser = systemUserService.getOne(systemUserLambdaQueryWrapper);
        if (systemUser == null) {  // 账号没有查询到
            throw new LeaseException(ResultCodeEnum.ADMIN_ACCOUNT_NOT_EXIST_ERROR);
        }
        // 5.比较密码是否正确
        // 在上方已经查到一位用户了，但是根据这个用户对象得到的密码时已经加过密的
        // 所以前端传进来的也得通过md5加密
        if (!systemUser.getPassword().equals(DigestUtils.md5(loginVo.getPassword()))) {
            throw new LeaseException(ResultCodeEnum.ADMIN_ACCOUNT_ERROR);
        }
        // 6.判断该用户是否被禁用
        if (systemUser.getStatus() == BaseStatus.DISABLE) {
            throw new LeaseException(ResultCodeEnum.ADMIN_ACCOUNT_DISABLED_ERROR);
        }
        // 7.生成token
        String token = JwtUtil.createToken(systemUser.getId(), systemUser.getUsername());
        return token;
    }
}
