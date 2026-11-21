package com.atguigu.lease.web.app.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 租客端只需要「查这个账号是不是后台员工」这一个能力。
 * <p>
 * 为什么要查：C 端和后台是两套账号体系（user_info / system_user），
 * 用户拿后台账号来租客端登录时，如果只提示「账号或密码错误」，
 * 会以为是自己密码打错了。这里查一下，给个明确的引导。
 */
@Mapper
public interface SystemUserMapper {

    @Select("SELECT `username` FROM `system_user` " +
            "WHERE `username` = #{username} AND `is_deleted` = 0 LIMIT 1")
    String selectUsernameByUsername(@Param("username") String username);
}
