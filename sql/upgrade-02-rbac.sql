-- =============================================================================
--  归心寓 · RBAC 角色权限（升级脚本 02）
--
--  用途：给后台管理端加「角色 → 菜单/按钮」权限模型。
--        不破坏任何已有表，只是新增 4 张表 + 种子数据。
--
--  幂等：CREATE TABLE IF NOT EXISTS + INSERT IGNORE，
--        可以重复执行，不会产生重复的角色/菜单/授权关系。
--
--  执行：mysql -uroot -p123456 --default-character-set=utf8mb4 lease < sql/upgrade-02-rbac.sql
-- =============================================================================

SET NAMES utf8mb4;
SET autocommit = 1;

-- -----------------------------------------------------------------------------
-- 1. 角色表
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_role`
(
    `id`          bigint      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`        varchar(50) NOT NULL COMMENT '角色名称',
    `code`        varchar(50) NOT NULL COMMENT '角色编码（super_admin/ops/manager/agent/finance/maintainer）',
    `description` varchar(255)         DEFAULT NULL COMMENT '角色描述',
    `data_scope`  varchar(20) NOT NULL DEFAULT 'ALL' COMMENT '数据范围：ALL-全部 STORE-本门店 SELF-仅本人',
    `sort`        int         NOT NULL DEFAULT 0 COMMENT '排序',
    `status`      tinyint(1)  NOT NULL DEFAULT 1 COMMENT '状态(BaseStatus：0-禁用 1-正常)',
    `create_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`  tinyint(1)  NOT NULL DEFAULT 0 COMMENT '逻辑删除(0-未删除 1-已删除)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '角色表';

