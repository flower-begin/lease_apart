#!/usr/bin/env node
/**
 * =============================================================================
 *  全国省市区数据导入（幂等，不破坏已有数据）
 *
 *  做什么：
 *    1. 从公开镜像下载「省-市-区」三级行政区划数据（约 34 省 / 340 市 / 2900 区县）
 *    2. 生成一份纯 SQL：sql/region-full.sql
 *    3. SQL 全部是 INSERT ... SELECT ... WHERE NOT EXISTS，所以：
 *         - 已有的省/市/区 **不会重复插入**，id 保持不变
 *         - 你种子公寓里引用的 province_id / city_id / district_id 不会被破坏
 *         - 可以反复执行
 *
 *  用法（项目根目录）：
 *      node tools/import-regions.mjs
 *      node tools/import-regions.mjs --out sql/region-full.sql
 *      node tools/import-regions.mjs --offline        # 只用本地缓存，不联网
 *
 *  生成之后执行 SQL：
 *      mysql -uroot -p123456 --default-character-set=utf8mb4 lease < sql/region-full.sql
 *    或用 Navicat / DataGrip 打开 sql/region-full.sql 整体执行。
 * =============================================================================
 */

import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = dirname(fileURLToPath(import.meta.url))
const ROOT = resolve(__dirname, '..')
const CACHE_DIR = resolve(__dirname, '.cache')
const CACHE_FILE = resolve(CACHE_DIR, 'pca-code.json')

/* ---------------------------------- 参数 ---------------------------------- */
const argv = process.argv.slice(2)
const offline = argv.includes('--offline')
const outIndex = argv.indexOf('--out')
const OUT_FILE = resolve(ROOT, outIndex >= 0 ? argv[outIndex + 1] : 'sql/region-generated.sql')

/* ------------------------------- 数据源镜像 ------------------------------- */
const MIRRORS = [
  'https://unpkg.com/china-division@2.7.0/dist/pca-code.json',
  'https://cdn.jsdelivr.net/npm/china-division@2.7.0/dist/pca-code.json',
  'https://raw.githubusercontent.com/modood/Administrative-divisions-of-China/master/dist/pca-code.json',
  'https://cdn.jsdelivr.net/gh/modood/Administrative-divisions-of-China@master/dist/pca-code.json'
]

/* --------------------------------- 工具函数 -------------------------------- */
function log(msg) {
  console.log(msg)
}

/** SQL 单引号转义 */
function q(str) {
  return `'${String(str).replace(/'/g, "''")}'`
}

/** 直辖市 / 省直辖的占位城市名，改写成所属省份名（让「北京市 → 北京市 → 朝阳区」更好看） */
const PLACEHOLDER_CITY_NAMES = new Set(['市辖区', '县', '省直辖县级行政区划', '自治区直辖县级行政区划'])

function normalizeCityName(provinceName, cityName) {
  if (cityName === '市辖区' || cityName === '县') {
    return provinceName
  }
  return cityName
}

/* --------------------------------- 下载数据 -------------------------------- */
async function loadDataset() {
  if (existsSync(CACHE_FILE)) {
    log(`[1/3] 使用本地缓存：${CACHE_FILE}`)
    return JSON.parse(readFileSync(CACHE_FILE, 'utf8'))
  }

  if (offline) {
    throw new Error('本地没有缓存，且指定了 --offline。先去联网跑一次。')
  }

  for (const url of MIRRORS) {
    try {
      log(`[1/3] 尝试下载：${url}`)
      const res = await fetch(url, { signal: AbortSignal.timeout(30000) })
      if (!res.ok) {
        log(`      失败（HTTP ${res.status}）`)
        continue
      }
      const data = await res.json()
      if (!Array.isArray(data) || !data.length) {
        log('      失败（返回内容不是数组）')
        continue
      }
      mkdirSync(CACHE_DIR, { recursive: true })
      writeFileSync(CACHE_FILE, JSON.stringify(data), 'utf8')
      log(`      成功，已缓存到 ${CACHE_FILE}`)
      return data
    } catch (e) {
      log(`      失败：${e.message}`)
    }
  }

  throw new Error(
    '所有镜像都下载失败。\n' +
      '可以手动下载 pca-code.json 放到 tools/.cache/pca-code.json 再重跑 --offline。\n' +
      '数据源：https://github.com/modood/Administrative-divisions-of-China'
  )
}

