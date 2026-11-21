-- =============================================================================
--  归心寓 · 全国省 / 市 / 区 数据导入（幂等，可重复执行）
--
--  【覆盖范围】
--    省级行政区   34 个（完整）
--    地级行政区  337 个（完整：地级市 / 自治州 / 地区 / 盟 + 4 个直辖市）
--    区县        317 个（4 个直辖市全量 86 + 27 个省会/首府 + 5 个计划单列市 231）
--
--  【幂等设计】
--    每一级都是「临时表 → 剔除库里已存在的 → 只插入剩下的」：
--       1. CREATE TEMPORARY TABLE 装待导入数据
--       2. DELETE 掉目标表里已经有的（按 名称 + 父级id 匹配）
--       3. INSERT 剩下的
--    所以：
--      · 已有的省/市/区不会重复插入，id 保持不变
--      · 种子公寓引用的 province_id / city_id / district_id 全部保持有效
--      · 反复执行结果一致
--    用临时表中转而不是 INSERT ... WHERE NOT EXISTS，是为了彻底避开
--    MySQL 1093（You can't specify target table for update in FROM clause）这个坑。
--
--  【执行方式】
--    mysql -uroot -p123456 --default-character-set=utf8mb4 lease < sql/region-full.sql
--    或用 Navicat / DataGrip 打开本文件整体执行
-- =============================================================================

SET NAMES utf8mb4;

-- =============================================================================
--  1. 省级行政区（34）
-- =============================================================================
DROP TEMPORARY TABLE IF EXISTS `tmp_province`;
CREATE TEMPORARY TABLE `tmp_province` (
  `name` VARCHAR(50) NOT NULL
) DEFAULT CHARSET = utf8mb4;

INSERT INTO `tmp_province` (`name`) VALUES
  ('北京市'), ('天津市'), ('河北省'), ('山西省'), ('内蒙古自治区'),
  ('辽宁省'), ('吉林省'), ('黑龙江省'), ('上海市'), ('江苏省'),
  ('浙江省'), ('安徽省'), ('福建省'), ('江西省'), ('山东省'),
  ('河南省'), ('湖北省'), ('湖南省'), ('广东省'), ('广西壮族自治区'),
  ('海南省'), ('重庆市'), ('四川省'), ('贵州省'), ('云南省'),
  ('西藏自治区'), ('陕西省'), ('甘肃省'), ('青海省'), ('宁夏回族自治区'),
  ('新疆维吾尔自治区'), ('台湾省'), ('香港特别行政区'), ('澳门特别行政区');

DELETE t FROM `tmp_province` t
  JOIN `province_info` p ON p.`name` = t.`name`;

INSERT INTO `province_info` (`name`, `create_time`, `update_time`, `is_deleted`)
SELECT `name`, NOW(), NOW(), 0 FROM `tmp_province`;

DROP TEMPORARY TABLE IF EXISTS `tmp_province`;


-- =============================================================================
--  2. 地级行政区（337）
--     数据用「城市名 + 所属省份名」，靠 JOIN province_info 拿到 province_id，
--     不用手算 id，也不会和已有记录撞主键
-- =============================================================================
DROP TEMPORARY TABLE IF EXISTS `tmp_city`;
CREATE TEMPORARY TABLE `tmp_city` (
  `name`        VARCHAR(50) NOT NULL,
  `province_id` INT         NOT NULL
) DEFAULT CHARSET = utf8mb4;

