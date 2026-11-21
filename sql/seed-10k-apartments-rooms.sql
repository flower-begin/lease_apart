-- =============================================================================
--  归心寓 · 批量房源数据（公寓 1000 + 房间 9000 = 1 万条）
--
--  为什么要用 SQL 造数据：
--    1 万条 INSERT VALUES 手写不现实（文件两三兆），这里用「数字表 + INSERT ... SELECT」
--    让 MySQL 自己生成，整个脚本只有几 KB。
--
--  幂等：每一张表都是「先造到临时表 → 按业务键剔掉库里已有的 → 再插入」，
--        所以跑几遍都不会重复。想重来就先把 tmp_* 相关的数据删掉（见文件末尾）。
--
--  依赖：不需要提前导省市区。有 district 就用真实省市区，没有就回退到「朝阳区/北京市」。
--
--  执行：mysql -uroot -p123456 --default-character-set=utf8mb4 lease < sql/seed-10k-apartments-rooms.sql
--        预计 5~20 秒（生成 5 万行左右）
-- =============================================================================

SET NAMES utf8mb4;
SET autocommit = 1;

SELECT '生成前' AS 阶段,
       (SELECT COUNT(*) FROM `apartment_info`) AS 公寓,
       (SELECT COUNT(*) FROM `room_info`)      AS 房间;

-- -----------------------------------------------------------------------------
-- 0. 清理上次残留的临时表
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `tmp_d10`;
DROP TABLE IF EXISTS `tmp_seq`;
DROP TABLE IF EXISTS `tmp_word`;
DROP TABLE IF EXISTS `tmp_pick`;
DROP TABLE IF EXISTS `tmp_new_apt`;
DROP TABLE IF EXISTS `tmp_new_room`;
DROP TABLE IF EXISTS `tmp_new_graph`;
DROP TABLE IF EXISTS `tmp_new_alabel`;
DROP TABLE IF EXISTS `tmp_new_rfac`;

-- -----------------------------------------------------------------------------
-- 1. 数字表 1..10000（10 × 10 × 10 × 10 交叉连接）
-- -----------------------------------------------------------------------------
CREATE TABLE `tmp_d10` (`i` INT PRIMARY KEY);
INSERT INTO `tmp_d10` (`i`) VALUES (0),(1),(2),(3),(4),(5),(6),(7),(8),(9);

CREATE TABLE `tmp_seq` (`n` INT PRIMARY KEY);
INSERT INTO `tmp_seq` (`n`)
SELECT a.`i` + b.`i` * 10 + c.`i` * 100 + d.`i` * 1000 + 1
FROM `tmp_d10` a, `tmp_d10` b, `tmp_d10` c, `tmp_d10` d;

-- -----------------------------------------------------------------------------
-- 2. 名字词库（用来拼「北京星辰公寓」「梧桐路」这种像样的名字）
-- -----------------------------------------------------------------------------
CREATE TABLE `tmp_word` (`k` INT PRIMARY KEY, `w` VARCHAR(20) NOT NULL);
INSERT INTO `tmp_word` (`k`, `w`) VALUES
(0, '星辰'), (1, '悦居'), (2, '栖舍'), (3, '暖家'), (4, '云栖'),
(5, '静园'), (6, '梧桐'), (7, '拾光'), (8, '归心'), (9, '南山');

-- -----------------------------------------------------------------------------
-- 3. 地区池：把「区县 → 城市 → 省份」拍平成一行，用行号取模轮换
--    没有导省市区数据时，回退成「朝阳区 / 北京市 / 北京市」
-- -----------------------------------------------------------------------------
CREATE TABLE `tmp_pick` (
  `rn`            INT PRIMARY KEY,
  `district_id`   BIGINT,
  `district_name` VARCHAR(50),
  `city_id`       BIGINT,
  `city_name`     VARCHAR(50),
  `province_id`   BIGINT,
  `province_name` VARCHAR(50)
);

INSERT INTO `tmp_pick`
SELECT ROW_NUMBER() OVER (ORDER BY d.`id`),
       d.`id`, d.`name`, c.`id`, c.`name`, p.`id`, p.`name`
FROM `district_info` d
JOIN `city_info` c     ON c.`id` = d.`city_id` AND c.`is_deleted` = 0
JOIN `province_info` p ON p.`id` = c.`province_id` AND p.`is_deleted` = 0
WHERE d.`is_deleted` = 0;

INSERT INTO `tmp_pick` (`rn`, `district_id`, `district_name`, `city_id`, `city_name`, `province_id`, `province_name`)
SELECT 1, 1, '朝阳区', 1, '北京市', 1, '北京市'
WHERE NOT EXISTS (SELECT 1 FROM `tmp_pick`);

SET @region_total = (SELECT COUNT(*) FROM `tmp_pick`);

