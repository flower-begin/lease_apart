/**
 * =============================================================================
 *  「分享给别人看」用的通用静态服务器（零依赖，只用 Node 内置模块）
 *
 *  支持 PC 租客端 / 移动端 H5 —— 用 --dist 指定各自的产物目录即可。
 *
 *  为什么不用 npm run dev：
 *    dev server 是按需编译 + 带 HMR 的开发服务器，
 *    经隧道访问时 ws 会反复断线、首屏几秒才出，别人打开体验很差。
 *    这里直接服务 build 出来的 dist/，并自己代理 /app 和 /upload，
 *    只有一个进程、秒开、没有 HMR。
 *
 *  用法：
 *    node tools/share-static.mjs --dist web-app-pc/dist --port 8088 --name "PC 租客端"
 *    node tools/share-static.mjs --dist web-app-frontend/dist --port 8888 --name "移动端 H5"
 *
 *  可选参数：
 *    --dist <dir>    产物目录（默认 web-app-pc/dist）
 *    --port <n>      监听端口（默认 8088）
 *    --api <url>     后端地址（默认 http://127.0.0.1:8081）
 *    --name <text>   打印用的名字
 * =============================================================================
 */

import http from 'node:http'
import { createReadStream, existsSync, statSync } from 'node:fs'
import { extname, join, resolve } from 'node:path'
import { networkInterfaces } from 'node:os'

/* ------------------------------- 参数解析 ------------------------------- */
function arg(name, fallback) {
  const i = process.argv.indexOf(`--${name}`)
  return i >= 0 && process.argv[i + 1] ? process.argv[i + 1] : fallback
}

const DIST = resolve(arg('dist', 'web-app-pc/dist'))
const PORT = Number(arg('port', process.env.SHARE_PORT || 8088))
const API = arg('api', process.env.SHARE_API || 'http://127.0.0.1:8081')
const NAME = arg('name', '静态站点')

const MIME = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.mjs': 'text/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.svg': 'image/svg+xml',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.webp': 'image/webp',
  '.gif': 'image/gif',
  '.ico': 'image/x-icon',
  '.woff': 'font/woff',
  '.woff2': 'font/woff2',
  '.ttf': 'font/ttf',
  '.map': 'application/json; charset=utf-8'
}

/** 需要转发到后端的路径前缀 */
const PROXY_PREFIXES = ['/app', '/upload']

/* -------------------------------- 日志 -------------------------------- */
function log(...args) {
  const t = new Date().toTimeString().slice(0, 8)
  console.log(`\x1b[90m${t}\x1b[0m`, ...args)
}

/* -------------------------------- 代理 -------------------------------- */
function proxy(req, res) {
  const target = new URL(API)
  const upstream = http.request(
    {
      hostname: target.hostname,
      port: target.port || 80,
      path: req.url,
      method: req.method,
      headers: { ...req.headers, host: target.host }
    },
    up => {
      res.writeHead(up.statusCode || 502, up.headers)
      up.pipe(res)
    }
  )
  upstream.on('error', e => {
    log(`\x1b[31m代理失败 ${req.method} ${req.url} → ${API}：${e.message}\x1b[0m`)
    res.writeHead(502, { 'content-type': 'text/plain; charset=utf-8' })
    res.end(`代理到后端 ${API} 失败：${e.message}\n\n检查一下：后端 web-app（8081）起来了吗？`)
  })
  req.pipe(upstream)
}

/* ------------------------------- 静态文件 ------------------------------- */
function sendFile(res, filePath) {
  const type = MIME[extname(filePath).toLowerCase()] || 'application/octet-stream'
  const isHashed = /-[A-Za-z0-9_]{8,}\.(js|css)$/.test(filePath)
  res.writeHead(200, {
    'content-type': type,
    // 带 hash 的产物可以长期缓存，index.html 不能缓存
    'cache-control': isHashed ? 'public, max-age=31536000, immutable' : 'no-cache'
  })
  createReadStream(filePath).pipe(res)
}

/* -------------------------------- 服务 -------------------------------- */
const INDEX = join(DIST, 'index.html')

if (!existsSync(INDEX)) {
  console.error(`\n[错误] 找不到 ${INDEX}\n` +
    `请先构建产物，例如：\n` +
    `    cd web-app-pc        && npm run build -- --mode share\n` +
    `    cd web-app-frontend  && npm run build -- --mode share\n`)
  process.exit(1)
}

const server = http.createServer((req, res) => {
  const rawUrl = req.url || '/'
  const pathname = decodeURIComponent(rawUrl.split('?')[0])

  if (PROXY_PREFIXES.some(p => pathname === p || pathname.startsWith(p + '/'))) {
    log(`\x1b[36mPROXY\x1b[0m ${req.method} ${pathname}`)
    return proxy(req, res)
  }

  // 防目录穿越：解析后的绝对路径必须仍在 DIST 里
  let filePath = resolve(join(DIST, pathname))
  if (!filePath.startsWith(DIST)) {
    res.writeHead(403, { 'content-type': 'text/plain; charset=utf-8' })
    return res.end('403')
  }
  if (!existsSync(filePath) || statSync(filePath).isDirectory()) {
    // 单页应用：任何未知路径都回退到 index.html（hash / history 路由都适用）
    filePath = INDEX
  }
  log(`\x1b[32mFILE \x1b[0m ${req.method} ${pathname}`)
  sendFile(res, filePath)
})

server.on('error', e => {
  if (e.code === 'EADDRINUSE') {
    console.error(`\n[错误] 端口 ${PORT} 已被占用。`)
    console.error(`       如果 ${PORT} 是 dev server（npm run dev）在跑，先把它停掉，`)
    console.error(`       或者换个端口： --port ${PORT + 1}\n`)
  } else {
    console.error('\n[错误] 启动失败：', e.message, '\n')
  }
  process.exit(1)
})

server.listen(PORT, '0.0.0.0', () => {
  const ips = Object.values(networkInterfaces())
    .flat()
    .filter(i => i && i.family === 'IPv4' && !i.internal)
    .map(i => i.address)

  console.log('\n' + '='.repeat(70))
  console.log(`  ${NAME} · 分享服务已启动`)
  console.log('='.repeat(70))
  console.log(`  产物目录 : ${DIST}`)
  console.log(`  后端代理 : /app  /upload  →  ${API}`)
  console.log('')
  console.log(`  本机     : http://localhost:${PORT}`)
  ips.forEach(ip => console.log(`  局域网   : http://${ip}:${PORT}`))
  console.log('')
  console.log('  想让外网访问，另开一个窗口跑：')
  console.log(`      hostc ${PORT}`)
  console.log('  （或 ngrok / cloudflare tunnel 指到同一个端口）')
  console.log('='.repeat(70) + '\n')
})
