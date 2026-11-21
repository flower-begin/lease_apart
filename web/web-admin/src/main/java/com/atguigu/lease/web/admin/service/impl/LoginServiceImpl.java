package com.atguigu.lease.web.admin.service.impl;

import com.atguigu.lease.common.cache.CodeCache;
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
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.UUID;

@Service
public class LoginServiceImpl implements LoginService {

    @Autowired
    private CodeCache codeCache;

    @Autowired
    private SystemUserService systemUserService;

    @Override
    public CaptchaVo getCaptcha() {
        // 1. 生成验证码图片与文本
        SpecCaptcha specCaptcha = new SpecCaptcha(130, 48, 4);
        specCaptcha.setCharType(SpecCaptcha.TYPE_DEFAULT);

        // 2. 验证码文本放到缓存（Redis 不可用时自动降级到内存）
        String code = specCaptcha.text().toLowerCase();
        String key = RedisConstant.ADMIN_LOGIN_PREFIX + UUID.randomUUID();
        codeCache.put(key, code, RedisConstant.ADMIN_LOGIN_CAPTCHA_TTL_SEC);

        // 3. 图片转 base64 返回
        return new CaptchaVo(specCaptcha.toBase64(), key);
    }

    @Override
    public String login(LoginVo loginVo) {
        // 1. 验证码非空校验
        if (ObjectUtils.isEmpty(loginVo.getCaptchaCode())) {
            throw new LeaseException(ResultCodeEnum.ADMIN_CAPTCHA_CODE_NOT_FOUND);
        }
        // 2. 根据 key 取缓存的验证码
        String cachedCode = codeCache.get(loginVo.getCaptchaKey());
        if (ObjectUtils.isEmpty(cachedCode)) {
            throw new LeaseException(ResultCodeEnum.ADMIN_CAPTCHA_CODE_EXPIRED);
        }
        // 3. 校验验证码
        if (!cachedCode.equalsIgnoreCase(loginVo.getCaptchaCode())) {
            throw new LeaseException(ResultCodeEnum.ADMIN_CAPTCHA_CODE_ERROR);
        }
        // 验证码一次性使用
        codeCache.remove(loginVo.getCaptchaKey());

        // 4. 查账号
        LambdaQueryWrapper<SystemUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemUser::getUsername, loginVo.getUsername());
        SystemUser systemUser = systemUserService.getOne(wrapper);
        if (systemUser == null) {
            throw new LeaseException(ResultCodeEnum.ADMIN_ACCOUNT_NOT_EXIST_ERROR);
        }

        // 5. 校验密码
        // SystemUserController 保存时用的是 md5Hex，这里必须一致。
        // 注意 DigestUtils.md5(...) 返回 byte[]，与 String 比较恒为 false。
        if (systemUser.getPassword() == null
                || !systemUser.getPassword().equalsIgnoreCase(DigestUtils.md5Hex(loginVo.getPassword()))) {
            throw new LeaseException(ResultCodeEnum.ADMIN_ACCOUNT_ERROR);
        }

        // 6. 状态校验
        if (systemUser.getStatus() == BaseStatus.DISABLE) {
            throw new LeaseException(ResultCodeEnum.ADMIN_ACCOUNT_DISABLED_ERROR);
        }

        // 7. 签发 token
        return JwtUtil.createToken(systemUser.getId(), systemUser.getUsername());
    }
}