-- -----------------------------------------------------------------------------
-- 4. 生成 1000 个公寓（先落到临时表，方便后面按名字关联房间）
-- -----------------------------------------------------------------------------
CREATE TABLE `tmp_new_apt` (
  `seq`            INT PRIMARY KEY,
  `name`           VARCHAR(50)  NOT NULL,
  `introduction`   VARCHAR(1000),
  `district_id`    BIGINT,
  `district_name`  VARCHAR(50),
  `city_id`        BIGINT,
  `city_name`      VARCHAR(50),
  `province_id`    BIGINT,
  `province_name`  VARCHAR(50),
  `address_detail` VARCHAR(200),
  `latitude`       VARCHAR(50),
  `longitude`      VARCHAR(50),
  `phone`          VARCHAR(20),
  `is_release`     TINYINT      NOT NULL
);

INSERT INTO `tmp_new_apt`
SELECT s.`n`,
       CONCAT(REPLACE(t.`city_name`, '市', ''), w.`w`, '公寓', s.`n`),
       CONCAT('位于', t.`district_name`, '核心地段，步行 5 分钟到地铁站。',
              w.`w`, '公寓专注长租运营，房间精装交付，周边商超、餐饮、医院配套齐全，',
              '24 小时管家服务，支持押一付一与月付。'),
       t.`district_id`, t.`district_name`,
       t.`city_id`, t.`city_name`,
       t.`province_id`, t.`province_name`,
       CONCAT(t.`district_name`, w.`w`, '路', MOD(s.`n` * 37, 300) + 1, '号'),
       CONCAT('3', MOD(s.`n` * 7, 9) + 1, '.', LPAD(MOD(s.`n` * 11, 9999), 4, '0')),
       CONCAT('11', MOD(s.`n` * 13, 9) + 1, '.', LPAD(MOD(s.`n` * 17, 9999), 4, '0')),
       CONCAT('0', MOD(s.`n`, 900) + 100, '-', LPAD(MOD(s.`n` * 29, 10000), 4, '0')),
       IF(MOD(s.`n`, 13) = 0, 0, 1)
FROM `tmp_seq` s
         JOIN `tmp_pick` t ON t.`rn` = MOD(s.`n` - 1, @region_total) + 1
         JOIN `tmp_word` w ON w.`k` = MOD(s.`n` - 1, 10)
WHERE s.`n` <= 1000;

-- 名字已经存在的就不重复插入
DELETE x FROM `tmp_new_apt` x
    JOIN `apartment_info` a ON a.`name` = x.`name`;

INSERT INTO `apartment_info`
(`name`, `introduction`, `district_id`, `district_name`, `city_id`, `city_name`,
 `province_id`, `province_name`, `address_detail`, `latitude`, `longitude`,
 `phone`, `is_release`)
SELECT `name`, `introduction`, `district_id`, `district_name`, `city_id`, `city_name`,
       `province_id`, `province_name`, `address_detail`, `latitude`, `longitude`,
       `phone`, `is_release`
FROM `tmp_new_apt`;

-- -----------------------------------------------------------------------------
-- 5. 生成 9000 个房间（每个公寓 9 间：101/102/103/201/.../303）
-- -----------------------------------------------------------------------------
CREATE TABLE `tmp_new_room` (
  `seq`         INT PRIMARY KEY,
  `apt_name`    VARCHAR(50)   NOT NULL,
  `room_number` VARCHAR(50)   NOT NULL,
  `rent`        DECIMAL(10, 2) NOT NULL,
  `is_release`  TINYINT       NOT NULL
);

INSERT INTO `tmp_new_room`
SELECT s.`n`,
       na.`name`,
       CONCAT(CAST(CEIL(((s.`n` - 1) DIV 1000 + 1) / 3) AS UNSIGNED),
              LPAD(MOD((s.`n` - 1) DIV 1000, 3) + 1, 2, '0')),
       ROUND(900 + MOD(s.`n` * 137, 4200), 2),
       IF(MOD(s.`n`, 17) = 0, 0, 1)
FROM `tmp_seq` s
         JOIN `tmp_new_apt` na ON na.`seq` = MOD(s.`n` - 1, 1000) + 1
WHERE s.`n` <= 9000;

-- 公寓名 + 房间号 已经存在的剔掉
DELETE x FROM `tmp_new_room` x
    JOIN `apartment_info` a ON a.`name` = x.`apt_name`
    JOIN `room_info` r ON r.`apartment_id` = a.`id` AND r.`room_number` = x.`room_number`;

INSERT INTO `room_info` (`room_number`, `rent`, `apartment_id`, `is_release`)
SELECT x.`room_number`, x.`rent`, a.`id`, x.`is_release`
FROM `tmp_new_room` x
         JOIN `apartment_info` a ON a.`name` = x.`apt_name`;

-- -----------------------------------------------------------------------------
-- 6. 图片：公寓 2 张封面 + 房间 1 张
-- -----------------------------------------------------------------------------
CREATE TABLE `tmp_new_graph` (
  `name`      VARCHAR(100) NOT NULL,
  `item_type` TINYINT      NOT NULL,
  `item_id`   BIGINT       NOT NULL,
  `url`       VARCHAR(500) NOT NULL
);

