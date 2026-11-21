package com.atguigu.lease.web.app.service.impl;


import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.atguigu.lease.common.cache.CodeCache;
import com.atguigu.lease.common.constants.RedisConstant;
import com.atguigu.lease.common.exception.LeaseException;
import com.atguigu.lease.common.result.ResultCodeEnum;
import com.atguigu.lease.common.utils.JwtUtil;
import com.atguigu.lease.common.utils.Md5Util;
import com.atguigu.lease.model.entity.UserInfo;
import com.atguigu.lease.model.enums.BaseStatus;
import com.atguigu.lease.web.app.service.LoginService;
import com.atguigu.lease.web.app.service.UserInfoService;
import com.atguigu.lease.web.app.vo.user.LoginVo;
import com.atguigu.lease.web.app.vo.user.RegisterVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
public class LoginServiceImpl implements LoginService {

    @Autowired
    private CodeCache codeCache;

    @Autowired
    private UserInfoService userInfoService;

    /** 阿里云短信客户端。mock 模式下不依赖它，所以允许不存在。 */
    @Autowired(required = false)
    private Client smsClient;

    /** true：不真正发短信，验证码打到日志里（本地开发默认）。 */
    @Value("${lease.sms.mock:true}")
    private boolean smsMock;

    /** mock 模式下可以固定一个验证码，方便复制粘贴；留空则随机 6 位。 */
    @Value("${lease.sms.fixed-code:}")
    private String fixedCode;

    @Value("${lease.sms.sign-name:阿里云短信测试}")
    private String smsSignName;

    @Value("${lease.sms.template-code:SMS_154950909}")
    private String smsTemplateCode;

    private static final java.util.regex.Pattern PHONE_PATTERN =
            java.util.regex.Pattern.compile("^1[3-9]\\d{9}$");

    /** 登录账号：3~20 位字母、数字或下划线 */
    private static final java.util.regex.Pattern USERNAME_PATTERN =
            java.util.regex.Pattern.compile("^[A-Za-z0-9_]{3,20}$");

    @Override
    public void sendSms(String phone) throws Exception {
        if (!StringUtils.hasText(phone)) {
            throw new LeaseException(ResultCodeEnum.APP_LOGIN_PHONE_EMPTY);
        }
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            throw new LeaseException("手机号格式不正确", ResultCodeEnum.PARAM_ERROR.getCode());
        }
        String cacheKey = RedisConstant.APP_LOGIN_PREFIX + phone;

        // 1. 判断验证码是否还在有效期内
        String exists = codeCache.get(cacheKey);
        if (!ObjectUtils.isEmpty(exists)) {
            if (smsMock) {
                // 本地开发别把人卡死：直接把还在有效期内的验证码再打一遍，不抛"发送过于频繁"
                log.info("【短信 mock】{} 的验证码仍在有效期，直接复用：{}", phone, exists);
                return;
            }
            throw new LeaseException(ResultCodeEnum.APP_SEND_SMS_TOO_OFTEN);
        }