INSERT INTO `tmp_city` (`name`, `province_id`)
SELECT DISTINCT v.city, p.`id`
FROM (
  -- ---------- 直辖市（城市层就是它自己） ----------
           SELECT '北京市' AS city, '北京市' AS province
  UNION ALL SELECT '天津市', '天津市'
  UNION ALL SELECT '上海市', '上海市'
  UNION ALL SELECT '重庆市', '重庆市'

  -- ---------- 河北省 11 ----------
  UNION ALL SELECT '石家庄市', '河北省'
  UNION ALL SELECT '唐山市', '河北省'
  UNION ALL SELECT '秦皇岛市', '河北省'
  UNION ALL SELECT '邯郸市', '河北省'
  UNION ALL SELECT '邢台市', '河北省'
  UNION ALL SELECT '保定市', '河北省'
  UNION ALL SELECT '张家口市', '河北省'
  UNION ALL SELECT '承德市', '河北省'
  UNION ALL SELECT '沧州市', '河北省'
  UNION ALL SELECT '廊坊市', '河北省'
  UNION ALL SELECT '衡水市', '河北省'

  -- ---------- 山西省 11 ----------
  UNION ALL SELECT '太原市', '山西省'
  UNION ALL SELECT '大同市', '山西省'
  UNION ALL SELECT '阳泉市', '山西省'
  UNION ALL SELECT '长治市', '山西省'
  UNION ALL SELECT '晋城市', '山西省'
  UNION ALL SELECT '朔州市', '山西省'
  UNION ALL SELECT '晋中市', '山西省'
  UNION ALL SELECT '运城市', '山西省'
  UNION ALL SELECT '忻州市', '山西省'
  UNION ALL SELECT '临汾市', '山西省'
  UNION ALL SELECT '吕梁市', '山西省'

  -- ---------- 内蒙古自治区 12 ----------
  UNION ALL SELECT '呼和浩特市', '内蒙古自治区'
  UNION ALL SELECT '包头市', '内蒙古自治区'
  UNION ALL SELECT '乌海市', '内蒙古自治区'
  UNION ALL SELECT '赤峰市', '内蒙古自治区'
  UNION ALL SELECT '通辽市', '内蒙古自治区'
  UNION ALL SELECT '鄂尔多斯市', '内蒙古自治区'
  UNION ALL SELECT '呼伦贝尔市', '内蒙古自治区'
  UNION ALL SELECT '巴彦淖尔市', '内蒙古自治区'
  UNION ALL SELECT '乌兰察布市', '内蒙古自治区'
  UNION ALL SELECT '兴安盟', '内蒙古自治区'
  UNION ALL SELECT '锡林郭勒盟', '内蒙古自治区'
  UNION ALL SELECT '阿拉善盟', '内蒙古自治区'

  -- ---------- 辽宁省 14 ----------
  UNION ALL SELECT '沈阳市', '辽宁省'
  UNION ALL SELECT '大连市', '辽宁省'
  UNION ALL SELECT '鞍山市', '辽宁省'
  UNION ALL SELECT '抚顺市', '辽宁省'
  UNION ALL SELECT '本溪市', '辽宁省'
  UNION ALL SELECT '丹东市', '辽宁省'
  UNION ALL SELECT '锦州市', '辽宁省'
  UNION ALL SELECT '营口市', '辽宁省'
  UNION ALL SELECT '阜新市', '辽宁省'
  UNION ALL SELECT '辽阳市', '辽宁省'
  UNION ALL SELECT '盘锦市', '辽宁省'
  UNION ALL SELECT '铁岭市', '辽宁省'
  UNION ALL SELECT '朝阳市', '辽宁省'
  UNION ALL SELECT '葫芦岛市', '辽宁省'

  -- ---------- 吉林省 9 ----------
  UNION ALL SELECT '长春市', '吉林省'
  UNION ALL SELECT '吉林市', '吉林省'
  UNION ALL SELECT '四平市', '吉林省'
  UNION ALL SELECT '辽源市', '吉林省'
  UNION ALL SELECT '通化市', '吉林省'
  UNION ALL SELECT '白山市', '吉林省'
  UNION ALL SELECT '松原市', '吉林省'
  UNION ALL SELECT '白城市', '吉林省'
  UNION ALL SELECT '延边朝鲜族自治州', '吉林省'

  -- ---------- 黑龙江省 13 ----------
  UNION ALL SELECT '哈尔滨市', '黑龙江省'
  UNION ALL SELECT '齐齐哈尔市', '黑龙江省'
  UNION ALL SELECT '鸡西市', '黑龙江省'
  UNION ALL SELECT '鹤岗市', '黑龙江省'
  UNION ALL SELECT '双鸭山市', '黑龙江省'
  UNION ALL SELECT '大庆市', '黑龙江省'
  UNION ALL SELECT '伊春市', '黑龙江省'
  UNION ALL SELECT '佳木斯市', '黑龙江省'
  UNION ALL SELECT '七台河市', '黑龙江省'
  UNION ALL SELECT '牡丹江市', '黑龙江省'
  UNION ALL SELECT '黑河市', '黑龙江省'
  UNION ALL SELECT '绥化市', '黑龙江省'
  UNION ALL SELECT '大兴安岭地区', '黑龙江省'

  -- ---------- 江苏省 13 ----------
  UNION ALL SELECT '南京市', '江苏省'
  UNION ALL SELECT '无锡市', '江苏省'
  UNION ALL SELECT '徐州市', '江苏省'
  UNION ALL SELECT '常州市', '江苏省'
  UNION ALL SELECT '苏州市', '江苏省'
  UNION ALL SELECT '南通市', '江苏省'
  UNION ALL SELECT '连云港市', '江苏省'
  UNION ALL SELECT '淮安市', '江苏省'
  UNION ALL SELECT '盐城市', '江苏省'
  UNION ALL SELECT '扬州市', '江苏省'
  UNION ALL SELECT '镇江市', '江苏省'
  UNION ALL SELECT '泰州市', '江苏省'
  UNION ALL SELECT '宿迁市', '江苏省'

  -- ---------- 浙江省 11 ----------
  UNION ALL SELECT '杭州市', '浙江省'
  UNION ALL SELECT '宁波市', '浙江省'
  UNION ALL SELECT '温州市', '浙江省'
  UNION ALL SELECT '嘉兴市', '浙江省'
  UNION ALL SELECT '湖州市', '浙江省'
  UNION ALL SELECT '绍兴市', '浙江省'
  UNION ALL SELECT '金华市', '浙江省'
  UNION ALL SELECT '衢州市', '浙江省'
  UNION ALL SELECT '舟山市', '浙江省'
  UNION ALL SELECT '台州市', '浙江省'
  UNION ALL SELECT '丽水市', '浙江省'

  -- ---------- 安徽省 16 ----------
  UNION ALL SELECT '合肥市', '安徽省'
  UNION ALL SELECT '芜湖市', '安徽省'
  UNION ALL SELECT '蚌埠市', '安徽省'
  UNION ALL SELECT '淮南市', '安徽省'
  UNION ALL SELECT '马鞍山市', '安徽省'
  UNION ALL SELECT '淮北市', '安徽省'
  UNION ALL SELECT '铜陵市', '安徽省'
  UNION ALL SELECT '安庆市', '安徽省'
  UNION ALL SELECT '黄山市', '安徽省'
  UNION ALL SELECT '滁州市', '安徽省'
  UNION ALL SELECT '阜阳市', '安徽省'
  UNION ALL SELECT '宿州市', '安徽省'
  UNION ALL SELECT '六安市', '安徽省'
  UNION ALL SELECT '亳州市', '安徽省'
  UNION ALL SELECT '池州市', '安徽省'
  UNION ALL SELECT '宣城市', '安徽省'

  -- ---------- 福建省 9 ----------
  UNION ALL SELECT '福州市', '福建省'
  UNION ALL SELECT '厦门市', '福建省'
  UNION ALL SELECT '莆田市', '福建省'
  UNION ALL SELECT '三明市', '福建省'
  UNION ALL SELECT '泉州市', '福建省'
  UNION ALL SELECT '漳州市', '福建省'
  UNION ALL SELECT '南平市', '福建省'
  UNION ALL SELECT '龙岩市', '福建省'
  UNION ALL SELECT '宁德市', '福建省'

  -- ---------- 江西省 11 ----------
  UNION ALL SELECT '南昌市', '江西省'
  UNION ALL SELECT '景德镇市', '江西省'
  UNION ALL SELECT '萍乡市', '江西省'
  UNION ALL SELECT '九江市', '江西省'
  UNION ALL SELECT '新余市', '江西省'
  UNION ALL SELECT '鹰潭市', '江西省'
  UNION ALL SELECT '赣州市', '江西省'
  UNION ALL SELECT '吉安市', '江西省'
  UNION ALL SELECT '宜春市', '江西省'
  UNION ALL SELECT '抚州市', '江西省'
  UNION ALL SELECT '上饶市', '江西省'

  -- ---------- 山东省 16 ----------
  UNION ALL SELECT '济南市', '山东省'
  UNION ALL SELECT '青岛市', '山东省'
  UNION ALL SELECT '淄博市', '山东省'
  UNION ALL SELECT '枣庄市', '山东省'
  UNION ALL SELECT '东营市', '山东省'
  UNION ALL SELECT '烟台市', '山东省'
  UNION ALL SELECT '潍坊市', '山东省'
  UNION ALL SELECT '济宁市', '山东省'
  UNION ALL SELECT '泰安市', '山东省'
  UNION ALL SELECT '威海市', '山东省'
  UNION ALL SELECT '日照市', '山东省'
  UNION ALL SELECT '临沂市', '山东省'
  UNION ALL SELECT '德州市', '山东省'
  UNION ALL SELECT '聊城市', '山东省'
  UNION ALL SELECT '滨州市', '山东省'
  UNION ALL SELECT '菏泽市', '山东省'

  -- ---------- 河南省 17 ----------
  UNION ALL SELECT '郑州市', '河南省'
  UNION ALL SELECT '开封市', '河南省'
  UNION ALL SELECT '洛阳市', '河南省'
  UNION ALL SELECT '平顶山市', '河南省'
  UNION ALL SELECT '安阳市', '河南省'
  UNION ALL SELECT '鹤壁市', '河南省'
  UNION ALL SELECT '新乡市', '河南省'
  UNION ALL SELECT '焦作市', '河南省'
  UNION ALL SELECT '濮阳市', '河南省'
  UNION ALL SELECT '许昌市', '河南省'
  UNION ALL SELECT '漯河市', '河南省'
  UNION ALL SELECT '三门峡市', '河南省'
  UNION ALL SELECT '南阳市', '河南省'
  UNION ALL SELECT '商丘市', '河南省'
  UNION ALL SELECT '信阳市', '河南省'
  UNION ALL SELECT '周口市', '河南省'
  UNION ALL SELECT '驻马店市', '河南省'

  -- ---------- 湖北省 13 ----------
  UNION ALL SELECT '武汉市', '湖北省'
  UNION ALL SELECT '黄石市', '湖北省'
  UNION ALL SELECT '十堰市', '湖北省'
  UNION ALL SELECT '宜昌市', '湖北省'
  UNION ALL SELECT '襄阳市', '湖北省'
  UNION ALL SELECT '鄂州市', '湖北省'
  UNION ALL SELECT '荆门市', '湖北省'
  UNION ALL SELECT '孝感市', '湖北省'
  UNION ALL SELECT '荆州市', '湖北省'
  UNION ALL SELECT '黄冈市', '湖北省'
  UNION ALL SELECT '咸宁市', '湖北省'
  UNION ALL SELECT '随州市', '湖北省'
  UNION ALL SELECT '恩施土家族苗族自治州', '湖北省'

  -- ---------- 湖南省 14 ----------
  UNION ALL SELECT '长沙市', '湖南省'
  UNION ALL SELECT '株洲市', '湖南省'
  UNION ALL SELECT '湘潭市', '湖南省'
  UNION ALL SELECT '衡阳市', '湖南省'
  UNION ALL SELECT '邵阳市', '湖南省'
  UNION ALL SELECT '岳阳市', '湖南省'
  UNION ALL SELECT '常德市', '湖南省'
  UNION ALL SELECT '张家界市', '湖南省'
  UNION ALL SELECT '益阳市', '湖南省'
  UNION ALL SELECT '郴州市', '湖南省'
  UNION ALL SELECT '永州市', '湖南省'
  UNION ALL SELECT '怀化市', '湖南省'
  UNION ALL SELECT '娄底市', '湖南省'
  UNION ALL SELECT '湘西土家族苗族自治州', '湖南省'

  -- ---------- 广东省 21 ----------
  UNION ALL SELECT '广州市', '广东省'
  UNION ALL SELECT '韶关市', '广东省'
  UNION ALL SELECT '深圳市', '广东省'
  UNION ALL SELECT '珠海市', '广东省'
  UNION ALL SELECT '汕头市', '广东省'
  UNION ALL SELECT '佛山市', '广东省'
  UNION ALL SELECT '江门市', '广东省'
  UNION ALL SELECT '湛江市', '广东省'
  UNION ALL SELECT '茂名市', '广东省'
  UNION ALL SELECT '肇庆市', '广东省'
  UNION ALL SELECT '惠州市', '广东省'
  UNION ALL SELECT '梅州市', '广东省'
  UNION ALL SELECT '汕尾市', '广东省'
  UNION ALL SELECT '河源市', '广东省'
  UNION ALL SELECT '阳江市', '广东省'
  UNION ALL SELECT '清远市', '广东省'
  UNION ALL SELECT '东莞市', '广东省'
  UNION ALL SELECT '中山市', '广东省'
  UNION ALL SELECT '潮州市', '广东省'
  UNION ALL SELECT '揭阳市', '广东省'
  UNION ALL SELECT '云浮市', '广东省'

  -- ---------- 广西壮族自治区 14 ----------
  UNION ALL SELECT '南宁市', '广西壮族自治区'
  UNION ALL SELECT '柳州市', '广西壮族自治区'
  UNION ALL SELECT '桂林市', '广西壮族自治区'
  UNION ALL SELECT '梧州市', '广西壮族自治区'
  UNION ALL SELECT '北海市', '广西壮族自治区'
  UNION ALL SELECT '防城港市', '广西壮族自治区'
  UNION ALL SELECT '钦州市', '广西壮族自治区'
  UNION ALL SELECT '贵港市', '广西壮族自治区'
  UNION ALL SELECT '玉林市', '广西壮族自治区'
  UNION ALL SELECT '百色市', '广西壮族自治区'
  UNION ALL SELECT '贺州市', '广西壮族自治区'
  UNION ALL SELECT '河池市', '广西壮族自治区'
  UNION ALL SELECT '来宾市', '广西壮族自治区'
  UNION ALL SELECT '崇左市', '广西壮族自治区'

  -- ---------- 海南省 4 ----------
  UNION ALL SELECT '海口市', '海南省'
  UNION ALL SELECT '三亚市', '海南省'
  UNION ALL SELECT '三沙市', '海南省'
  UNION ALL SELECT '儋州市', '海南省'

  -- ---------- 四川省 21 ----------
  UNION ALL SELECT '成都市', '四川省'
  UNION ALL SELECT '自贡市', '四川省'
  UNION ALL SELECT '攀枝花市', '四川省'
  UNION ALL SELECT '泸州市', '四川省'
  UNION ALL SELECT '德阳市', '四川省'
  UNION ALL SELECT '绵阳市', '四川省'
  UNION ALL SELECT '广元市', '四川省'
  UNION ALL SELECT '遂宁市', '四川省'
  UNION ALL SELECT '内江市', '四川省'
  UNION ALL SELECT '乐山市', '四川省'
  UNION ALL SELECT '南充市', '四川省'
  UNION ALL SELECT '眉山市', '四川省'
  UNION ALL SELECT '宜宾市', '四川省'
  UNION ALL SELECT '广安市', '四川省'
  UNION ALL SELECT '达州市', '四川省'
  UNION ALL SELECT '雅安市', '四川省'
  UNION ALL SELECT '巴中市', '四川省'
  UNION ALL SELECT '资阳市', '四川省'
  UNION ALL SELECT '阿坝藏族羌族自治州', '四川省'
  UNION ALL SELECT '甘孜藏族自治州', '四川省'
  UNION ALL SELECT '凉山彝族自治州', '四川省'

  -- ---------- 贵州省 9 ----------
  UNION ALL SELECT '贵阳市', '贵州省'
  UNION ALL SELECT '六盘水市', '贵州省'
  UNION ALL SELECT '遵义市', '贵州省'
  UNION ALL SELECT '安顺市', '贵州省'
  UNION ALL SELECT '毕节市', '贵州省'
  UNION ALL SELECT '铜仁市', '贵州省'
  UNION ALL SELECT '黔西南布依族苗族自治州', '贵州省'
  UNION ALL SELECT '黔东南苗族侗族自治州', '贵州省'
  UNION ALL SELECT '黔南布依族苗族自治州', '贵州省'

  -- ---------- 云南省 16 ----------
  UNION ALL SELECT '昆明市', '云南省'
  UNION ALL SELECT '曲靖市', '云南省'
  UNION ALL SELECT '玉溪市', '云南省'
  UNION ALL SELECT '保山市', '云南省'
  UNION ALL SELECT '昭通市', '云南省'
  UNION ALL SELECT '丽江市', '云南省'
  UNION ALL SELECT '普洱市', '云南省'
  UNION ALL SELECT '临沧市', '云南省'
  UNION ALL SELECT '楚雄彝族自治州', '云南省'
  UNION ALL SELECT '红河哈尼族彝族自治州', '云南省'
  UNION ALL SELECT '文山壮族苗族自治州', '云南省'
  UNION ALL SELECT '西双版纳傣族自治州', '云南省'
  UNION ALL SELECT '大理白族自治州', '云南省'
  UNION ALL SELECT '德宏傣族景颇族自治州', '云南省'
  UNION ALL SELECT '怒江傈僳族自治州', '云南省'
  UNION ALL SELECT '迪庆藏族自治州', '云南省'

  -- ---------- 西藏自治区 7 ----------
  UNION ALL SELECT '拉萨市', '西藏自治区'
  UNION ALL SELECT '日喀则市', '西藏自治区'
  UNION ALL SELECT '昌都市', '西藏自治区'
  UNION ALL SELECT '林芝市', '西藏自治区'
  UNION ALL SELECT '山南市', '西藏自治区'
  UNION ALL SELECT '那曲市', '西藏自治区'
  UNION ALL SELECT '阿里地区', '西藏自治区'

  -- ---------- 陕西省 10 ----------
  UNION ALL SELECT '西安市', '陕西省'
  UNION ALL SELECT '铜川市', '陕西省'
  UNION ALL SELECT '宝鸡市', '陕西省'
  UNION ALL SELECT '咸阳市', '陕西省'
  UNION ALL SELECT '渭南市', '陕西省'
  UNION ALL SELECT '延安市', '陕西省'
  UNION ALL SELECT '汉中市', '陕西省'
  UNION ALL SELECT '榆林市', '陕西省'
  UNION ALL SELECT '安康市', '陕西省'
  UNION ALL SELECT '商洛市', '陕西省'

  -- ---------- 甘肃省 14 ----------
  UNION ALL SELECT '兰州市', '甘肃省'
  UNION ALL SELECT '嘉峪关市', '甘肃省'
  UNION ALL SELECT '金昌市', '甘肃省'
  UNION ALL SELECT '白银市', '甘肃省'
  UNION ALL SELECT '天水市', '甘肃省'
  UNION ALL SELECT '武威市', '甘肃省'
  UNION ALL SELECT '张掖市', '甘肃省'
  UNION ALL SELECT '平凉市', '甘肃省'
  UNION ALL SELECT '酒泉市', '甘肃省'
  UNION ALL SELECT '庆阳市', '甘肃省'
  UNION ALL SELECT '定西市', '甘肃省'
  UNION ALL SELECT '陇南市', '甘肃省'
  UNION ALL SELECT '临夏回族自治州', '甘肃省'
  UNION ALL SELECT '甘南藏族自治州', '甘肃省'

  -- ---------- 青海省 8 ----------
  UNION ALL SELECT '西宁市', '青海省'
  UNION ALL SELECT '海东市', '青海省'
  UNION ALL SELECT '海北藏族自治州', '青海省'
  UNION ALL SELECT '黄南藏族自治州', '青海省'
  UNION ALL SELECT '海南藏族自治州', '青海省'
  UNION ALL SELECT '果洛藏族自治州', '青海省'
  UNION ALL SELECT '玉树藏族自治州', '青海省'
  UNION ALL SELECT '海西蒙古族藏族自治州', '青海省'

  -- ---------- 宁夏回族自治区 5 ----------
  UNION ALL SELECT '银川市', '宁夏回族自治区'
  UNION ALL SELECT '石嘴山市', '宁夏回族自治区'
  UNION ALL SELECT '吴忠市', '宁夏回族自治区'
  UNION ALL SELECT '固原市', '宁夏回族自治区'
  UNION ALL SELECT '中卫市', '宁夏回族自治区'

  -- ---------- 新疆维吾尔自治区 14 ----------
  UNION ALL SELECT '乌鲁木齐市', '新疆维吾尔自治区'
  UNION ALL SELECT '克拉玛依市', '新疆维吾尔自治区'
  UNION ALL SELECT '吐鲁番市', '新疆维吾尔自治区'
  UNION ALL SELECT '哈密市', '新疆维吾尔自治区'
  UNION ALL SELECT '昌吉回族自治州', '新疆维吾尔自治区'
  UNION ALL SELECT '博尔塔拉蒙古自治州', '新疆维吾尔自治区'
  UNION ALL SELECT '巴音郭楞蒙古自治州', '新疆维吾尔自治区'
  UNION ALL SELECT '阿克苏地区', '新疆维吾尔自治区'
  UNION ALL SELECT '克孜勒苏柯尔克孜自治州', '新疆维吾尔自治区'
  UNION ALL SELECT '喀什地区', '新疆维吾尔自治区'
  UNION ALL SELECT '和田地区', '新疆维吾尔自治区'
  UNION ALL SELECT '伊犁哈萨克自治州', '新疆维吾尔自治区'
  UNION ALL SELECT '塔城地区', '新疆维吾尔自治区'
  UNION ALL SELECT '阿勒泰地区', '新疆维吾尔自治区'
) v
JOIN `province_info` p ON p.`name` = v.province;

