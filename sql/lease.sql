-- =====================================================================================
--  尚庭公寓（lease）数据库初始化脚本  ——  建表 + 初始化数据
--  目标数据库：MySQL 8.0+    字符集：utf8mb4 / utf8mb4_general_ci
--
--  本脚本完全依据仓库代码反推，未凭记忆编写：
--    * 表名、列名、列注释  : model/src/main/java/com/atguigu/lease/model/entity/*.java
--    * 索引、关联关系      : web/web-admin & web/web-app 的 resources/mapper/*.xml
--    * 状态字段取值范围    : model/src/main/java/com/atguigu/lease/model/enums/*.java
--    * create_time/update_time/is_deleted : 实体 BaseEntity + MybatisPlusObjectHandler
--
--  统一约定（所有实体都继承 BaseEntity）：
--    id          bigint  AUTO_INCREMENT  —— @TableId(type = IdType.AUTO)
--    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP  —— insertFill 自动填充
--    update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
--                                                              —— updateFill 自动填充
--    is_deleted  tinyint(1) NOT NULL DEFAULT 0                —— @TableLogic 逻辑删除
--
--  反推过程中发现的、与“课程原始 schema”可能不同的点（以实体为准）：
--    1) city_info.province_id / district_info.city_id 在实体里是 Integer → 这里用 int，
--       而 apartment_info 的 province_id/city_id/district_id 是 Long → bigint。
--    2) apartment_info.latitude 的 @Schema 注释是“经度”、longitude 是“纬度”（与常规命名相反），
--       列注释按实体原样保留。
--    3) system_post.postCode 映射到列 `code`（不是 post_code）。
--    4) view_appointment 必须是“实体表”，不能是关联视图，详见下方第 28 节的说明。
--    5) 不建物理外键：项目使用 @TableLogic 逻辑删除，物理外键会阻塞逻辑删除后的关联写入；
--       所有关联列均已建普通索引（下面每个 CREATE TABLE 的 KEY）。
--
--  ⚠ 本脚本会先 DROP 同名表/视图再重建，可重复执行，但会清空数据，请勿在生产库执行。
-- =====================================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS `lease` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `lease`;


-- =====================================================================================
--  第一部分：建表
-- =====================================================================================

-- -------------------------------------------------------------------------------------
-- 01. apartment_info  公寓信息表                      实体：ApartmentInfo
-- -------------------------------------------------------------------------------------
DROP TABLE IF EXISTS `apartment_info`;
CREATE TABLE `apartment_info`
(
    `id`             bigint        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`           varchar(50)   NOT NULL COMMENT '公寓名称',
    `introduction`   varchar(1000)          DEFAULT NULL COMMENT '公寓介绍',
    `district_id`    bigint                 DEFAULT NULL COMMENT '所处区域id',
    `district_name`  varchar(50)            DEFAULT NULL COMMENT '所处区域名称',
    `city_id`        bigint                 DEFAULT NULL COMMENT '所处城市id',
    `city_name`      varchar(50)            DEFAULT NULL COMMENT '所处城市名称',
    `province_id`    bigint                 DEFAULT NULL COMMENT '所处省份id',
    `province_name`  varchar(50)            DEFAULT NULL COMMENT '所处省份名称',
    `address_detail` varchar(200)           DEFAULT NULL COMMENT '详细地址',
    `latitude`       varchar(50)            DEFAULT NULL COMMENT '经度（实体@Schema原文如此，命名与常规相反）',
    `longitude`      varchar(50)            DEFAULT NULL COMMENT '纬度（实体@Schema原文如此，命名与常规相反）',
    `phone`          varchar(20)            DEFAULT NULL COMMENT '公寓前台电话',
    `is_release`     tinyint(1)    NOT NULL DEFAULT 0 COMMENT '是否发布(ReleaseStatus：0-未发布 1-已发布)',
    `create_time`    datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`     tinyint(1)    NOT NULL DEFAULT 0 COMMENT '逻辑删除(0-未删除 1-已删除)',
    PRIMARY KEY (`id`),
    KEY `idx_province_id` (`province_id`),
    KEY `idx_city_id` (`city_id`),
    KEY `idx_district_id` (`district_id`),
    KEY `idx_is_release` (`is_release`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '公寓信息表';

-- -------------------------------------------------------------------------------------
-- 02. apartment_facility  公寓&配套关联表              实体：ApartmentFacility
-- -------------------------------------------------------------------------------------
DROP TABLE IF EXISTS `apartment_facility`;
CREATE TABLE `apartment_facility`
(
    `id`           bigint     NOT NULL AUTO_INCREMENT COMMENT '主键',
    `apartment_id` bigint     NOT NULL COMMENT '公寓id',
    `facility_id`  bigint     NOT NULL COMMENT '设施id',
    `create_time`  datetime   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  datetime   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`   tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除(0-未删除 1-已删除)',
    PRIMARY KEY (`id`),
    KEY `idx_apartment_id` (`apartment_id`),
    KEY `idx_facility_id` (`facility_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '公寓&配套关联表';

-- -------------------------------------------------------------------------------------
-- 03. apartment_fee_value  公寓&杂费关联表             实体：ApartmentFeeValue
-- -------------------------------------------------------------------------------------
DROP TABLE IF EXISTS `apartment_fee_value`;
CREATE TABLE `apartment_fee_value`
(
    `id`            bigint     NOT NULL AUTO_INCREMENT COMMENT '主键',
    `apartment_id`  bigint     NOT NULL COMMENT '公寓id',
    `fee_value_id`  bigint     NOT NULL COMMENT '收费项value_id',
    `create_time`   datetime   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   datetime   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`    tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除(0-未删除 1-已删除)',
    PRIMARY KEY (`id`),
    KEY `idx_apartment_id` (`apartment_id`),
    KEY `idx_fee_value_id` (`fee_value_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '公寓&杂费关联表';

-- -------------------------------------------------------------------------------------
-- 04. apartment_label  公寓&标签关联表                 实体：ApartmentLabel
-- -------------------------------------------------------------------------------------
DROP TABLE IF EXISTS `apartment_label`;
CREATE TABLE `apartment_label`
(
    `id`           bigint     NOT NULL AUTO_INCREMENT COMMENT '主键',
    `apartment_id` bigint     NOT NULL COMMENT '公寓id',
    `label_id`     bigint     NOT NULL COMMENT '标签id',
    `create_time`  datetime   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  datetime   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`   tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除(0-未删除 1-已删除)',
    PRIMARY KEY (`id`),
    KEY `idx_apartment_id` (`apartment_id`),
    KEY `idx_label_id` (`label_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '公寓&标签关联表';

-- -------------------------------------------------------------------------------------
-- 05. attr_key  房间基本属性表(key)                    实体：AttrKey
-- -------------------------------------------------------------------------------------
DROP TABLE IF EXISTS `attr_key`;
CREATE TABLE `attr_key`
(
    `id`          bigint      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`        varchar(50) NOT NULL COMMENT '属性key（属性名称，如：朝向、楼层、装修）',
    `create_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`  tinyint(1)  NOT NULL DEFAULT 0 COMMENT '逻辑删除(0-未删除 1-已删除)',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '房间基本属性表';

-- -------------------------------------------------------------------------------------
-- 06. attr_value  房间基本属性值表                     实体：AttrValue
-- -------------------------------------------------------------------------------------
DROP TABLE IF EXISTS `attr_value`;
CREATE TABLE `attr_value`
(
    `id`          bigint      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`        varchar(50) NOT NULL COMMENT '属性value',
    `attr_key_id` bigint      NOT NULL COMMENT '对应的属性key_id',
    `create_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`  tinyint(1)  NOT NULL DEFAULT 0 COMMENT '逻辑删除(0-未删除 1-已删除)',
    PRIMARY KEY (`id`),
    KEY `idx_attr_key_id` (`attr_key_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '房间基本属性值表';

-- -------------------------------------------------------------------------------------
-- 07. browsing_history  浏览历史表                     实体：BrowsingHistory
-- -------------------------------------------------------------------------------------
DROP TABLE IF EXISTS `browsing_history`;
CREATE TABLE `browsing_history`
(
    `id`          bigint     NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`     bigint     NOT NULL COMMENT '用户id',
    `room_id`     bigint     NOT NULL COMMENT '房间id',
    `browse_time` datetime   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '浏览时间',
    `create_time` datetime   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`  tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除(0-未删除 1-已删除)',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_room_id` (`room_id`),
    KEY `idx_browse_time` (`browse_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '浏览历史表';

-- -------------------------------------------------------------------------------------
-- 08. city_info  城市信息表                            实体：CityInfo
-- -------------------------------------------------------------------------------------
DROP TABLE IF EXISTS `city_info`;
CREATE TABLE `city_info`
(
    `id`          bigint      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`        varchar(50) NOT NULL COMMENT '城市名称',
    `province_id` int         NOT NULL COMMENT '所属省份id（实体为Integer）',
    `create_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`  tinyint(1)  NOT NULL DEFAULT 0 COMMENT '逻辑删除(0-未删除 1-已删除)',
    PRIMARY KEY (`id`),
    KEY `idx_province_id` (`province_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '城市信息表';

-- -------------------------------------------------------------------------------------
-- 09. district_info  区域信息表                        实体：DistrictInfo
-- -------------------------------------------------------------------------------------
DROP TABLE IF EXISTS `district_info`;
CREATE TABLE `district_info`
(
    `id`          bigint      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`        varchar(50) NOT NULL COMMENT '区域名称',
    `city_id`     int         NOT NULL COMMENT '所属城市id（实体为Integer）',
    `create_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`  tinyint(1)  NOT NULL DEFAULT 0 COMMENT '逻辑删除(0-未删除 1-已删除)',
    PRIMARY KEY (`id`),
    KEY `idx_city_id` (`city_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '区域信息表';

-- -------------------------------------------------------------------------------------
-- 10. facility_info  配套信息表                        实体：FacilityInfo
--     icon 为前端 <van-icon :name="icon"> 使用的 Vant 图标名，不是图片地址
-- -------------------------------------------------------------------------------------
DROP TABLE IF EXISTS `facility_info`;
CREATE TABLE `facility_info`
(
    `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `type`        tinyint(1)   NOT NULL DEFAULT 2 COMMENT '配套所属对象类型(ItemType：1-公寓 2-房间)',
    `name`        varchar(50)  NOT NULL COMMENT '名称',
    `icon`        varchar(100)          DEFAULT NULL COMMENT '图标(Vant 图标名，如 wifi)',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`  tinyint(1)   NOT NULL DEFAULT 0 COMMENT '逻辑删除(0-未删除 1-已删除)',
    PRIMARY KEY (`id`),
    KEY `idx_type` (`type`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '配套信息表';

-- -------------------------------------------------------------------------------------
-- 11. fee_key  杂项费用名称表                          实体：FeeKey
-- -------------------------------------------------------------------------------------
DROP TABLE IF EXISTS `fee_key`;
CREATE TABLE `fee_key`
(
    `id`          bigint      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`        varchar(50) NOT NULL COMMENT '付款项key（费用名称，如：物业费）',
    `create_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`  tinyint(1)  NOT NULL DEFAULT 0 COMMENT '逻辑删除(0-未删除 1-已删除)',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '杂项费用名称表';

-- -------------------------------------------------------------------------------------
-- 12. fee_value  杂项费用值表                          实体：FeeValue
-- -------------------------------------------------------------------------------------
DROP TABLE IF EXISTS `fee_value`;
CREATE TABLE `fee_value`
(
    `id`          bigint      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`        varchar(50) NOT NULL COMMENT '费用value（金额）',
    `unit`        varchar(20)          DEFAULT NULL COMMENT '收费单位',
    `fee_key_id`  bigint      NOT NULL COMMENT '费用所对的fee_key编码',
    `create_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`  tinyint(1)  NOT NULL DEFAULT 0 COMMENT '逻辑删除(0-未删除 1-已删除)',
    PRIMARY KEY (`id`),
    KEY `idx_fee_key_id` (`fee_key_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '杂项费用值表';

-- -------------------------------------------------------------------------------------
-- 13. graph_info  图片信息表                           实体：GraphInfo
-- -------------------------------------------------------------------------------------
DROP TABLE IF EXISTS `graph_info`;
CREATE TABLE `graph_info`
(
    `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`        varchar(100) NOT NULL COMMENT '图片名称',
    `item_type`   tinyint(1)   NOT NULL COMMENT '图片所属对象类型(ItemType：1-公寓 2-房间)',
    `item_id`     bigint       NOT NULL COMMENT '图片所属对象id',
    `url`         varchar(500) NOT NULL COMMENT '图片地址',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`  tinyint(1)   NOT NULL DEFAULT 0 COMMENT '逻辑删除(0-未删除 1-已删除)',
    PRIMARY KEY (`id`),
    KEY `idx_item` (`item_type`, `item_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '图片信息表';

-- -------------------------------------------------------------------------------------
-- 14. label_info  标签信息表                           实体：LabelInfo
-- -------------------------------------------------------------------------------------
DROP TABLE IF EXISTS `label_info`;
CREATE TABLE `label_info`
(
    `id`          bigint      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `type`        tinyint(1)  NOT NULL DEFAULT 2 COMMENT '类型(ItemType：1-公寓标签 2-房间标签)',
    `name`        varchar(50) NOT NULL COMMENT '标签名称',
    `create_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`  tinyint(1)  NOT NULL DEFAULT 0 COMMENT '逻辑删除(0-未删除 1-已删除)',
    PRIMARY KEY (`id`),
    KEY `idx_type` (`type`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '标签信息表';

-- -------------------------------------------------------------------------------------
-- 15. lease_agreement  租约信息表                      实体：LeaseAgreement
-- -------------------------------------------------------------------------------------
DROP TABLE IF EXISTS `lease_agreement`;
CREATE TABLE `lease_agreement`
(
    `id`                    bigint         NOT NULL AUTO_INCREMENT COMMENT '主键',
    `phone`                 varchar(20)             DEFAULT NULL COMMENT '承租人手机号码',
    `name`                  varchar(50)             DEFAULT NULL COMMENT '承租人姓名',
    `identification_number` varchar(50)             DEFAULT NULL COMMENT '承租人身份证号码',
    `apartment_id`          bigint                  DEFAULT NULL COMMENT '签约公寓id',
    `room_id`               bigint                  DEFAULT NULL COMMENT '签约房间id',
    `lease_start_date`      date                    DEFAULT NULL COMMENT '租约开始日期',
    `lease_end_date`        date                    DEFAULT NULL COMMENT '租约结束日期',
    `lease_term_id`         bigint                  DEFAULT NULL COMMENT '租期id',
    `rent`                  decimal(10, 2)          DEFAULT NULL COMMENT '租金（元/月）',
    `deposit`               decimal(10, 2)          DEFAULT NULL COMMENT '押金（元）',
    `payment_type_id`       bigint                  DEFAULT NULL COMMENT '支付类型id',
    `status`                tinyint(1)              DEFAULT NULL COMMENT '租约状态(LeaseStatus：1-签约待确认 2-已签约 3-已取消 4-已到期 5-退租待确认 6-已退租 7-续约待确认)',
    `source_type`           tinyint(1)              DEFAULT NULL COMMENT '租约来源(LeaseSourceType：1-新签 2-续约)',
    `additional_info`       varchar(500)            DEFAULT NULL COMMENT '备注信息',
    `create_time`           datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`           datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`            tinyint(1)     NOT NULL DEFAULT 0 COMMENT '逻辑删除(0-未删除 1-已删除)',
    PRIMARY KEY (`id`),
    KEY `idx_phone` (`phone`),
    KEY `idx_apartment_id` (`apartment_id`),
    KEY `idx_room_id` (`room_id`),
    KEY `idx_status` (`status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '租约信息表';

-- -------------------------------------------------------------------------------------
-- 16. lease_term  租期信息表                           实体：LeaseTerm
-- -------------------------------------------------------------------------------------
DROP TABLE IF EXISTS `lease_term`;
CREATE TABLE `lease_term`
(
    `id`          bigint      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `month_count` int         NOT NULL DEFAULT 1 COMMENT '租期月数',
    `unit`        varchar(20) NOT NULL DEFAULT '月' COMMENT '租期单位',
    `create_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`  tinyint(1)  NOT NULL DEFAULT 0 COMMENT '逻辑删除(0-未删除 1-已删除)',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '租期信息表';

-- -------------------------------------------------------------------------------------
-- 17. payment_type  支付方式表                         实体：PaymentType
-- -------------------------------------------------------------------------------------
DROP TABLE IF EXISTS `payment_type`;
CREATE TABLE `payment_type`
(
    `id`              bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`            varchar(50)  NOT NULL COMMENT '付款方式名称',
    `pay_month_count` int          NOT NULL DEFAULT 1 COMMENT '每次支付租期数',
    `additional_info` varchar(255)          DEFAULT NULL COMMENT '付费说明',
    `create_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`      tinyint(1)   NOT NULL DEFAULT 0 COMMENT '逻辑删除(0-未删除 1-已删除)',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '支付方式表';

-- -------------------------------------------------------------------------------------
-- 18. province_info  省份信息表                        实体：ProvinceInfo
-- -------------------------------------------------------------------------------------
DROP TABLE IF EXISTS `province_info`;
CREATE TABLE `province_info`
(
    `id`          bigint      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`        varchar(50) NOT NULL COMMENT '省份名称',
    `create_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`  tinyint(1)  NOT NULL DEFAULT 0 COMMENT '逻辑删除(0-未删除 1-已删除)',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '省份信息表';

-- -------------------------------------------------------------------------------------
-- 19. room_attr_value  房间&属性值关联表               实体：RoomAttrValue
-- -------------------------------------------------------------------------------------
DROP TABLE IF EXISTS `room_attr_value`;
CREATE TABLE `room_attr_value`
(
    `id`            bigint     NOT NULL AUTO_INCREMENT COMMENT '主键',
    `room_id`       bigint     NOT NULL COMMENT '房间id',
    `attr_value_id` bigint     NOT NULL COMMENT '属性值id',
    `create_time`   datetime   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   datetime   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`    tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除(0-未删除 1-已删除)',
    PRIMARY KEY (`id`),
    KEY `idx_room_id` (`room_id`),
    KEY `idx_attr_value_id` (`attr_value_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '房间&基本属性值关联表';

-- -------------------------------------------------------------------------------------
-- 20. room_facility  房间&配套关联表                   实体：RoomFacility
-- -------------------------------------------------------------------------------------
DROP TABLE IF EXISTS `room_facility`;
CREATE TABLE `room_facility`
(
    `id`          bigint     NOT NULL AUTO_INCREMENT COMMENT '主键',
    `room_id`     bigint     NOT NULL COMMENT '房间id',
    `facility_id` bigint     NOT NULL COMMENT '房间设施id',
    `create_time` datetime   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`  tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除(0-未删除 1-已删除)',
    PRIMARY KEY (`id`),
    KEY `idx_room_id` (`room_id`),
    KEY `idx_facility_id` (`facility_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '房间&配套关联表';

-- -------------------------------------------------------------------------------------
-- 21. room_info  房间信息表                            实体：RoomInfo
-- -------------------------------------------------------------------------------------
DROP TABLE IF EXISTS `room_info`;
CREATE TABLE `room_info`
(
    `id`           bigint         NOT NULL AUTO_INCREMENT COMMENT '主键',
    `room_number`  varchar(50)    NOT NULL COMMENT '房间号',
    `rent`         decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '租金（元/月）',
    `apartment_id` bigint         NOT NULL COMMENT '所属公寓id',
    `is_release`   tinyint(1)     NOT NULL DEFAULT 0 COMMENT '是否发布(ReleaseStatus：0-未发布 1-已发布)',
    `create_time`  datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`   tinyint(1)     NOT NULL DEFAULT 0 COMMENT '逻辑删除(0-未删除 1-已删除)',
    PRIMARY KEY (`id`),
    KEY `idx_apartment_id` (`apartment_id`),
    KEY `idx_rent` (`rent`),
    KEY `idx_is_release` (`is_release`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '房间信息表';

-- -------------------------------------------------------------------------------------
-- 22. room_label  房间&标签关联表                      实体：RoomLabel
-- -------------------------------------------------------------------------------------
DROP TABLE IF EXISTS `room_label`;
CREATE TABLE `room_label`
(
    `id`          bigint     NOT NULL AUTO_INCREMENT COMMENT '主键',
    `room_id`     bigint     NOT NULL COMMENT '房间id',
    `label_id`    bigint     NOT NULL COMMENT '标签id',
    `create_time` datetime   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`  tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除(0-未删除 1-已删除)',
    PRIMARY KEY (`id`),
    KEY `idx_room_id` (`room_id`),
    KEY `idx_label_id` (`label_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '房间&标签关联表';

-- -------------------------------------------------------------------------------------
-- 23. room_lease_term  房间&租期关联表                 实体：RoomLeaseTerm
-- -------------------------------------------------------------------------------------
DROP TABLE IF EXISTS `room_lease_term`;
CREATE TABLE `room_lease_term`
(
    `id`            bigint     NOT NULL AUTO_INCREMENT COMMENT '主键',
    `room_id`       bigint     NOT NULL COMMENT '房间id',
    `lease_term_id` bigint     NOT NULL COMMENT '租期id',
    `create_time`   datetime   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   datetime   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`    tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除(0-未删除 1-已删除)',
    PRIMARY KEY (`id`),
    KEY `idx_room_id` (`room_id`),
    KEY `idx_lease_term_id` (`lease_term_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '房间&租期关联表';

-- -------------------------------------------------------------------------------------
-- 24. room_payment_type  房间&支付方式关联表           实体：RoomPaymentType
-- -------------------------------------------------------------------------------------
DROP TABLE IF EXISTS `room_payment_type`;
CREATE TABLE `room_payment_type`
(
    `id`              bigint     NOT NULL AUTO_INCREMENT COMMENT '主键',
    `room_id`         bigint     NOT NULL COMMENT '房间id',
    `payment_type_id` bigint     NOT NULL COMMENT '支付类型id',
    `create_time`     datetime   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     datetime   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`      tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除(0-未删除 1-已删除)',
    PRIMARY KEY (`id`),
    KEY `idx_room_id` (`room_id`),
    KEY `idx_payment_type_id` (`payment_type_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '房间&支付方式关联表';

-- -------------------------------------------------------------------------------------
-- 25. system_post  岗位信息表                          实体：SystemPost
--     注意：实体字段 postCode 映射到列 `code`
-- -------------------------------------------------------------------------------------
DROP TABLE IF EXISTS `system_post`;
CREATE TABLE `system_post`
(
    `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `code`        varchar(50)  NOT NULL COMMENT '岗位编码',
    `name`        varchar(50)  NOT NULL COMMENT '岗位名称',
    `description` varchar(255)          DEFAULT NULL COMMENT '岗位描述信息',
    `status`      tinyint(1)   NOT NULL DEFAULT 1 COMMENT '岗位状态(BaseStatus：0-禁用 1-正常)',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`  tinyint(1)   NOT NULL DEFAULT 0 COMMENT '逻辑删除(0-未删除 1-已删除)',
    PRIMARY KEY (`id`),
    KEY `idx_code` (`code`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '岗位信息表';

-- -------------------------------------------------------------------------------------
-- 26. system_user  员工（后台用户）信息表              实体：SystemUser
--     password 存的是 MD5 十六进制小写串（32 位），见 SystemUserController#saveOrUpdate
-- -------------------------------------------------------------------------------------
DROP TABLE IF EXISTS `system_user`;
CREATE TABLE `system_user`
(
    `id`              bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`        varchar(50)  NOT NULL COMMENT '用户名',
    `password`        varchar(64)           DEFAULT NULL COMMENT '密码(32位MD5小写十六进制)',
    `name`            varchar(50)           DEFAULT NULL COMMENT '姓名',
    `type`            tinyint(1)   NOT NULL DEFAULT 1 COMMENT '用户类型(SystemUserType：0-管理员 1-普通用户)',
    `phone`           varchar(20)           DEFAULT NULL COMMENT '手机号码',
    `avatar_url`      varchar(500)          DEFAULT NULL COMMENT '头像地址',
    `additional_info` varchar(500)          DEFAULT NULL COMMENT '备注信息',
    `post_id`         bigint                DEFAULT NULL COMMENT '岗位id',
    `status`          tinyint(1)   NOT NULL DEFAULT 1 COMMENT '账号状态(BaseStatus：0-禁用 1-正常)',
    `create_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`      tinyint(1)   NOT NULL DEFAULT 0 COMMENT '逻辑删除(0-未删除 1-已删除)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_phone` (`phone`),
    KEY `idx_post_id` (`post_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '员工信息表';

-- -------------------------------------------------------------------------------------
-- 27. user_info  用户（App 端）信息表                  实体：UserInfo
-- -------------------------------------------------------------------------------------
DROP TABLE IF EXISTS `user_info`;
CREATE TABLE `user_info`
(
    `id`          bigint      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `phone`       varchar(20) NOT NULL COMMENT '手机号码（用做登录用户名）',
    `password`    varchar(64)          DEFAULT NULL COMMENT '密码',
    `avatar_url`  varchar(500)         DEFAULT NULL COMMENT '头像url',
    `nickname`    varchar(50)          DEFAULT NULL COMMENT '昵称',
    `status`      tinyint(1)  NOT NULL DEFAULT 1 COMMENT '账号状态(BaseStatus：0-禁用 1-正常)',
    `create_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`  tinyint(1)  NOT NULL DEFAULT 0 COMMENT '逻辑删除(0-未删除 1-已删除)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_phone` (`phone`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '用户信息表';

-- -------------------------------------------------------------------------------------
-- 28. view_appointment  预约看房信息表                 实体：ViewAppointment
--
--  【重要】这里是实体表，不是视图，原因（有代码依据，不是猜的）：
--    * 实体 ViewAppointment 继承 BaseEntity，自带 create_time/update_time/is_deleted；
--    * App 端 web-app/.../controller/appointment/ViewAppointmentController.java:32
--      调用 saveOrUpdate(viewAppointment) → 会生成 INSERT INTO view_appointment；
--    * 后台 web-admin/.../controller/lease/ViewAppointmentController.java:41
--      调用 update(...) → 会生成 UPDATE view_appointment SET appointment_status = ?；
--    * Mapper XML 里都是 select ... from view_appointment va
--      （web-app/.../ViewAppointmentMapper.xml:17,25、web-admin/.../ViewAppointmentMapper.xml:22）。
--    MySQL 的多表关联视图不可写（不能 INSERT/UPDATE），所以本表必须是实体表，
--    否则「预约看房」功能直接报错。
--    需要“预约 + 公寓 + 用户”的联表结果时，用本脚本末尾的只读视图 view_appointment_detail。
--    另外：实体里没有 room_id，预约只精确到公寓，因此该视图没有关联 room_info（会产生行膨胀）。
-- -------------------------------------------------------------------------------------
DROP TABLE IF EXISTS `view_appointment`;
CREATE TABLE `view_appointment`
(
    `id`                 bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`            bigint                DEFAULT NULL COMMENT '用户id',
    `name`               varchar(50)           DEFAULT NULL COMMENT '用户姓名',
    `phone`              varchar(20)           DEFAULT NULL COMMENT '用户手机号码',
    `apartment_id`       bigint                DEFAULT NULL COMMENT '公寓id',
    `appointment_time`   datetime              DEFAULT NULL COMMENT '预约时间',
    `additional_info`    varchar(500)          DEFAULT NULL COMMENT '备注信息',
    `appointment_status` tinyint(1)   NOT NULL DEFAULT 1 COMMENT '预约状态(AppointmentStatus：1-待看房 2-已取消 3-已看房)',
    `create_time`        datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`        datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`         tinyint(1)   NOT NULL DEFAULT 0 COMMENT '逻辑删除(0-未删除 1-已删除)',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_apartment_id` (`apartment_id`),
    KEY `idx_appointment_status` (`appointment_status`),
    KEY `idx_appointment_time` (`appointment_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '预约看房信息表';


-- =====================================================================================
--  第二部分：初始化数据
--  说明：id 显式指定，方便前端/接口对照；未写的列走 DEFAULT（create_time/update_time/
--        is_deleted 均由数据库默认值兜底，行为与 MyBatis-Plus 自动填充一致）。
-- =====================================================================================

-- -------------------------------------------------------------------------------------
-- 2.1 省 / 市 / 区（真实行政区划：北京、上海、广东、浙江）
-- -------------------------------------------------------------------------------------
INSERT INTO `province_info` (`id`, `name`)
VALUES (1, '北京市'),
       (2, '上海市'),
       (3, '广东省'),
       (4, '浙江省');

INSERT INTO `city_info` (`id`, `name`, `province_id`)
VALUES (1, '北京市', 1),
       (2, '上海市', 2),
       (3, '广州市', 3),
       (4, '深圳市', 3),
       (5, '珠海市', 3),
       (6, '杭州市', 4),
       (7, '宁波市', 4);

INSERT INTO `district_info` (`id`, `name`, `city_id`)
VALUES (1, '朝阳区', 1),
       (2, '海淀区', 1),
       (3, '昌平区', 1),
       (4, '丰台区', 1),
       (5, '浦东新区', 2),
       (6, '徐汇区', 2),
       (7, '静安区', 2),
       (8, '天河区', 3),
       (9, '番禺区', 3),
       (10, '南山区', 4),
       (11, '福田区', 4),
       (12, '宝安区', 4),
       (13, '香洲区', 5),
       (14, '西湖区', 6),
       (15, '滨江区', 6),
       (16, '鄞州区', 7);

-- -------------------------------------------------------------------------------------
-- 2.2 标签（type：1-公寓 2-房间，ItemType）
-- -------------------------------------------------------------------------------------
INSERT INTO `label_info` (`id`, `type`, `name`)
VALUES (1, 1, '近地铁'),
       (2, 1, '精装修'),
       (3, 1, '拎包入住'),
       (4, 2, '独立卫生间'),
       (5, 2, '朝南'),
       (6, 2, '采光好'),
       (7, 2, '带阳台'),
       (8, 1, '可短租');

-- -------------------------------------------------------------------------------------
-- 2.3 配套（type：1-公寓 2-房间；icon 为 Vant 图标名）
-- -------------------------------------------------------------------------------------
INSERT INTO `facility_info` (`id`, `type`, `name`, `icon`)
VALUES (1, 2, 'WiFi', 'wifi'),
       (2, 2, '空调', 'setting-o'),
       (3, 2, '洗衣机', 'apps-o'),
       (4, 2, '冰箱', 'shop-o'),
       (5, 2, '热水器', 'fire-o'),
       (6, 1, '健身房', 'award-o'),
       (7, 1, '停车场', 'guide-o'),
       (8, 1, '电梯', 'cluster-o');

-- -------------------------------------------------------------------------------------
-- 2.4 房间属性 key / value
-- -------------------------------------------------------------------------------------
INSERT INTO `attr_key` (`id`, `name`)
VALUES (1, '朝向'),
       (2, '楼层'),
       (3, '装修'),
       (4, '面积');

INSERT INTO `attr_value` (`id`, `name`, `attr_key_id`)
VALUES (1, '朝南', 1),
       (2, '朝北', 1),
       (3, '朝东', 1),
       (4, '低楼层', 2),
       (5, '中楼层', 2),
       (6, '高楼层', 2),
       (7, '精装修', 3),
       (8, '简装修', 3),
       (9, '25㎡', 4),
       (10, '35㎡', 4);

-- -------------------------------------------------------------------------------------
-- 2.5 杂项费用 key / value
-- -------------------------------------------------------------------------------------
INSERT INTO `fee_key` (`id`, `name`)
VALUES (1, '物业费'),
       (2, '车位费'),
       (3, '宽带费'),
       (4, '保洁费');

INSERT INTO `fee_value` (`id`, `name`, `unit`, `fee_key_id`)
VALUES (1, '300', '元/月', 1),
       (2, '400', '元/月', 1),
       (3, '200', '元/月', 2),
       (4, '100', '元/月', 3),
       (5, '80', '元/次', 4),
       (6, '500', '元/月', 1);

-- -------------------------------------------------------------------------------------
-- 2.6 支付方式 / 租期
-- -------------------------------------------------------------------------------------
INSERT INTO `payment_type` (`id`, `name`, `pay_month_count`, `additional_info`)
VALUES (1, '按月支付', 1, '每月支付一次租金，押一付一'),
       (2, '按季度支付', 3, '每季度支付一次租金，押一付三'),
       (3, '按半年支付', 6, '每半年支付一次租金，押金为1个月租金'),
       (4, '按年支付', 12, '每年支付一次租金，押金为1个月租金');

INSERT INTO `lease_term` (`id`, `month_count`, `unit`)
VALUES (1, 1, '月'),
       (2, 3, '月'),
       (3, 6, '月'),
       (4, 12, '月'),
       (5, 24, '月');

-- -------------------------------------------------------------------------------------
-- 2.7 岗位 / 员工（后台用户）
--     密码 123456 的 MD5 小写十六进制 = e10adc3949ba59abbe56e057f20f883e
--     与 SystemUserController#saveOrUpdate 的 DigestUtils.md5Hex(...) 结果一致。
--     ⚠ 注意：web-admin 的 LoginServiceImpl#login 第 76 行用的是 DigestUtils.md5(...)，
--       该方法返回 byte[]，与 String 比较恒为 false，所以 admin 账号目前登录必然失败；
--       需要把该行改成 DigestUtils.md5Hex(loginVo.getPassword()) 才能登录成功。
--       （数据库侧无需改动，存的仍是 md5Hex 格式）
-- -------------------------------------------------------------------------------------
INSERT INTO `system_post` (`id`, `code`, `name`, `description`, `status`)
VALUES (1, 'ADMIN', '管理员', '拥有后台管理系统全部权限', 1),
       (2, 'OPERATOR', '运营专员', '负责公寓、房间、租约信息维护', 1),
       (3, 'CLEANER', '保洁', '负责房源保洁工作', 1);

INSERT INTO `system_user` (`id`, `username`, `password`, `name`, `type`, `phone`, `avatar_url`, `additional_info`,
                           `post_id`, `status`)
VALUES (1, 'admin', 'e10adc3949ba59abbe56e057f20f883e', '系统管理员', 0, '13900000000',
        'https://picsum.photos/seed/lease-admin/200/200', '超级管理员，密码 123456', 1, 1),
       (2, 'operator', 'e10adc3949ba59abbe56e057f20f883e', '李运营', 1, '13900000001',
        'https://picsum.photos/seed/lease-operator/200/200', '负责天河、南山片区', 2, 1),
       (3, 'cleaner', 'e10adc3949ba59abbe56e057f20f883e', '王保洁', 1, '13900000002',
        'https://picsum.photos/seed/lease-cleaner/200/200', '负责望京、中关村片区', 3, 1);

-- -------------------------------------------------------------------------------------
-- 2.8 App 端用户（手机号即登录名，App 端用短信验证码登录，password 仅占位）
-- -------------------------------------------------------------------------------------
INSERT INTO `user_info` (`id`, `phone`, `password`, `avatar_url`, `nickname`, `status`)
VALUES (1, '13800000001', 'e10adc3949ba59abbe56e057f20f883e', 'https://picsum.photos/seed/lease-user1/200/200', '张三', 1),
       (2, '13800000002', 'e10adc3949ba59abbe56e057f20f883e', 'https://picsum.photos/seed/lease-user2/200/200', '李四', 1),
       (3, '13800000003', 'e10adc3949ba59abbe56e057f20f883e', 'https://picsum.photos/seed/lease-user3/200/200', '王五', 1);

-- -------------------------------------------------------------------------------------
-- 2.9 公寓（id 1~6，全部已发布）
--     latitude=经度、longitude=纬度（按实体 @Schema 的注释填）
-- -------------------------------------------------------------------------------------
INSERT INTO `apartment_info` (`id`, `name`, `introduction`, `district_id`, `district_name`, `city_id`, `city_name`,
                              `province_id`, `province_name`, `address_detail`, `latitude`, `longitude`, `phone`,
                              `is_release`)
VALUES (1, '望京SOHO公寓', '位于望京核心商圈，紧邻地铁14号线望京站，步行约10分钟，周边餐饮商超齐全，适合国贸、望京上班族。',
        1, '朝阳区', 1, '北京市', 1, '北京市', '北京市朝阳区望京街10号院', '116.4757', '39.9968', '010-64701234', 1),
       (2, '中关村人才公寓', '中关村大街旁，紧邻地铁4号线中关村站，周边高校与互联网公司云集，配套完善，拎包入住。',
        2, '海淀区', 1, '北京市', 1, '北京市', '北京市海淀区中关村大街27号', '116.3157', '39.9836', '010-62881234', 1),
       (3, '回龙观青年公寓', '回龙观东大街地铁8号线旁，整租开间为主，租金友好，社区配套成熟，适合刚毕业的年轻人。',
        3, '昌平区', 1, '北京市', 1, '北京市', '北京市昌平区回龙观东大街338号', '116.3368', '40.0759', '010-80761234', 1),
       (4, '陆家嘴滨江公寓', '地处陆家嘴金融贸易区，江景房源，地铁2号线陆家嘴站步行5分钟，安防与物业服务完善。',
        5, '浦东新区', 2, '上海市', 2, '上海市', '上海市浦东新区陆家嘴环路1000号', '121.5057', '31.2397', '021-58881234', 1),
       (5, '天河体育中心公寓', '天河体育中心商圈，地铁1号线、3号线双线可达，商圈成熟，生活便利，房型多样。',
        8, '天河区', 3, '广州市', 3, '广东省', '广州市天河区天河路299号', '113.3245', '23.1353', '020-38881234', 1),
       (6, '深圳湾科技公寓', '深圳湾科技生态园旁，地铁2号线科苑站步行8分钟，面向科技园上班族，智能门锁与全屋家电。',
        10, '南山区', 4, '深圳市', 3, '广东省', '深圳市南山区科苑南路2666号', '113.9437', '22.5177', '0755-86661234', 1);

-- -------------------------------------------------------------------------------------
-- 2.10 房间（id 1~12，每个公寓 2 间，全部已发布）
-- -------------------------------------------------------------------------------------
INSERT INTO `room_info` (`id`, `room_number`, `rent`, `apartment_id`, `is_release`)
VALUES (1, '101', 2500.00, 1, 1),
       (2, '102', 2800.00, 1, 1),
       (3, '201', 3000.00, 2, 1),
       (4, '202', 3200.00, 2, 1),
       (5, '301', 2200.00, 3, 1),
       (6, '302', 2400.00, 3, 1),
       (7, '401', 4500.00, 4, 1),
       (8, '402', 4800.00, 4, 1),
       (9, '501', 3300.00, 5, 1),
       (10, '502', 3600.00, 5, 1),
       (11, '601', 4200.00, 6, 1),
       (12, '602', 4600.00, 6, 1);

-- -------------------------------------------------------------------------------------
-- 2.11 公寓与 配套 / 标签 / 杂费 的关联
-- -------------------------------------------------------------------------------------
INSERT INTO `apartment_facility` (`id`, `apartment_id`, `facility_id`)
VALUES (1, 1, 6), (2, 1, 7), (3, 1, 8),
       (4, 2, 6), (5, 2, 7), (6, 2, 8),
       (7, 3, 7), (8, 3, 8),
       (9, 4, 6), (10, 4, 7), (11, 4, 8),
       (12, 5, 7), (13, 5, 8),
       (14, 6, 6), (15, 6, 7), (16, 6, 8);

INSERT INTO `apartment_label` (`id`, `apartment_id`, `label_id`)
VALUES (1, 1, 1), (2, 1, 2), (3, 1, 3),
       (4, 2, 1), (5, 2, 2), (6, 2, 3),
       (7, 3, 1), (8, 3, 2), (9, 3, 8),
       (10, 4, 1), (11, 4, 3),
       (12, 5, 1), (13, 5, 2),
       (14, 6, 2), (15, 6, 3);

INSERT INTO `apartment_fee_value` (`id`, `apartment_id`, `fee_value_id`)
VALUES (1, 1, 1), (2, 1, 3), (3, 1, 4),
       (4, 2, 1), (5, 2, 3),
       (6, 3, 1), (7, 3, 3), (8, 3, 5),
       (9, 4, 1), (10, 4, 2), (11, 4, 3),
       (12, 5, 1), (13, 5, 3),
       (14, 6, 1), (15, 6, 3), (16, 6, 4);

-- -------------------------------------------------------------------------------------
-- 2.12 房间与 标签 / 配套 / 属性值 / 支付方式 / 租期 的关联
-- -------------------------------------------------------------------------------------
INSERT INTO `room_label` (`id`, `room_id`, `label_id`)
VALUES (1, 1, 4), (2, 1, 5),
       (3, 2, 4), (4, 2, 6),
       (5, 3, 5), (6, 3, 7),
       (7, 4, 4), (8, 4, 5),
       (9, 5, 6),
       (10, 6, 4), (11, 6, 7),
       (12, 7, 5), (13, 7, 6),
       (14, 8, 4), (15, 8, 5),
       (16, 9, 5), (17, 9, 7),
       (18, 10, 4), (19, 10, 6),
       (20, 11, 5), (21, 11, 6),
       (22, 12, 4), (23, 12, 5);

-- 每个房间都配齐 房间类配套（facility_info.type = 2，即 id 1~5）
INSERT INTO `room_facility` (`room_id`, `facility_id`)
SELECT `r`.`id`, `f`.`id`
FROM `room_info` AS `r`
         CROSS JOIN `facility_info` AS `f`
WHERE `f`.`type` = 2
  AND `f`.`is_deleted` = 0
ORDER BY `r`.`id`, `f`.`id`;

-- 属性值：朝向按房间 id 轮询 朝南/朝北/朝东，楼层 低/中/高，装修 精装/简装
INSERT INTO `room_attr_value` (`room_id`, `attr_value_id`)
SELECT `r`.`id`, 1 + MOD(`r`.`id`, 3)
FROM `room_info` AS `r`
ORDER BY `r`.`id`;

INSERT INTO `room_attr_value` (`room_id`, `attr_value_id`)
SELECT `r`.`id`, 4 + MOD(`r`.`id`, 3)
FROM `room_info` AS `r`
ORDER BY `r`.`id`;

INSERT INTO `room_attr_value` (`room_id`, `attr_value_id`)
SELECT `r`.`id`, 7 + MOD(`r`.`id`, 2)
FROM `room_info` AS `r`
ORDER BY `r`.`id`;

-- 支付方式：每个房间支持 按月 / 按季度 / 按半年（payment_type id 1~3）
INSERT INTO `room_payment_type` (`room_id`, `payment_type_id`)
SELECT `r`.`id`, `p`.`id`
FROM `room_info` AS `r`
         CROSS JOIN `payment_type` AS `p`
WHERE `p`.`is_deleted` = 0
  AND `p`.`id` IN (1, 2, 3)
ORDER BY `r`.`id`, `p`.`id`;

-- 租期：每个房间支持 1 / 3 / 6 / 12 个月
INSERT INTO `room_lease_term` (`room_id`, `lease_term_id`)
SELECT `r`.`id`, `lt`.`id`
FROM `room_info` AS `r`
         CROSS JOIN `lease_term` AS `lt`
WHERE `lt`.`is_deleted` = 0
  AND `lt`.`month_count` IN (1, 3, 6, 12)
ORDER BY `r`.`id`, `lt`.`month_count`;

-- -------------------------------------------------------------------------------------
-- 2.13 图片（item_type：1-公寓 2-房间）
--      url 使用 picsum 占位图，可换成自己 upload 目录里的图片地址
-- -------------------------------------------------------------------------------------
INSERT INTO `graph_info` (`id`, `name`, `item_type`, `item_id`, `url`)
VALUES (1, '望京SOHO公寓-客厅', 1, 1, 'https://picsum.photos/seed/lease-apt1-1/800/600'),
       (2, '望京SOHO公寓-卧室', 1, 1, 'https://picsum.photos/seed/lease-apt1-2/800/600'),
       (3, '中关村人才公寓-客厅', 1, 2, 'https://picsum.photos/seed/lease-apt2-1/800/600'),
       (4, '中关村人才公寓-卧室', 1, 2, 'https://picsum.photos/seed/lease-apt2-2/800/600'),
       (5, '回龙观青年公寓-客厅', 1, 3, 'https://picsum.photos/seed/lease-apt3-1/800/600'),
       (6, '回龙观青年公寓-卧室', 1, 3, 'https://picsum.photos/seed/lease-apt3-2/800/600'),
       (7, '陆家嘴滨江公寓-客厅', 1, 4, 'https://picsum.photos/seed/lease-apt4-1/800/600'),
       (8, '陆家嘴滨江公寓-卧室', 1, 4, 'https://picsum.photos/seed/lease-apt4-2/800/600'),
       (9, '天河体育中心公寓-客厅', 1, 5, 'https://picsum.photos/seed/lease-apt5-1/800/600'),
       (10, '天河体育中心公寓-卧室', 1, 5, 'https://picsum.photos/seed/lease-apt5-2/800/600'),
       (11, '深圳湾科技公寓-客厅', 1, 6, 'https://picsum.photos/seed/lease-apt6-1/800/600'),
       (12, '深圳湾科技公寓-卧室', 1, 6, 'https://picsum.photos/seed/lease-apt6-2/800/600'),
       (13, '101房间', 2, 1, 'https://picsum.photos/seed/lease-room1/800/600'),
       (14, '102房间', 2, 2, 'https://picsum.photos/seed/lease-room2/800/600'),
       (15, '201房间', 2, 3, 'https://picsum.photos/seed/lease-room3/800/600'),
       (16, '202房间', 2, 4, 'https://picsum.photos/seed/lease-room4/800/600'),
       (17, '301房间', 2, 5, 'https://picsum.photos/seed/lease-room5/800/600'),
       (18, '302房间', 2, 6, 'https://picsum.photos/seed/lease-room6/800/600'),
       (19, '401房间', 2, 7, 'https://picsum.photos/seed/lease-room7/800/600'),
       (20, '402房间', 2, 8, 'https://picsum.photos/seed/lease-room8/800/600'),
       (21, '501房间', 2, 9, 'https://picsum.photos/seed/lease-room9/800/600'),
       (22, '502房间', 2, 10, 'https://picsum.photos/seed/lease-room10/800/600'),
       (23, '601房间', 2, 11, 'https://picsum.photos/seed/lease-room11/800/600'),
       (24, '602房间', 2, 12, 'https://picsum.photos/seed/lease-room12/800/600');

-- -------------------------------------------------------------------------------------
-- 2.14 租约（status：1-签约待确认 2-已签约 3-已取消 4-已到期 5-退租待确认 6-已退租 7-续约待确认）
--      后台“房间列表”的入住状态 = status in (2,5,7)
-- -------------------------------------------------------------------------------------
INSERT INTO `lease_agreement` (`id`, `phone`, `name`, `identification_number`, `apartment_id`, `room_id`,
                              `lease_start_date`, `lease_end_date`, `lease_term_id`, `rent`, `deposit`,
                              `payment_type_id`, `status`, `source_type`, `additional_info`)
VALUES (1, '13800000001', '张三', '110101199001011234', 1, 1, '2025-01-01', '2025-06-30', 3, 2500.00, 2500.00, 1, 2, 1,
        '首次签约，押一付一'),
       (2, '13800000002', '李四', '310101199202022345', 2, 3, '2025-03-01', '2026-02-28', 4, 3000.00, 3000.00, 2, 2, 1,
        '年付优惠，赠送1个月物业费'),
       (3, '13800000003', '王五', '440101199303033456', 5, 9, '2025-02-01', '2025-07-31', 3, 3300.00, 3300.00, 1, 2, 1,
        '短租半年'),
       (4, '13800000001', '张三', '110101199001011234', 4, 7, '2025-08-01', '2026-07-31', 4, 4500.00, 4500.00, 2, 1, 1,
        '续租意向已提交，等待签约确认');

-- -------------------------------------------------------------------------------------
-- 2.15 浏览历史
-- -------------------------------------------------------------------------------------
INSERT INTO `browsing_history` (`id`, `user_id`, `room_id`, `browse_time`)
VALUES (1, 1, 1, '2025-07-01 10:05:00'),
       (2, 1, 2, '2025-07-01 10:12:31'),
       (3, 2, 3, '2025-07-02 11:30:02'),
       (4, 3, 9, '2025-07-04 14:20:45'),
       (5, 2, 7, '2025-07-05 09:03:18');

-- -------------------------------------------------------------------------------------
-- 2.16 预约看房（appointment_status：1-待看房 2-已取消 3-已看房）
-- -------------------------------------------------------------------------------------
INSERT INTO `view_appointment` (`id`, `user_id`, `name`, `phone`, `apartment_id`, `appointment_time`, `additional_info`,
                                `appointment_status`)
VALUES (1, 1, '张三', '13800000001', 1, '2025-07-10 10:00:00', '想重点看101房间，周末方便', 1),
       (2, 2, '李四', '13800000002', 2, '2025-07-11 15:00:00', '希望了解物业费与停车位', 1),
       (3, 3, '王五', '13800000003', 3, '2025-07-05 09:30:00', '已看房，房源满意，考虑中', 3),
       (4, 1, '张三', '13800000001', 4, '2025-07-06 16:00:00', '临时有事取消预约', 2);


-- =====================================================================================
--  第三部分：只读视图 view_appointment_detail
--  预约 + 公寓 + 用户 的联表结果，字段覆盖 ViewAppointment 实体的全部字段。
--  说明：view_appointment 表里没有 room_id（实体 ViewAppointment 只有 apartment_id），
--        预约粒度是“公寓”，因此该视图不关联 room_info，避免一间公寓多个房间导致行膨胀；
--        房间信息请直接用 room_info（带 apartment_id）。
-- =====================================================================================
CREATE OR REPLACE VIEW `view_appointment_detail` AS
SELECT `va`.`id`                 AS `id`,
       `va`.`user_id`            AS `user_id`,
       `va`.`name`               AS `name`,
       `va`.`phone`              AS `phone`,
       `va`.`apartment_id`       AS `apartment_id`,
       `va`.`appointment_time`   AS `appointment_time`,
       `va`.`additional_info`    AS `additional_info`,
       `va`.`appointment_status` AS `appointment_status`,
       `va`.`create_time`        AS `create_time`,
       `va`.`update_time`        AS `update_time`,
       `va`.`is_deleted`         AS `is_deleted`,
       `ai`.`name`               AS `apartment_name`,
       `ai`.`province_id`        AS `province_id`,
       `ai`.`province_name`      AS `province_name`,
       `ai`.`city_id`            AS `city_id`,
       `ai`.`city_name`          AS `city_name`,
       `ai`.`district_id`        AS `district_id`,
       `ai`.`district_name`      AS `district_name`,
       `ai`.`address_detail`     AS `address_detail`,
       `ai`.`phone`              AS `apartment_phone`,
       `ai`.`is_release`         AS `apartment_is_release`,
       `ui`.`nickname`           AS `user_nickname`,
       `ui`.`avatar_url`         AS `user_avatar_url`,
       `ui`.`phone`              AS `user_phone`
FROM `view_appointment` AS `va`
         LEFT JOIN `apartment_info` AS `ai` ON `va`.`apartment_id` = `ai`.`id` AND `ai`.`is_deleted` = 0
         LEFT JOIN `user_info` AS `ui` ON `va`.`user_id` = `ui`.`id` AND `ui`.`is_deleted` = 0;


SET FOREIGN_KEY_CHECKS = 1;

-- =====================================================================================
--  初始化完成
--  可选的校验语句（手动执行，确认数据是否齐备）：
--    SELECT COUNT(*) FROM province_info;  -- 4
--    SELECT COUNT(*) FROM city_info;      -- 7
--    SELECT COUNT(*) FROM district_info;  -- 16
--    SELECT COUNT(*) FROM apartment_info; -- 6
--    SELECT COUNT(*) FROM room_info;      -- 12
--    SELECT COUNT(*) FROM view_appointment_detail; -- 4
-- =====================================================================================