INSERT INTO `tmp_new_graph`
SELECT CONCAT(a.`name`, '-封面', k.`k`), 1, a.`id`,
       CONCAT('https://picsum.photos/seed/apt', a.`id` * 10 + k.`k`, '/800/600')
FROM `tmp_new_apt` na
         JOIN `apartment_info` a ON a.`name` = na.`name`
         CROSS JOIN (SELECT 1 AS k UNION ALL SELECT 2) k;

INSERT INTO `tmp_new_graph`
SELECT CONCAT(r.`room_number`, '-房间图'), 2, r.`id`,
       CONCAT('https://picsum.photos/seed/room', r.`id`, '/800/600')
FROM `tmp_new_room` nr
         JOIN `apartment_info` a ON a.`name` = nr.`apt_name`
         JOIN `room_info` r ON r.`apartment_id` = a.`id` AND r.`room_number` = nr.`room_number`;

DELETE x FROM `tmp_new_graph` x
    JOIN `graph_info` g
    ON g.`item_type` = x.`item_type` AND g.`item_id` = x.`item_id` AND g.`name` = x.`name`;

INSERT INTO `graph_info` (`name`, `item_type`, `item_id`, `url`)
SELECT `name`, `item_type`, `item_id`, `url` FROM `tmp_new_graph`;

-- -----------------------------------------------------------------------------
-- 7. 公寓标签（类型1 的标签：近地铁 / 精装修 / 拎包入住）
-- -----------------------------------------------------------------------------
CREATE TABLE `tmp_new_alabel` (`apartment_id` BIGINT NOT NULL, `label_id` BIGINT NOT NULL);

INSERT INTO `tmp_new_alabel`
SELECT a.`id`, l.`lid`
FROM `tmp_new_apt` na
         JOIN `apartment_info` a ON a.`name` = na.`name`
         CROSS JOIN (SELECT 1 AS lid UNION ALL SELECT 2 UNION ALL SELECT 3) l;

DELETE x FROM `tmp_new_alabel` x
    JOIN `apartment_label` t ON t.`apartment_id` = x.`apartment_id` AND t.`label_id` = x.`label_id`;

INSERT INTO `apartment_label` (`apartment_id`, `label_id`)
SELECT `apartment_id`, `label_id` FROM `tmp_new_alabel`;

-- -----------------------------------------------------------------------------
-- 8. 房间配套（WiFi / 空调 / 洗衣机）
-- -----------------------------------------------------------------------------
CREATE TABLE `tmp_new_rfac` (`room_id` BIGINT NOT NULL, `facility_id` BIGINT NOT NULL);

INSERT INTO `tmp_new_rfac`
SELECT r.`id`, f.`fid`
FROM `tmp_new_room` nr
         JOIN `apartment_info` a ON a.`name` = nr.`apt_name`
         JOIN `room_info` r ON r.`apartment_id` = a.`id` AND r.`room_number` = nr.`room_number`
         CROSS JOIN (SELECT 1 AS fid UNION ALL SELECT 2 UNION ALL SELECT 3) f;

DELETE x FROM `tmp_new_rfac` x
    JOIN `room_facility` t ON t.`room_id` = x.`room_id` AND t.`facility_id` = x.`facility_id`;

INSERT INTO `room_facility` (`room_id`, `facility_id`)
SELECT `room_id`, `facility_id` FROM `tmp_new_rfac`;

-- -----------------------------------------------------------------------------
-- 9. 收尾
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `tmp_new_rfac`;
DROP TABLE IF EXISTS `tmp_new_alabel`;
DROP TABLE IF EXISTS `tmp_new_graph`;
DROP TABLE IF EXISTS `tmp_new_room`;
DROP TABLE IF EXISTS `tmp_new_apt`;
DROP TABLE IF EXISTS `tmp_pick`;
DROP TABLE IF EXISTS `tmp_word`;
DROP TABLE IF EXISTS `tmp_seq`;
DROP TABLE IF EXISTS `tmp_d10`;

SELECT '生成后' AS 阶段,
       (SELECT COUNT(*) FROM `apartment_info`) AS 公寓,
       (SELECT COUNT(*) FROM `room_info`)      AS 房间,
       (SELECT COUNT(*) FROM `graph_info`)     AS 图片,
       (SELECT COUNT(*) FROM `apartment_label`) AS 公寓标签,
       (SELECT COUNT(*) FROM `room_facility`)  AS 房间配套;

-- 抽查：随便看一个公寓的房间
SELECT a.`name` AS 公寓, a.`province_name` AS 省, a.`city_name` AS 市, a.`district_name` AS 区,
       COUNT(r.`id`) AS 房间数, MIN(r.`rent`) AS 最低租金, MAX(r.`rent`) AS 最高租金
FROM `apartment_info` a
         LEFT JOIN `room_info` r ON r.`apartment_id` = a.`id`
WHERE a.`name` LIKE '%公寓%'
GROUP BY a.`id`, a.`name`, a.`province_name`, a.`city_name`, a.`district_name`
ORDER BY a.`id` DESC
LIMIT 5;