-- -----------------------------------------------------------------------------
-- 2. 菜单/权限表
--    type：1-目录 2-菜单 3-按钮；perms 是接口鉴权用的权限串
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_menu`
(
    `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键（种子数据显式指定，便于授权引用）',
    `parent_id`   bigint       NOT NULL DEFAULT 0 COMMENT '父级id，0 为顶级',
    `name`        varchar(50)  NOT NULL COMMENT '菜单名称',
    `type`        tinyint(1)   NOT NULL DEFAULT 2 COMMENT '类型：1-目录 2-菜单 3-按钮',
    `path`        varchar(200)          DEFAULT NULL COMMENT '前端路由路径',
    `component`   varchar(200)          DEFAULT NULL COMMENT '前端组件路径',
    `icon`        varchar(50)           DEFAULT NULL COMMENT 'Element Plus 图标名',
    `perms`       varchar(100)          DEFAULT NULL COMMENT '权限标识，如 apartment:save',
    `sort`        int          NOT NULL DEFAULT 0 COMMENT '排序',
    `visible`     tinyint(1)   NOT NULL DEFAULT 1 COMMENT '是否在菜单中显示(1-显示 0-隐藏)',
    `status`      tinyint(1)   NOT NULL DEFAULT 1 COMMENT '状态(BaseStatus：0-禁用 1-正常)',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`  tinyint(1)   NOT NULL DEFAULT 0 COMMENT '逻辑删除(0-未删除 1-已删除)',
    PRIMARY KEY (`id`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_perms` (`perms`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '菜单/权限表';

-- -----------------------------------------------------------------------------
-- 3. 角色-菜单关联
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_role_menu`
(
    `id`      bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `role_id` bigint NOT NULL COMMENT '角色id',
    `menu_id` bigint NOT NULL COMMENT '菜单id',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_menu` (`role_id`, `menu_id`),
    KEY `idx_role_id` (`role_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '角色菜单关联表';

-- -----------------------------------------------------------------------------
-- 4. 用户-角色关联
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_user_role`
(
    `id`      bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` bigint NOT NULL COMMENT '员工id（system_user.id）',
    `role_id` bigint NOT NULL COMMENT '角色id',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '用户角色关联表';


-- =============================================================================
--  种子数据
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 5. 角色（6 个）
-- -----------------------------------------------------------------------------
INSERT IGNORE INTO `sys_role` (`id`, `name`, `code`, `description`, `data_scope`, `sort`) VALUES
(1, '超级管理员', 'super_admin', '平台最高权限，含账号与权限配置', 'ALL',  1),
(2, '运营专员',   'ops',         '管房源内容、字典、预约；不碰账号与财务', 'ALL',  2),
(3, '门店店长',   'manager',     '管本店房源、租约、工单，可看本店员工业绩', 'STORE', 3),
(4, '经纪人',     'agent',       '只管自己名下的房源、预约、租约', 'SELF', 4),
(5, '财务',       'finance',     '账单、缴费、退款、对账；租约只读', 'ALL',  5),
(6, '维修保洁',   'maintainer',  '只看派给自己的工单', 'SELF', 6);

-- -----------------------------------------------------------------------------
-- 6. 菜单 / 按钮
--    id 段规划：1=首页，10~19=房源，20~29=租务，30~39=客户，
--              40~49=字典，50~59=系统管理；百位是子项，+1/+2 是按钮
-- -----------------------------------------------------------------------------
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `path`, `component`, `icon`, `perms`, `sort`) VALUES
-- 首页
(1,   0,  '首页',             2, '/dashboard',      'dashboard/index',  'Odometer',       'dashboard:view',      1),

-- 房源管理
(10,  0,  '房源管理',         1, '/',               NULL,               'OfficeBuilding', NULL,                 10),
(11,  10, '公寓管理',         2, '/apartment',      'apartment/index',  NULL,             'apartment:list',     11),
(111, 11, '新增/编辑公寓',    3, NULL,              NULL,               NULL,             'apartment:save',      1),
(112, 11, '删除公寓',         3, NULL,              NULL,               NULL,             'apartment:delete',    2),
(12,  10, '房间管理',         2, '/room',           'room/index',       NULL,             'room:list',          12),
(121, 12, '新增/编辑房间',    3, NULL,              NULL,               NULL,             'room:save',           1),
(122, 12, '删除房间',         3, NULL,              NULL,               NULL,             'room:delete',         2),

-- 租务管理
(20,  0,  '租务管理',         1, '/',               NULL,               'Calendar',       NULL,                 20),
(21,  20, '预约看房',         2, '/appointment',    'appointment/index',NULL,             'appointment:list',   21),
(211, 21, '处理预约',         3, NULL,              NULL,               NULL,             'appointment:update',  1),
(22,  20, '租约管理',         2, '/agreement',      'agreement/index',  NULL,             'agreement:list',     22),
(221, 22, '录入/编辑租约',    3, NULL,              NULL,               NULL,             'agreement:save',      1),

-- 客户管理
(30,  0,  '客户管理',         1, '/',               NULL,               'User',           NULL,                 30),
(31,  30, '用户管理',         2, '/user',           'user/index',       NULL,             'user:list',          31),
(311, 31, '编辑用户',         3, NULL,              NULL,               NULL,             'user:save',           1),

-- 字典管理
(40,  0,  '字典管理',         1, '/',               NULL,               'Collection',     NULL,                 40),
(41,  40, '标签管理',         2, '/dict/label',     'dict/label',       NULL,             'dict:label:list',    41),
(42,  40, '配套管理',         2, '/dict/facility',  'dict/facility',    NULL,             'dict:facility:list', 42),
(43,  40, '属性管理',         2, '/dict/attr',      'dict/attr',        NULL,             'dict:attr:list',     43),
(44,  40, '杂费管理',         2, '/dict/fee',       'dict/fee',         NULL,             'dict:fee:list',      44),
(45,  40, '支付方式',         2, '/dict/payment',   'dict/payment',     NULL,             'dict:payment:list',  45),
(46,  40, '租期管理',         2, '/dict/term',      'dict/term',        NULL,             'dict:term:list',     46),
(47,  40, '维护字典项',       3, NULL,              NULL,               NULL,             'dict:save',           1),

-- 系统管理
(50,  0,  '系统管理',         1, '/',               NULL,               'Setting',        NULL,                 50),
(51,  50, '员工管理',         2, '/system/user',    'system/user',      NULL,             'system:user:list',   51),
(511, 51, '新增/编辑员工',    3, NULL,              NULL,               NULL,             'system:user:save',    1),
(512, 51, '分配角色',         3, NULL,              NULL,               NULL,             'system:user:assign',  2),
(52,  50, '岗位管理',         2, '/system/post',    'system/post',      NULL,             'system:post:list',   52),
(53,  50, '角色管理',         2, '/system/role',    'system/role',      NULL,             'system:role:list',   53),
(531, 53, '新增/编辑角色',    3, NULL,              NULL,               NULL,             'system:role:save',    1),
(532, 53, '删除角色',         3, NULL,              NULL,               NULL,             'system:role:delete',  2),
(533, 53, '分配菜单权限',     3, NULL,              NULL,               NULL,             'system:role:assign',  3);

-- -----------------------------------------------------------------------------
-- 7. 角色授权
-- -----------------------------------------------------------------------------

-- 7.1 超级管理员：全部菜单
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, m.`id` FROM `sys_menu` m;

-- 7.2 运营专员：除系统管理外全部
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(2, 1),
(2, 10), (2, 11), (2, 111), (2, 112), (2, 12), (2, 121), (2, 122),
(2, 20), (2, 21), (2, 211), (2, 22),
(2, 30), (2, 31), (2, 311),
(2, 40), (2, 41), (2, 42), (2, 43), (2, 44), (2, 45), (2, 46), (2, 47);

-- 7.3 门店店长：公寓只读、房间可写、租约/预约可写、员工只读
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(3, 1),
(3, 10), (3, 11), (3, 12), (3, 121), (3, 122),
(3, 20), (3, 21), (3, 211), (3, 22), (3, 221),
(3, 30), (3, 31),
(3, 50), (3, 51);

-- 7.4 经纪人：全部只读 + 处理预约
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(4, 1),
(4, 10), (4, 11), (4, 12),
(4, 20), (4, 21), (4, 211), (4, 22),
(4, 30), (4, 31);

-- 7.5 财务：首页 + 租约只读 + 用户只读
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(5, 1),
(5, 20), (5, 22),
(5, 30), (5, 31);

-- 7.6 维修保洁：只有首页
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(6, 1);

-- -----------------------------------------------------------------------------
-- 8. 给现有员工授权（按用户名匹配，重复执行不会重复插）
-- -----------------------------------------------------------------------------
INSERT IGNORE INTO `sys_user_role` (`user_id`, `role_id`)
SELECT u.`id`, r.`id`
FROM `system_user` u
         JOIN `sys_role` r ON r.`code` = 'super_admin'
WHERE u.`username` = 'admin';

INSERT IGNORE INTO `sys_user_role` (`user_id`, `role_id`)
SELECT u.`id`, r.`id`
FROM `system_user` u
         JOIN `sys_role` r ON r.`code` = 'ops'
WHERE u.`username` = 'operator';

INSERT IGNORE INTO `sys_user_role` (`user_id`, `role_id`)
SELECT u.`id`, r.`id`
FROM `system_user` u
         JOIN `sys_role` r ON r.`code` = 'maintainer'
WHERE u.`username` = 'cleaner';

-- 兜底：如果一个角色都没分配到的账号，统一给「超级管理员」，
-- 避免老数据登录后侧边栏空白（如果你不想这样，把这句删掉）
INSERT IGNORE INTO `sys_user_role` (`user_id`, `role_id`)
SELECT u.`id`, 1
FROM `system_user` u
WHERE NOT EXISTS (SELECT 1 FROM `sys_user_role` ur WHERE ur.`user_id` = u.`id`);

-- -----------------------------------------------------------------------------
-- 9. 自查
-- -----------------------------------------------------------------------------
SELECT 'RBAC 初始化结果' AS 阶段,
       (SELECT COUNT(*) FROM `sys_role`)      AS 角色数,
       (SELECT COUNT(*) FROM `sys_menu`)      AS 菜单权限数,
       (SELECT COUNT(*) FROM `sys_role_menu`) AS 角色授权数,
       (SELECT COUNT(*) FROM `sys_user_role`) AS 员工授权数;

SELECT r.`name` AS 角色, COUNT(rm.`menu_id`) AS 可见菜单数
FROM `sys_role` r
         LEFT JOIN `sys_role_menu` rm ON rm.`role_id` = r.`id`
GROUP BY r.`id`, r.`name`
ORDER BY r.`sort`;
