-- =====================================================================================
--  升级脚本 01：给 user_info 加「登录账号」字段
--
--  背景：
--    原来的登录方式是「手机号 + 密码 / 手机号 + 验证码」，手机号同时充当登录名。
--    现在改成「账号 + 密码」，所以要单独加一个 username 字段。
--
--  执行方式（任选其一）：
--    · Navicat / DataGrip 连到 lease 库，把这个文件内容整体执行
--    · 命令行： mysql -uroot -p lease < sql/upgrade-01-add-username.sql
--
--  ⚠️ 这个脚本只需要执行一次。如果第二次执行会报
--     "Duplicate column name 'username'"，说明已经加过了，忽略即可。
-- =====================================================================================

-- 1. 加字段（放在 id 后面，便于查看）
ALTER TABLE `user_info`
    ADD COLUMN `username` VARCHAR(50) DEFAULT NULL COMMENT '登录账号' AFTER `id`;

-- 2. 给种子用户设置账号
UPDATE `user_info` SET `username` = 'zhangsan' WHERE `phone` = '13800000001';
UPDATE `user_info` SET `username` = 'lisi'     WHERE `phone` = '13800000002';
UPDATE `user_info` SET `username` = 'wangwu'   WHERE `phone` = '13800000003';

-- 3. 其余没有账号的用户，用手机号兜底（保证 username 不为空，也能登录）
UPDATE `user_info` SET `username` = `phone`
WHERE `username` IS NULL OR `username` = '';

-- 4. 设成 NOT NULL + 唯一索引（登录账号必须唯一）
ALTER TABLE `user_info`
    MODIFY COLUMN `username` VARCHAR(50) NOT NULL COMMENT '登录账号';

ALTER TABLE `user_info`
    ADD UNIQUE KEY `uk_username` (`username`);

-- 5. 确认结果
-- SELECT id, username, phone, nickname, password FROM user_info;
--   期望：1 zhangsan 13800000001 张三 e10adc3949ba59abbe56e057f20f883e
--        2 lisi     13800000002 李四 e10adc3949ba59abbe56e057f20f883e
--        3 wangwu   13800000003 王五 e10adc3949ba59abbe56e057f20f883e