DELETE t FROM `tmp_city` t
  JOIN `city_info` c ON c.`name` = t.`name` AND c.`province_id` = t.`province_id`;

INSERT INTO `city_info` (`name`, `province_id`, `create_time`, `update_time`, `is_deleted`)
SELECT `name`, `province_id`, NOW(), NOW(), 0 FROM `tmp_city`;

DROP TEMPORARY TABLE IF EXISTS `tmp_city`;


-- =============================================================================
--  3. 区县（317）
--     数据用「区县名 + 所属城市 + 所属省份」，带省份是为了消歧
--     （朝阳区 在北京和长春都有，和平区 在天津和沈阳都有）
-- =============================================================================
DROP TEMPORARY TABLE IF EXISTS `tmp_district`;
CREATE TEMPORARY TABLE `tmp_district` (
  `name`    VARCHAR(50) NOT NULL,
  `city_id` INT         NOT NULL
) DEFAULT CHARSET = utf8mb4;

INSERT INTO `tmp_district` (`name`, `city_id`)
SELECT DISTINCT v.district, c.`id`
FROM (
  -- ---------- 北京市 16 ----------
           SELECT '东城区' AS district, '北京市' AS city, '北京市' AS province
  UNION ALL SELECT '西城区', '北京市', '北京市'
  UNION ALL SELECT '朝阳区', '北京市', '北京市'
  UNION ALL SELECT '丰台区', '北京市', '北京市'
  UNION ALL SELECT '石景山区', '北京市', '北京市'
  UNION ALL SELECT '海淀区', '北京市', '北京市'
  UNION ALL SELECT '门头沟区', '北京市', '北京市'
  UNION ALL SELECT '房山区', '北京市', '北京市'
  UNION ALL SELECT '通州区', '北京市', '北京市'
  UNION ALL SELECT '顺义区', '北京市', '北京市'
  UNION ALL SELECT '昌平区', '北京市', '北京市'
  UNION ALL SELECT '大兴区', '北京市', '北京市'
  UNION ALL SELECT '怀柔区', '北京市', '北京市'
  UNION ALL SELECT '平谷区', '北京市', '北京市'
  UNION ALL SELECT '密云区', '北京市', '北京市'
  UNION ALL SELECT '延庆区', '北京市', '北京市'

  -- ---------- 天津市 16 ----------
  UNION ALL SELECT '和平区', '天津市', '天津市'
  UNION ALL SELECT '河东区', '天津市', '天津市'
  UNION ALL SELECT '河西区', '天津市', '天津市'
  UNION ALL SELECT '南开区', '天津市', '天津市'
  UNION ALL SELECT '河北区', '天津市', '天津市'
  UNION ALL SELECT '红桥区', '天津市', '天津市'
  UNION ALL SELECT '东丽区', '天津市', '天津市'
  UNION ALL SELECT '西青区', '天津市', '天津市'
  UNION ALL SELECT '津南区', '天津市', '天津市'
  UNION ALL SELECT '北辰区', '天津市', '天津市'
  UNION ALL SELECT '武清区', '天津市', '天津市'
  UNION ALL SELECT '宝坻区', '天津市', '天津市'
  UNION ALL SELECT '滨海新区', '天津市', '天津市'
  UNION ALL SELECT '宁河区', '天津市', '天津市'
  UNION ALL SELECT '静海区', '天津市', '天津市'
  UNION ALL SELECT '蓟州区', '天津市', '天津市'

  -- ---------- 上海市 16 ----------
  UNION ALL SELECT '黄浦区', '上海市', '上海市'
  UNION ALL SELECT '徐汇区', '上海市', '上海市'
  UNION ALL SELECT '长宁区', '上海市', '上海市'
  UNION ALL SELECT '静安区', '上海市', '上海市'
  UNION ALL SELECT '普陀区', '上海市', '上海市'
  UNION ALL SELECT '虹口区', '上海市', '上海市'
  UNION ALL SELECT '杨浦区', '上海市', '上海市'
  UNION ALL SELECT '闵行区', '上海市', '上海市'
  UNION ALL SELECT '宝山区', '上海市', '上海市'
  UNION ALL SELECT '嘉定区', '上海市', '上海市'
  UNION ALL SELECT '浦东新区', '上海市', '上海市'
  UNION ALL SELECT '金山区', '上海市', '上海市'
  UNION ALL SELECT '松江区', '上海市', '上海市'
  UNION ALL SELECT '青浦区', '上海市', '上海市'
  UNION ALL SELECT '奉贤区', '上海市', '上海市'
  UNION ALL SELECT '崇明区', '上海市', '上海市'

  -- ---------- 重庆市 38 ----------
  UNION ALL SELECT '渝中区', '重庆市', '重庆市'
  UNION ALL SELECT '大渡口区', '重庆市', '重庆市'
  UNION ALL SELECT '江北区', '重庆市', '重庆市'
  UNION ALL SELECT '沙坪坝区', '重庆市', '重庆市'
  UNION ALL SELECT '九龙坡区', '重庆市', '重庆市'
  UNION ALL SELECT '南岸区', '重庆市', '重庆市'
  UNION ALL SELECT '北碚区', '重庆市', '重庆市'
  UNION ALL SELECT '渝北区', '重庆市', '重庆市'
  UNION ALL SELECT '巴南区', '重庆市', '重庆市'
  UNION ALL SELECT '万州区', '重庆市', '重庆市'
  UNION ALL SELECT '涪陵区', '重庆市', '重庆市'
  UNION ALL SELECT '黔江区', '重庆市', '重庆市'
  UNION ALL SELECT '长寿区', '重庆市', '重庆市'
  UNION ALL SELECT '江津区', '重庆市', '重庆市'
  UNION ALL SELECT '合川区', '重庆市', '重庆市'
  UNION ALL SELECT '永川区', '重庆市', '重庆市'
  UNION ALL SELECT '南川区', '重庆市', '重庆市'
  UNION ALL SELECT '綦江区', '重庆市', '重庆市'
  UNION ALL SELECT '大足区', '重庆市', '重庆市'
  UNION ALL SELECT '璧山区', '重庆市', '重庆市'
  UNION ALL SELECT '铜梁区', '重庆市', '重庆市'
  UNION ALL SELECT '潼南区', '重庆市', '重庆市'
  UNION ALL SELECT '荣昌区', '重庆市', '重庆市'
  UNION ALL SELECT '开州区', '重庆市', '重庆市'
  UNION ALL SELECT '梁平区', '重庆市', '重庆市'
  UNION ALL SELECT '武隆区', '重庆市', '重庆市'
  UNION ALL SELECT '城口县', '重庆市', '重庆市'
  UNION ALL SELECT '丰都县', '重庆市', '重庆市'
  UNION ALL SELECT '垫江县', '重庆市', '重庆市'
  UNION ALL SELECT '忠县', '重庆市', '重庆市'
  UNION ALL SELECT '云阳县', '重庆市', '重庆市'
  UNION ALL SELECT '奉节县', '重庆市', '重庆市'
  UNION ALL SELECT '巫山县', '重庆市', '重庆市'
  UNION ALL SELECT '巫溪县', '重庆市', '重庆市'
  UNION ALL SELECT '石柱土家族自治县', '重庆市', '重庆市'
  UNION ALL SELECT '秀山土家族苗族自治县', '重庆市', '重庆市'
  UNION ALL SELECT '酉阳土家族苗族自治县', '重庆市', '重庆市'
  UNION ALL SELECT '彭水苗族土家族自治县', '重庆市', '重庆市'

  -- ---------- 石家庄市 8 ----------
  UNION ALL SELECT '长安区', '石家庄市', '河北省'
  UNION ALL SELECT '桥西区', '石家庄市', '河北省'
  UNION ALL SELECT '新华区', '石家庄市', '河北省'
  UNION ALL SELECT '井陉矿区', '石家庄市', '河北省'
  UNION ALL SELECT '裕华区', '石家庄市', '河北省'
  UNION ALL SELECT '藁城区', '石家庄市', '河北省'
  UNION ALL SELECT '鹿泉区', '石家庄市', '河北省'
  UNION ALL SELECT '栾城区', '石家庄市', '河北省'

  -- ---------- 太原市 6 ----------
  UNION ALL SELECT '小店区', '太原市', '山西省'
  UNION ALL SELECT '迎泽区', '太原市', '山西省'
  UNION ALL SELECT '杏花岭区', '太原市', '山西省'
  UNION ALL SELECT '尖草坪区', '太原市', '山西省'
  UNION ALL SELECT '万柏林区', '太原市', '山西省'
  UNION ALL SELECT '晋源区', '太原市', '山西省'

  -- ---------- 呼和浩特市 4 ----------
  UNION ALL SELECT '新城区', '呼和浩特市', '内蒙古自治区'
  UNION ALL SELECT '回民区', '呼和浩特市', '内蒙古自治区'
  UNION ALL SELECT '玉泉区', '呼和浩特市', '内蒙古自治区'
  UNION ALL SELECT '赛罕区', '呼和浩特市', '内蒙古自治区'

  -- ---------- 沈阳市 10 ----------
  UNION ALL SELECT '和平区', '沈阳市', '辽宁省'
  UNION ALL SELECT '沈河区', '沈阳市', '辽宁省'
  UNION ALL SELECT '大东区', '沈阳市', '辽宁省'
  UNION ALL SELECT '皇姑区', '沈阳市', '辽宁省'
  UNION ALL SELECT '铁西区', '沈阳市', '辽宁省'
  UNION ALL SELECT '苏家屯区', '沈阳市', '辽宁省'
  UNION ALL SELECT '浑南区', '沈阳市', '辽宁省'
  UNION ALL SELECT '沈北新区', '沈阳市', '辽宁省'
  UNION ALL SELECT '于洪区', '沈阳市', '辽宁省'
  UNION ALL SELECT '辽中区', '沈阳市', '辽宁省'

  -- ---------- 大连市 7 ----------
  UNION ALL SELECT '中山区', '大连市', '辽宁省'
  UNION ALL SELECT '西岗区', '大连市', '辽宁省'
  UNION ALL SELECT '沙河口区', '大连市', '辽宁省'
  UNION ALL SELECT '甘井子区', '大连市', '辽宁省'
  UNION ALL SELECT '旅顺口区', '大连市', '辽宁省'
  UNION ALL SELECT '金州区', '大连市', '辽宁省'
  UNION ALL SELECT '普兰店区', '大连市', '辽宁省'

  -- ---------- 长春市 7 ----------
  UNION ALL SELECT '南关区', '长春市', '吉林省'
  UNION ALL SELECT '宽城区', '长春市', '吉林省'
  UNION ALL SELECT '朝阳区', '长春市', '吉林省'
  UNION ALL SELECT '二道区', '长春市', '吉林省'
  UNION ALL SELECT '绿园区', '长春市', '吉林省'
  UNION ALL SELECT '双阳区', '长春市', '吉林省'
  UNION ALL SELECT '九台区', '长春市', '吉林省'

  -- ---------- 哈尔滨市 9 ----------
  UNION ALL SELECT '道里区', '哈尔滨市', '黑龙江省'
  UNION ALL SELECT '南岗区', '哈尔滨市', '黑龙江省'
  UNION ALL SELECT '道外区', '哈尔滨市', '黑龙江省'
  UNION ALL SELECT '平房区', '哈尔滨市', '黑龙江省'
  UNION ALL SELECT '松北区', '哈尔滨市', '黑龙江省'
  UNION ALL SELECT '香坊区', '哈尔滨市', '黑龙江省'
  UNION ALL SELECT '呼兰区', '哈尔滨市', '黑龙江省'
  UNION ALL SELECT '阿城区', '哈尔滨市', '黑龙江省'
  UNION ALL SELECT '双城区', '哈尔滨市', '黑龙江省'

  -- ---------- 南京市 11 ----------
  UNION ALL SELECT '玄武区', '南京市', '江苏省'
  UNION ALL SELECT '秦淮区', '南京市', '江苏省'
  UNION ALL SELECT '建邺区', '南京市', '江苏省'
  UNION ALL SELECT '鼓楼区', '南京市', '江苏省'
  UNION ALL SELECT '浦口区', '南京市', '江苏省'
  UNION ALL SELECT '栖霞区', '南京市', '江苏省'
  UNION ALL SELECT '雨花台区', '南京市', '江苏省'
  UNION ALL SELECT '江宁区', '南京市', '江苏省'
  UNION ALL SELECT '六合区', '南京市', '江苏省'
  UNION ALL SELECT '溧水区', '南京市', '江苏省'
  UNION ALL SELECT '高淳区', '南京市', '江苏省'

  -- ---------- 杭州市 10 ----------
  UNION ALL SELECT '上城区', '杭州市', '浙江省'
  UNION ALL SELECT '拱墅区', '杭州市', '浙江省'
  UNION ALL SELECT '西湖区', '杭州市', '浙江省'
  UNION ALL SELECT '滨江区', '杭州市', '浙江省'
  UNION ALL SELECT '萧山区', '杭州市', '浙江省'
  UNION ALL SELECT '余杭区', '杭州市', '浙江省'
  UNION ALL SELECT '富阳区', '杭州市', '浙江省'
  UNION ALL SELECT '临安区', '杭州市', '浙江省'
  UNION ALL SELECT '临平区', '杭州市', '浙江省'
  UNION ALL SELECT '钱塘区', '杭州市', '浙江省'

  -- ---------- 宁波市 6 ----------
  UNION ALL SELECT '海曙区', '宁波市', '浙江省'
  UNION ALL SELECT '江北区', '宁波市', '浙江省'
  UNION ALL SELECT '北仑区', '宁波市', '浙江省'
  UNION ALL SELECT '镇海区', '宁波市', '浙江省'
  UNION ALL SELECT '鄞州区', '宁波市', '浙江省'
  UNION ALL SELECT '奉化区', '宁波市', '浙江省'

  -- ---------- 合肥市 4 ----------
  UNION ALL SELECT '瑶海区', '合肥市', '安徽省'
  UNION ALL SELECT '庐阳区', '合肥市', '安徽省'
  UNION ALL SELECT '蜀山区', '合肥市', '安徽省'
  UNION ALL SELECT '包河区', '合肥市', '安徽省'

  -- ---------- 福州市 6 ----------
  UNION ALL SELECT '鼓楼区', '福州市', '福建省'
  UNION ALL SELECT '台江区', '福州市', '福建省'
  UNION ALL SELECT '仓山区', '福州市', '福建省'
  UNION ALL SELECT '马尾区', '福州市', '福建省'
  UNION ALL SELECT '晋安区', '福州市', '福建省'
  UNION ALL SELECT '长乐区', '福州市', '福建省'

  -- ---------- 厦门市 6 ----------
  UNION ALL SELECT '思明区', '厦门市', '福建省'
  UNION ALL SELECT '海沧区', '厦门市', '福建省'
  UNION ALL SELECT '湖里区', '厦门市', '福建省'
  UNION ALL SELECT '集美区', '厦门市', '福建省'
  UNION ALL SELECT '同安区', '厦门市', '福建省'
  UNION ALL SELECT '翔安区', '厦门市', '福建省'

  -- ---------- 南昌市 6 ----------
  UNION ALL SELECT '东湖区', '南昌市', '江西省'
  UNION ALL SELECT '西湖区', '南昌市', '江西省'
  UNION ALL SELECT '青云谱区', '南昌市', '江西省'
  UNION ALL SELECT '青山湖区', '南昌市', '江西省'
  UNION ALL SELECT '新建区', '南昌市', '江西省'
  UNION ALL SELECT '红谷滩区', '南昌市', '江西省'

  -- ---------- 济南市 10 ----------
  UNION ALL SELECT '历下区', '济南市', '山东省'
  UNION ALL SELECT '市中区', '济南市', '山东省'
  UNION ALL SELECT '槐荫区', '济南市', '山东省'
  UNION ALL SELECT '天桥区', '济南市', '山东省'
  UNION ALL SELECT '历城区', '济南市', '山东省'
  UNION ALL SELECT '长清区', '济南市', '山东省'
  UNION ALL SELECT '章丘区', '济南市', '山东省'
  UNION ALL SELECT '济阳区', '济南市', '山东省'
  UNION ALL SELECT '莱芜区', '济南市', '山东省'
  UNION ALL SELECT '钢城区', '济南市', '山东省'

  -- ---------- 青岛市 7 ----------
  UNION ALL SELECT '市南区', '青岛市', '山东省'
  UNION ALL SELECT '市北区', '青岛市', '山东省'
  UNION ALL SELECT '黄岛区', '青岛市', '山东省'
  UNION ALL SELECT '崂山区', '青岛市', '山东省'
  UNION ALL SELECT '李沧区', '青岛市', '山东省'
  UNION ALL SELECT '城阳区', '青岛市', '山东省'
  UNION ALL SELECT '即墨区', '青岛市', '山东省'

  -- ---------- 郑州市 6 ----------
  UNION ALL SELECT '中原区', '郑州市', '河南省'
  UNION ALL SELECT '二七区', '郑州市', '河南省'
  UNION ALL SELECT '管城回族区', '郑州市', '河南省'
  UNION ALL SELECT '金水区', '郑州市', '河南省'
  UNION ALL SELECT '上街区', '郑州市', '河南省'
  UNION ALL SELECT '惠济区', '郑州市', '河南省'

  -- ---------- 武汉市 13 ----------
  UNION ALL SELECT '江岸区', '武汉市', '湖北省'
  UNION ALL SELECT '江汉区', '武汉市', '湖北省'
  UNION ALL SELECT '硚口区', '武汉市', '湖北省'
  UNION ALL SELECT '汉阳区', '武汉市', '湖北省'
  UNION ALL SELECT '武昌区', '武汉市', '湖北省'
  UNION ALL SELECT '青山区', '武汉市', '湖北省'
  UNION ALL SELECT '洪山区', '武汉市', '湖北省'
  UNION ALL SELECT '东西湖区', '武汉市', '湖北省'
  UNION ALL SELECT '汉南区', '武汉市', '湖北省'
  UNION ALL SELECT '蔡甸区', '武汉市', '湖北省'
  UNION ALL SELECT '江夏区', '武汉市', '湖北省'
  UNION ALL SELECT '黄陂区', '武汉市', '湖北省'
  UNION ALL SELECT '新洲区', '武汉市', '湖北省'

  -- ---------- 长沙市 6 ----------
  UNION ALL SELECT '芙蓉区', '长沙市', '湖南省'
  UNION ALL SELECT '天心区', '长沙市', '湖南省'
  UNION ALL SELECT '岳麓区', '长沙市', '湖南省'
  UNION ALL SELECT '开福区', '长沙市', '湖南省'
  UNION ALL SELECT '雨花区', '长沙市', '湖南省'
  UNION ALL SELECT '望城区', '长沙市', '湖南省'

  -- ---------- 广州市 11 ----------
  UNION ALL SELECT '荔湾区', '广州市', '广东省'
  UNION ALL SELECT '越秀区', '广州市', '广东省'
  UNION ALL SELECT '海珠区', '广州市', '广东省'
  UNION ALL SELECT '天河区', '广州市', '广东省'
  UNION ALL SELECT '白云区', '广州市', '广东省'
  UNION ALL SELECT '黄埔区', '广州市', '广东省'
  UNION ALL SELECT '番禺区', '广州市', '广东省'
  UNION ALL SELECT '花都区', '广州市', '广东省'
  UNION ALL SELECT '南沙区', '广州市', '广东省'
  UNION ALL SELECT '从化区', '广州市', '广东省'
  UNION ALL SELECT '增城区', '广州市', '广东省'

  -- ---------- 深圳市 9 ----------
  UNION ALL SELECT '罗湖区', '深圳市', '广东省'
  UNION ALL SELECT '福田区', '深圳市', '广东省'
  UNION ALL SELECT '南山区', '深圳市', '广东省'
  UNION ALL SELECT '宝安区', '深圳市', '广东省'
  UNION ALL SELECT '龙岗区', '深圳市', '广东省'
  UNION ALL SELECT '盐田区', '深圳市', '广东省'
  UNION ALL SELECT '龙华区', '深圳市', '广东省'
  UNION ALL SELECT '坪山区', '深圳市', '广东省'
  UNION ALL SELECT '光明区', '深圳市', '广东省'

  -- ---------- 南宁市 7 ----------
  UNION ALL SELECT '兴宁区', '南宁市', '广西壮族自治区'
  UNION ALL SELECT '青秀区', '南宁市', '广西壮族自治区'
  UNION ALL SELECT '江南区', '南宁市', '广西壮族自治区'
  UNION ALL SELECT '西乡塘区', '南宁市', '广西壮族自治区'
  UNION ALL SELECT '良庆区', '南宁市', '广西壮族自治区'
  UNION ALL SELECT '邕宁区', '南宁市', '广西壮族自治区'
  UNION ALL SELECT '武鸣区', '南宁市', '广西壮族自治区'

  -- ---------- 海口市 4 ----------
  UNION ALL SELECT '秀英区', '海口市', '海南省'
  UNION ALL SELECT '龙华区', '海口市', '海南省'
  UNION ALL SELECT '琼山区', '海口市', '海南省'
  UNION ALL SELECT '美兰区', '海口市', '海南省'

  -- ---------- 成都市 12 ----------
  UNION ALL SELECT '锦江区', '成都市', '四川省'
  UNION ALL SELECT '青羊区', '成都市', '四川省'
  UNION ALL SELECT '金牛区', '成都市', '四川省'
  UNION ALL SELECT '武侯区', '成都市', '四川省'
  UNION ALL SELECT '成华区', '成都市', '四川省'
  UNION ALL SELECT '龙泉驿区', '成都市', '四川省'
  UNION ALL SELECT '青白江区', '成都市', '四川省'
  UNION ALL SELECT '新都区', '成都市', '四川省'
  UNION ALL SELECT '温江区', '成都市', '四川省'
  UNION ALL SELECT '双流区', '成都市', '四川省'
  UNION ALL SELECT '郫都区', '成都市', '四川省'
  UNION ALL SELECT '新津区', '成都市', '四川省'

  -- ---------- 贵阳市 6 ----------
  UNION ALL SELECT '南明区', '贵阳市', '贵州省'
  UNION ALL SELECT '云岩区', '贵阳市', '贵州省'
  UNION ALL SELECT '花溪区', '贵阳市', '贵州省'
  UNION ALL SELECT '乌当区', '贵阳市', '贵州省'
  UNION ALL SELECT '白云区', '贵阳市', '贵州省'
  UNION ALL SELECT '观山湖区', '贵阳市', '贵州省'

  -- ---------- 昆明市 7 ----------
  UNION ALL SELECT '五华区', '昆明市', '云南省'
  UNION ALL SELECT '盘龙区', '昆明市', '云南省'
  UNION ALL SELECT '官渡区', '昆明市', '云南省'
  UNION ALL SELECT '西山区', '昆明市', '云南省'
  UNION ALL SELECT '东川区', '昆明市', '云南省'
  UNION ALL SELECT '呈贡区', '昆明市', '云南省'
  UNION ALL SELECT '晋宁区', '昆明市', '云南省'

  -- ---------- 拉萨市 3 ----------
  UNION ALL SELECT '城关区', '拉萨市', '西藏自治区'
  UNION ALL SELECT '堆龙德庆区', '拉萨市', '西藏自治区'
  UNION ALL SELECT '达孜区', '拉萨市', '西藏自治区'

  -- ---------- 西安市 11 ----------
  UNION ALL SELECT '新城区', '西安市', '陕西省'
  UNION ALL SELECT '碑林区', '西安市', '陕西省'
  UNION ALL SELECT '莲湖区', '西安市', '陕西省'
  UNION ALL SELECT '灞桥区', '西安市', '陕西省'
  UNION ALL SELECT '未央区', '西安市', '陕西省'
  UNION ALL SELECT '雁塔区', '西安市', '陕西省'
  UNION ALL SELECT '阎良区', '西安市', '陕西省'
  UNION ALL SELECT '临潼区', '西安市', '陕西省'
  UNION ALL SELECT '长安区', '西安市', '陕西省'
  UNION ALL SELECT '高陵区', '西安市', '陕西省'
  UNION ALL SELECT '鄠邑区', '西安市', '陕西省'

  -- ---------- 兰州市 5 ----------
  UNION ALL SELECT '城关区', '兰州市', '甘肃省'
  UNION ALL SELECT '七里河区', '兰州市', '甘肃省'
  UNION ALL SELECT '西固区', '兰州市', '甘肃省'
  UNION ALL SELECT '安宁区', '兰州市', '甘肃省'
  UNION ALL SELECT '红古区', '兰州市', '甘肃省'

  -- ---------- 西宁市 4 ----------
  UNION ALL SELECT '城东区', '西宁市', '青海省'
  UNION ALL SELECT '城中区', '西宁市', '青海省'
  UNION ALL SELECT '城西区', '西宁市', '青海省'
  UNION ALL SELECT '城北区', '西宁市', '青海省'

  -- ---------- 银川市 3 ----------
  UNION ALL SELECT '兴庆区', '银川市', '宁夏回族自治区'
  UNION ALL SELECT '西夏区', '银川市', '宁夏回族自治区'
  UNION ALL SELECT '金凤区', '银川市', '宁夏回族自治区'

  -- ---------- 乌鲁木齐市 7 ----------
  UNION ALL SELECT '天山区', '乌鲁木齐市', '新疆维吾尔自治区'
  UNION ALL SELECT '沙依巴克区', '乌鲁木齐市', '新疆维吾尔自治区'
  UNION ALL SELECT '新市区', '乌鲁木齐市', '新疆维吾尔自治区'
  UNION ALL SELECT '水磨沟区', '乌鲁木齐市', '新疆维吾尔自治区'
  UNION ALL SELECT '头屯河区', '乌鲁木齐市', '新疆维吾尔自治区'
  UNION ALL SELECT '米东区', '乌鲁木齐市', '新疆维吾尔自治区'
  UNION ALL SELECT '达坂城区', '乌鲁木齐市', '新疆维吾尔自治区'
) v
JOIN `province_info` p ON p.`name` = v.province
JOIN `city_info` c ON c.`name` = v.city AND c.`province_id` = p.`id`;

DELETE t FROM `tmp_district` t
  JOIN `district_info` d ON d.`name` = t.`name` AND d.`city_id` = t.`city_id`;

INSERT INTO `district_info` (`name`, `city_id`, `create_time`, `update_time`, `is_deleted`)
SELECT `name`, `city_id`, NOW(), NOW(), 0 FROM `tmp_district`;

DROP TEMPORARY TABLE IF EXISTS `tmp_district`;


-- =============================================================================
--  4. 结果自查（执行完会打印这三列）
-- =============================================================================
SELECT
  (SELECT COUNT(*) FROM `province_info`) AS 省,
  (SELECT COUNT(*) FROM `city_info`)     AS 市,
  (SELECT COUNT(*) FROM `district_info`) AS 区县;
