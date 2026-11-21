-- =============================================================================
--  归心寓 · 地区数据清理（修掉重跑产生的孤儿 + 去重）
--
--  背景：region-all-in-one.sql 是「先删省份再插省份」，
--        重跑时旧省份被删 → 上一轮插的城市 province_id 悬空，
--        这些城市就成了「孤儿」（前端树里看不到，但占着库）。
--        这个脚本把它们清掉，并顺手去重。
--
--  执行：mysql -uroot -p123456 --default-character-set=utf8mb4 lease < sql/region-cleanup.sql
-- =============================================================================

SET NAMES utf8mb4;
SET autocommit = 1;

SELECT '清理前' AS 阶段,
       (SELECT COUNT(*) FROM `province_info`) AS 省,
       (SELECT COUNT(*) FROM `city_info`)     AS 市,
       (SELECT COUNT(*) FROM `district_info`) AS 区县;

-- -----------------------------------------------------------------------------
-- 1. 孤儿区县：city_id 指向不存在的城市
-- -----------------------------------------------------------------------------
DELETE d FROM `district_info` d
LEFT JOIN `city_info` c ON c.`id` = d.`city_id`
WHERE c.`id` IS NULL;

-- -----------------------------------------------------------------------------
-- 2. 孤儿城市：province_id 指向不存在的省份
--    （先删区县再删城市，顺序不能反）
-- -----------------------------------------------------------------------------
DELETE c FROM `city_info` c
LEFT JOIN `province_info` p ON p.`id` = c.`province_id`
WHERE p.`id` IS NULL;

-- -----------------------------------------------------------------------------
-- 3. 去重：同名同父的只留 id 最小的那条
-- -----------------------------------------------------------------------------
DELETE x FROM `city_info` x
JOIN `city_info` y
  ON x.`name` = y.`name`
 AND x.`province_id` = y.`province_id`
 AND x.`id` > y.`id`;

DELETE x FROM `district_info` x
JOIN `district_info` y
  ON x.`name` = y.`name`
 AND x.`city_id` = y.`city_id`
 AND x.`id` > y.`id`;

-- -----------------------------------------------------------------------------
-- 4. 结果
-- -----------------------------------------------------------------------------
SELECT '清理后' AS 阶段,
       (SELECT COUNT(*) FROM `province_info`) AS 省,
       (SELECT COUNT(*) FROM `city_info`)     AS 市,
       (SELECT COUNT(*) FROM `district_info`) AS 区县;

-- 抽查：随便看一个省的市和区县
SELECT p.`name` AS 省, c.`name` AS 市, COUNT(d.`id`) AS 区县数
FROM `province_info` p
JOIN `city_info` c     ON c.`province_id` = p.`id`
LEFT JOIN `district_info` d ON d.`city_id` = c.`id`
WHERE p.`name` IN ('安徽省', '江苏省', '广东省')
GROUP BY p.`name`, c.`name`
ORDER BY p.`name`, c.`name`;