        // 2. 生成验证码
        String code = StringUtils.hasText(fixedCode)
                ? fixedCode
                : String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));

        // 3. 写入缓存（Redis 不可用时自动降级到内存）
        codeCache.put(cacheKey, code, RedisConstant.APP_LOGIN_CODE_TTL_SEC);

        // 4. 发送
        if (smsMock || smsClient == null) {
            log.info("【短信 mock】手机号 {} 的登录验证码是：{} （{} 秒内有效）", phone, code, RedisConstant.APP_LOGIN_CODE_TTL_SEC);
            return;
        }
        SendSmsRequest smsRequest = new SendSmsRequest();
        smsRequest.setPhoneNumbers(phone);
        smsRequest.setSignName(smsSignName);
        smsRequest.setTemplateCode(smsTemplateCode);
        smsRequest.setTemplateParam("{\"code\":\"" + code + "\"}");
        var smsResponse = smsClient.sendSms(smsRequest);
        if (smsResponse == null || smsResponse.getBody() == null
                || !"OK".equalsIgnoreCase(smsResponse.getBody().getCode())) {
            String detail = smsResponse == null || smsResponse.getBody() == null
                    ? "无响应"
                    : smsResponse.getBody().getCode() + " " + smsResponse.getBody().getMessage();
            throw new LeaseException("短信发送失败：" + detail, ResultCodeEnum.SERVICE_ERROR.getCode());
        }
    }

    /**
     * 登录入口，支持两种方式，按传参自动区分：
     * <p>
     * 1. <b>账号 + 密码</b>（网页版 / 移动端默认）：传 {@code username + password}
     * 2. 手机号 + 短信验证码（保留兼容）：传 {@code phone + code}
     * <p>
     * 只要带了 username 或 password，就走账号密码登录。
     */
    @Override
    public String login(LoginVo loginVo) {
        if (loginVo == null) {
            throw new LeaseException(ResultCodeEnum.APP_LOGIN_ACCOUNT_EMPTY);
        }

        if (StringUtils.hasText(loginVo.getUsername()) || StringUtils.hasText(loginVo.getPassword())) {
            return loginByAccount(loginVo);
        }
        return loginBySms(loginVo);
    }

    /**
     * 账号 + 密码登录。密码在库里存的是 MD5，这里同样用 MD5 比对。
     * 不走自动注册：密码登录必须账号已存在。
     */
    private String loginByAccount(LoginVo loginVo) {
        if (!StringUtils.hasText(loginVo.getUsername())) {
            throw new LeaseException(ResultCodeEnum.APP_LOGIN_ACCOUNT_EMPTY);
        }
        if (!StringUtils.hasText(loginVo.getPassword())) {
            throw new LeaseException(ResultCodeEnum.APP_LOGIN_PASSWORD_EMPTY);
        }

        LambdaQueryWrapper<UserInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserInfo::getUsername, loginVo.getUsername().trim());
        UserInfo userInfo = userInfoService.getOne(wrapper);

        // 账号不存在 / 没设过密码 / 密码错，统一提示，避免暴露账号是否存在
        if (userInfo == null
                || !StringUtils.hasText(userInfo.getPassword())
                || !Md5Util.matches(loginVo.getPassword(), userInfo.getPassword())) {
            throw new LeaseException(ResultCodeEnum.APP_LOGIN_ACCOUNT_ERROR);
        }

        if (userInfo.getStatus() != BaseStatus.ENABLE) {
            throw new LeaseException(ResultCodeEnum.APP_ACCOUNT_DISABLED_ERROR);
        }

        log.info("账号 {} 密码登录成功（userId={}）", loginVo.getUsername(), userInfo.getId());
        return JwtUtil.createToken(userInfo.getId(), userInfo.getPhone());
    }

    /**
     * 账号密码注册。校验通过后直接签发 token，前端可以注册完就进站。
     * <p>
     * 账号、手机号都必须全局唯一（数据库上也有 uk_username / uk_phone 唯一索引兜底）。
     */
    @Override
    public String register(RegisterVo registerVo) {
        if (registerVo == null) {
            throw new LeaseException("注册信息不能为空", ResultCodeEnum.PARAM_ERROR.getCode());
        }

        // ---------- 1. 参数校验 ----------
        String username = registerVo.getUsername() == null ? "" : registerVo.getUsername().trim();
        String password = registerVo.getPassword() == null ? "" : registerVo.getPassword();
        String phone = registerVo.getPhone() == null ? "" : registerVo.getPhone().trim();

        if (!StringUtils.hasText(username)) {
            throw new LeaseException("请输入账号", ResultCodeEnum.APP_LOGIN_ACCOUNT_EMPTY.getCode());
        }
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new LeaseException("账号需为 3~20 位字母、数字或下划线",
                    ResultCodeEnum.PARAM_ERROR.getCode());
        }
        if (!StringUtils.hasText(password)) {
            throw new LeaseException("请输入密码", ResultCodeEnum.APP_LOGIN_PASSWORD_EMPTY.getCode());
        }
        if (password.length() < 6 || password.length() > 20) {
            throw new LeaseException("密码长度需为 6~20 位", ResultCodeEnum.PARAM_ERROR.getCode());
        }
        if (!StringUtils.hasText(phone)) {
            throw new LeaseException("请输入手机号", ResultCodeEnum.APP_LOGIN_PHONE_EMPTY.getCode());
        }
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            throw new LeaseException("手机号格式不正确", ResultCodeEnum.PARAM_ERROR.getCode());
        }

        // ---------- 2. 唯一性校验 ----------
        LambdaQueryWrapper<UserInfo> byUsername = new LambdaQueryWrapper<>();
        byUsername.eq(UserInfo::getUsername, username);
        if (userInfoService.count(byUsername) > 0) {
            throw new LeaseException(ResultCodeEnum.APP_REGISTER_USERNAME_EXIST);
        }

        LambdaQueryWrapper<UserInfo> byPhone = new LambdaQueryWrapper<>();
        byPhone.eq(UserInfo::getPhone, phone);
        if (userInfoService.count(byPhone) > 0) {
            throw new LeaseException(ResultCodeEnum.APP_REGISTER_PHONE_EXIST);
        }

        // ---------- 3. 落库 ----------
        UserInfo userInfo = new UserInfo();
        userInfo.setUsername(username);
        userInfo.setPhone(phone);
        userInfo.setPassword(Md5Util.md5Hex(password));
        userInfo.setNickname(StringUtils.hasText(registerVo.getNickname())
                ? registerVo.getNickname().trim()
                : username);
        // 头像留空，由各端用自带的默认头像兜底
        userInfo.setAvatarUrl(null);
        userInfo.setStatus(BaseStatus.ENABLE);
        userInfoService.save(userInfo);

        log.info("新用户注册成功：{}（手机号 {}）", username, phone);

        // ---------- 4. 直接签发 token ----------
        return JwtUtil.createToken(userInfo.getId(), userInfo.getPhone());
    }

    /**
     * 手机号 + 短信验证码登录（保留）。第一次用某个手机号登录会自动注册。
     */
    private String loginBySms(LoginVo loginVo) {
        if (!StringUtils.hasText(loginVo.getPhone())) {
            throw new LeaseException(ResultCodeEnum.APP_LOGIN_PHONE_EMPTY);
        }
        if (!PHONE_PATTERN.matcher(loginVo.getPhone()).matches()) {
            throw new LeaseException("手机号格式不正确", ResultCodeEnum.PARAM_ERROR.getCode());
        }
        if (!StringUtils.hasText(loginVo.getCode())) {
            throw new LeaseException(ResultCodeEnum.APP_LOGIN_CODE_EMPTY);
        }
        String cacheKey = RedisConstant.APP_LOGIN_PREFIX + loginVo.getPhone();

        // 1. 校验验证码
        String code = codeCache.get(cacheKey);
        if (code == null) {
            throw new LeaseException(ResultCodeEnum.APP_LOGIN_CODE_EXPIRED);
        }
        if (!code.equals(loginVo.getCode())) {
            throw new LeaseException(ResultCodeEnum.APP_LOGIN_CODE_ERROR);
        }
        // 验证码一次性使用
        codeCache.remove(cacheKey);

        // 2. 查询手机号是否存在，不存在则自动注册
        LambdaQueryWrapper<UserInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserInfo::getPhone, loginVo.getPhone());
        UserInfo userInfo = userInfoService.getOne(wrapper);
        if (userInfo == null) {
            userInfo = new UserInfo();
            userInfo.setPhone(loginVo.getPhone());
            // username 是 NOT NULL + 唯一，自动注册时用手机号兜底，
            // 之后也能用这个号做账号密码登录
            userInfo.setUsername(loginVo.getPhone());
            userInfo.setAvatarUrl("https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg");
            userInfo.setNickname("用户" + loginVo.getPhone().substring(7));
            userInfo.setStatus(BaseStatus.ENABLE);
            userInfoService.save(userInfo);
        }

        // 3. 检查用户是否可用
        if (userInfo.getStatus() != BaseStatus.ENABLE) {
            throw new LeaseException(ResultCodeEnum.APP_ACCOUNT_DISABLED_ERROR);
        }

        // 4. 创建 token 并返回
        return JwtUtil.createToken(userInfo.getId(), userInfo.getPhone());
    }
}
