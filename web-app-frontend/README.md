# 归心寓 · 用户端 H5

移动端（C 端用户）前端，配合本项目后端 `web/web-app`（端口 **8081**）使用。

- 技术栈：Vue 3.3 + Vite 4 + TypeScript + **Vant 4** + Pinia + Less + Tailwind
- 移动端适配：`cnjm-postcss-px-to-viewport`，按 **375** 设计稿转 vw
- 接口前缀：`/app`（vite 代理到 `http://localhost:8081`）
- 开发端口：**8888**

> 脚手架基于开源项目 `vue3-h5-template`，页面与接口已全部替换为归心寓业务。

---

## 启动

```powershell
npm install        # 首次
npm run dev        # → http://localhost:8888
```

前提：`web-app`（8081）已启动，并且已经导入过 `sql/lease.sql`（老库还要跑一次 `sql/upgrade-01-add-username.sql`）。

**登录方式：账号 + 密码**

| 账号 | 密码 | 昵称 |
| --- | --- | --- |
| `zhangsan` | `123456` | 张三 |
| `lisi` | `123456` | 李四 |
| `wangwu` | `123456` | 王五 |

没有账号就在登录页点「**立即注册**」：填 账号 / 密码 / 手机号（昵称选填），注册成功后自动登录。

其他命令：

```powershell
npm run build       # 生产构建
npm run preview     # 预览构建产物
npm run type-check  # vue-tsc 类型检查
```

---

## 环境变量（`.env.development`）

| 变量 | 值 | 说明 |
| --- | --- | --- |
| `VITE_API_TITLE` | `归心寓` | 浏览器标题 |
| `VITE_APP_PORT` | `8888` | dev server 端口 |
| `VITE_APP_BASE_API` | `/app` | 接口前缀 |
| `VITE_APP_BASE_URL` | `http://localhost:8081` | 后端地址（vite 代理目标） |

换端口 / 换后端，只改这个文件，不用动代码。

---

## 页面

**底部 TabBar（`src/router/tabBarRoutes.ts`）**

| 页面 | 路径 | 文件 |
| --- | --- | --- |
| 找房 | `/search` | `views/search/search.vue` |
| 圈子 | `/group` | `views/group/group.vue` |
| 我的房间 | `/myRoom` | `views/myRoom/myRoom.vue` |
| 消息 | `/message` | `views/message/message.vue` |
| 个人中心 | `/userCenter` | `views/userCenter/userCenter.vue` |

**普通页面（`src/router/otherRoutes.ts`）**

| 页面 | 路径 | 文件 |
| --- | --- | --- |
| 房间详情 | `/roomDetail` | `views/roomDetail/roomDetail.vue` |
| 公寓详情 | `/apartmentDetail` | `views/apartmentDetail/apartmentDetail.vue` |
| 预约看房 | `/appointment` | `views/appointment/appointment.vue` |
| 我的预约 | `/myAppointment` | `views/myAppointment/myAppointment.vue` |
| 我的租约 | `/myAgreement` | `views/myAgreement/myAgreement.vue` |
| 租约详情 | `/agreement` | `views/agreement/agreement.vue` |
| 浏览历史 | `/browsingHistory` | `views/browsingHistory/browsingHistory.vue` |
| 登录 | `/login` | `views/login/login.vue` |

> ⚠️ `圈子`(`group`) 和 `消息`(`message`) 这两个 tab 是脚手架留下的**写死假数据的 demo 页**
> （`group.vue` 里就是 `v-for="item in 5"` + 假昵称/假帖子），后端没有对应接口。
> 其余 11 个页面对应的 `/app/**` 接口后端都已实现。
>
> 如果想让 App 看起来完整，可以把这两个 tab 从 `src/router/tabBarRoutes.ts` 里去掉，
> TabBar 会自动少两项。

---

## 目录结构

```
src/
├── api/            # 接口封装（search / user / template）
├── components/     # Tabbar、NavBar、SearchBar、ApartmentCard、RoomCard、SvgIcon…
├── enums/          # 状态枚举（与后端 @EnumValue 的 code 对齐）
├── hooks/          # useIntersectionObserver、useToggleDarkMode、useToLoginPage
├── icons/svg/      # 自动注册的 svg 图标
├── layout/         # 带 TabBar 的布局
├── router/         # routes / tabBarRoutes / otherRoutes
├── store/          # pinia（user / cachedView / darkMode）
├── styles/         # tailwind + less mixin
├── utils/          # http(axios 封装) / token / progress / validate
└── views/          # 13 个页面
```

---

## 与后端约定

- 接口返回 `{ code, message, data }`，`code === 200` 成功
- 登录态放请求头 `access_token`（JWT，1 小时）
- 失效码 `[501, 601, 602]` → 清 token 并弹登录框
- 状态字段用数字 code（如租约状态 `1~7`、预约状态 `1/2/3`）
