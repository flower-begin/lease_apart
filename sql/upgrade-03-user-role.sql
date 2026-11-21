-- =============================================================================
--  归心寓 · 用户角色 + 菜单调整（升级脚本 03）
--
--  这次调整的三件事：
--    1. user_info 加 role_code：admin-管理员 tenant-租客（C 端用）
--       并在 user_info 里种一个 admin 账号，让管理员能同时登录移动端 / 网页版
--    2. 后台「员工管理」改成「用户管理」，指向注册用户列表（user_info）
--       删掉重复的「客户管理 → 用户管理」分组
--    3. 后台只允许管理员登录（代码里校验 system_user.type = 0，见 LoginServiceImpl）
--
--  幂等：加列用 information_schema 判断，数据用 INSERT ... ON DUPLICATE KEY UPDATE，
--        菜单用 UPDATE/DELETE，可以重复执行。
--
--  执行：mysql -uroot -p123456 --default-character-set=utf8mb4 lease < sql/upgrade-03-user-role.sql
-- =============================================================================

SET NAMES utf8mb4;
SET autocommit = 1;

-- -----------------------------------------------------------------------------
-- 1. user_info 加 role_code（列已存在就跳过）
-- -----------------------------------------------------------------------------
SET @has_role_code = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'user_info'
      AND COLUMN_NAME = 'role_code'
);

SET @ddl = IF(@has_role_code = 0,
    'ALTER TABLE `user_info` ADD COLUMN `role_code` VARCHAR(20) NOT NULL DEFAULT ''tenant'' COMMENT ''角色：admin-管理员 tenant-租客'' AFTER `password`',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 老数据统一归为租客
UPDATE `user_info` SET `role_code` = 'tenant' WHERE `role_code` IS NULL OR `role_code` = '';

-- -----------------------------------------------------------------------------
-- 2. 在 user_info 里种一个管理员账号，让 admin 也能登录移动端 / 网页版
--    密码 123456（MD5 小写）
-- -----------------------------------------------------------------------------
INSERT INTO `user_info` (`username`, `phone`, `password`, `nickname`, `role_code`, `status`)
VALUES ('admin', '13900000000', 'e10adc3949ba59abbe56e057f20f883e', '平台管理员', 'admin', 1)
ON DUPLICATE KEY UPDATE `role_code` = 'admin', `status` = 1;

-- 其余测试账号明确为租客
UPDATE `user_info` SET `role_code` = 'tenant' WHERE `username` IN ('zhangsan', 'lisi', 'wangwu');

-- -----------------------------------------------------------------------------
-- 3. 后台菜单调整
-- -----------------------------------------------------------------------------

-- 3.1 「员工管理」(51) 改成顶级的「用户管理」，指向注册用户页 /user
--     注意 parent_id 要改成 0：用户管理是平台主菜单，不该埋在系统管理里
UPDATE `sys_menu`
SET `name`        = '用户管理',
    `parent_id`   = 0,
    `path`        = '/user',
    `component`   = 'user/index',
    `perms`       = 'user:list',
    `icon`        = 'User',
    `sort`        = 30
WHERE `id` = 51;

-- 3.2 「新增/编辑员工」(511) 改成「编辑用户」
UPDATE `sys_menu`
SET `name`  = '编辑用户',
    `perms` = 'user:save'
WHERE `id` = 511;

-- 3.3 「分配角色」(512) 是给员工用的，员工管理没了就一并去掉
DELETE FROM `sys_role_menu` WHERE `menu_id` = 512;
DELETE FROM `sys_menu` WHERE `id` = 512;

-- 3.4 删掉重复的「客户管理 → 用户管理」分组（30 / 31 / 311），
--     先清角色授权再删菜单，顺序不能反
DELETE FROM `sys_role_menu` WHERE `menu_id` IN (30, 31, 311);
DELETE FROM `sys_menu` WHERE `id` IN (30, 31, 311);

-- 3.5 「系统管理」(50) 下面现在只剩：岗位管理 / 角色管理
UPDATE `sys_menu` SET `sort` = 52 WHERE `id` = 52;
UPDATE `sys_menu` SET `sort` = 53 WHERE `id` = 53;

-- 3.6 超级管理员补齐新菜单（角色 1 直接全量重授权）
DELETE FROM `sys_role_menu` WHERE `role_id` = 1;
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, `id` FROM `sys_menu` WHERE `is_deleted` = 0;

-- -----------------------------------------------------------------------------
-- 4. 自查
-- -----------------------------------------------------------------------------
SELECT 'user_info 角色分布' AS 说明, `role_code` AS 角色, COUNT(*) AS 人数
FROM `user_info`
GROUP BY `role_code`;

SELECT '后台菜单（系统管理下）' AS 说明, `id`, `name` AS 菜单名, `path` AS 路由, `perms` AS 权限
FROM `sys_menu`
WHERE `parent_id` IN (0, 50) AND `is_deleted` = 0
ORDER BY `sort`;