/* --------------------------------- 生成 SQL -------------------------------- */
function buildSql(dataset) {
  const lines = []
  let provinceCount = 0
  let cityCount = 0
  let districtCount = 0

  lines.push('-- =============================================================================')
  lines.push('--  全国省 / 市 / 区 数据导入（幂等：可重复执行，已存在的不会重复插入）')
  lines.push('--')
  lines.push('--  由 tools/import-regions.mjs 自动生成，请勿手工编辑。')
  lines.push(`--  数据源：modood/Administrative-divisions-of-China (pca-code.json)`)
  lines.push('--')
  lines.push('--  执行方式：')
  lines.push('--    mysql -uroot -p --default-character-set=utf8mb4 lease < sql/region-full.sql')
  lines.push('--    或用 Navicat / DataGrip 打开本文件整体执行')
  lines.push('-- =============================================================================')
  lines.push('')
  lines.push('SET NAMES utf8mb4;')
  lines.push('')

  lines.push('-- ---------------------------- 省级行政区 ----------------------------')
  for (const province of dataset) {
    const name = province.name
    lines.push(
      'INSERT INTO `province_info` (`name`, `create_time`, `update_time`, `is_deleted`)\n' +
        `SELECT ${q(name)}, NOW(), NOW(), 0 FROM DUAL\n` +
        `WHERE NOT EXISTS (SELECT 1 FROM \`province_info\` WHERE \`name\` = ${q(name)});`
    )
    provinceCount++
  }
  lines.push('')

  lines.push('-- ---------------------------- 地级市 ----------------------------')
  for (const province of dataset) {
    const provinceName = province.name
    for (const city of province.children || []) {
      const cityName = normalizeCityName(provinceName, city.name)
      lines.push(
        'INSERT INTO `city_info` (`name`, `province_id`, `create_time`, `update_time`, `is_deleted`)\n' +
          `SELECT ${q(cityName)}, p.\`id\`, NOW(), NOW(), 0\n` +
          'FROM `province_info` p\n' +
          `WHERE p.\`name\` = ${q(provinceName)}\n` +
          `  AND NOT EXISTS (SELECT 1 FROM \`city_info\` c WHERE c.\`name\` = ${q(cityName)} AND c.\`province_id\` = p.\`id\`)\n` +
          'LIMIT 1;'
      )
      cityCount++

      for (const district of city.children || []) {
        const districtName = district.name
        lines.push(
          'INSERT INTO `district_info` (`name`, `city_id`, `create_time`, `update_time`, `is_deleted`)\n' +
            `SELECT ${q(districtName)}, c.\`id\`, NOW(), NOW(), 0\n` +
            'FROM `city_info` c\n' +
            'JOIN `province_info` p ON p.`id` = c.`province_id`\n' +
            `WHERE p.\`name\` = ${q(provinceName)} AND c.\`name\` = ${q(cityName)}\n` +
            `  AND NOT EXISTS (SELECT 1 FROM \`district_info\` d WHERE d.\`name\` = ${q(districtName)} AND d.\`city_id\` = c.\`id\`)\n` +
            'LIMIT 1;'
        )
        districtCount++
      }
    }
  }

  lines.push('')
  lines.push('-- ---------------------------- 导入结果自查 ----------------------------')
  lines.push('SELECT')
  lines.push('  (SELECT COUNT(*) FROM `province_info`) AS 省,')
  lines.push('  (SELECT COUNT(*) FROM `city_info`)     AS 市,')
  lines.push('  (SELECT COUNT(*) FROM `district_info`) AS 区县;')
  lines.push('')

  return { sql: lines.join('\n'), provinceCount, cityCount, districtCount }
}

/* ----------------------------------- 主流程 --------------------------------- */
try {
  log('')
  log('==============================================================================')
  log('  全国省市区数据导入')
  log('==============================================================================')
  log('')

  const dataset = await loadDataset()
  log(`[2/3] 解析完成：${dataset.length} 个省级行政区`)

  const { sql, provinceCount, cityCount, districtCount } = buildSql(dataset)

  mkdirSync(dirname(OUT_FILE), { recursive: true })
  writeFileSync(OUT_FILE, sql, 'utf8')

  log(`[3/3] 已生成 SQL：${OUT_FILE}`)
  log('')
  log('  待导入（全部是幂等 upsert，不会重复）：')
  log(`    省级   ${provinceCount}`)
  log(`    地级市 ${cityCount}`)
  log(`    区县   ${districtCount}`)
  log('')
  log('  下一步执行 SQL：')
  log('    mysql -uroot -p123456 --default-character-set=utf8mb4 lease < sql/region-generated.sql')
  log('  或用 Navicat 打开 sql/region-generated.sql 整体执行。')
  log('')
  log('  执行完刷新前端，地区选择器就是全国数据了。')
  log('')
} catch (e) {
  console.error('')
  console.error('[失败] ' + e.message)
  console.error('')
  process.exit(1)
}
