# 归心寓 · 用户端网页版（PC）

移动端 H5 的桌面版，**功能对齐移动端**、**视觉风格沿用移动端**（同一套配色与卡片语言），
登录方式为**账号 + 密码**。

- 技术栈：Vue 3 + Vite 5 + TypeScript + **Element Plus** + Pinia + vue-router
- 接口：与移动端共用同一套 `/app/**`（后端 `web/web-app`，端口 8081）
- 开发端口：**5175**
- 路由模式：**hash**（`/#/search`），刷新和静态托管都不会 404

---

## 启动

```powershell
cd D:\JavaProject\lease\web-app-pc
npm install
npm run dev
```

打开 <http://localhost:5175>

前提：`web-app`（8081）已启动。

> 老数据库需要先执行一次 `sql/upgrade-01-add-username.sql`（给 `user_info` 加 `username` 字段），
> 否则账号密码登录会报 `Unknown column 'username'`。

**测试账号**（密码都是 `123456`）：

| 账号 | 密码 | 手机号 | 昵称 |
| --- | --- | --- | --- |
| `zhangsan` | `123456` | 13800000001 | 张三 |
| `lisi` | `123456` | 13800000002 | 李四 |
| `wangwu` | `123456` | 13800000003 | 王五 |

没有账号就点登录卡片下方的「**立即注册**」：填 账号 / 密码 / 手机号（昵称选填），
注册成功后**自动登录并跳回首页**，不用再登一次。

---

## 与移动端的对应关系

| 功能 | 移动端（Vant） | 网页版（Element Plus） |
| --- | --- | --- |
| 登录 | 账号 + 密码 | **账号 + 密码**（同一个接口） |
| 找房 | `views/search` | `/search`：地区级联 + 租金 + 付款方式 + 排序 + 关键字 + 分页 |
| 公寓详情 | `apartmentDetail` | `/apartment/:id`：轮播 + 配套 + 标签 + 在租房源 |
| 房间详情 | `roomDetail` | `/room/:id`：轮播 + 属性 + 配套 + 杂费 + 支付方式 + 租期 |
| 预约看房 | `appointment` | `/appointment/create`：表单提交 |
| 我的预约 | `myAppointment` | `/appointment/mine`：列表 + 详情弹窗 + 取消 |
| 签约 | `agreement` | `/agreement/sign`：可续约（带 `renewId`） |
| 我的租约 | `myAgreement` | `/agreement/mine`：按状态筛选 + 取消签约 / 申请退租 / 撤销退租 / 续约 |
| 租约详情 | — | `/agreement/:id` |
| 浏览历史 | `browsingHistory` | `/history`：分页网格 |
| 个人中心 | `userCenter` | `/user`：改昵称 + 上传头像 |
| 圈子 / 消息 | 移动端是模板假页面 | **未移植**（后端无对应接口） |

> AI 问答功能已按需求从后端整体移除，移动端和网页版都没有。

---

## 视觉规范（奶油原木风，与移动端一致）

| 层级 | 项 | 值 |
| --- | --- | --- |
| **60% 主色** | 页面底色 | `#F9F7F2` 米白 / 燕麦 |
| | 卡片 / 导航 | `#FFFDFA` 暖白 |
| **30% 辅助色** | 区块 / 标签底 | `#E6D5C3` 奶茶 |
| | 分隔 / 描边 | `#D4B895` 浅木 |
| **10% 点缀色** | CTA 按钮 / 价格 / 选中态 | `#E07A5F` 陶土橙 |
| | 高亮 / 星级 | `#F2CC8F` 沙金 |
| 文字 | 主 / 次 / 弱 | `#3D3733` / `#6B615A` / `#9C928A`（暖灰，不用纯黑） |
| 卡片 | 暖白底 + `10px` 圆角 + 暖色阴影 | — |

Element Plus 的主题色通过 CSS 变量覆盖成陶土橙：

```css
--el-color-primary: #e07a5f;
--el-color-primary-light-3: #e9a28f;
--el-color-primary-light-5: #f0bcaf;
--el-color-primary-light-7: #f6d7cf;
--el-color-primary-light-8: #f9e4df;
--el-color-primary-light-9: #fcf2ef;
--el-color-primary-dark-2: #b3624c;
```

---

## 目录结构

```
src/
├── api/index.ts        # 所有接口（login / region / apartment / room / appointment / agreement / history / user）
├── components/
│   └── RoomCard.vue    # 房源卡片
├── layout/index.vue    # 顶部导航 + 页脚
├── router/index.ts     # 路由 + 登录守卫
├── stores/user.ts      # token / 用户信息
├── styles/index.css    # 设计变量 + Element Plus 主题覆盖
├── types/api.ts        # 与后端 VO 一一对应的类型
├── utils/
│   ├── request.ts      # axios 封装（ACCESS_TOKEN / code 200 / 失效码 501,601,602）
│   └── format.ts       # 状态字典、金额、日期、租期格式化
└── views/
    ├── login/          # 账号密码登录
    ├── search/         # 找房（首页）
    ├── apartment/      # 公寓详情
    ├── room/           # 房间详情
    ├── appointment/    # 预约（create / mine）
    ├── agreement/      # 租约（sign / mine / detail）
    ├── history/        # 浏览历史
    ├── user/           # 个人中心
    └── error/          # 404
```

---

## 接口约定（与移动端相同）

- 返回 `{ code, message, data }`，`code === 200` 成功
- 请求头 `ACCESS_TOKEN` 带 JWT
- 失效码 `[501, 601, 602]` → 清 token 并跳转登录页（带 `redirect` 参数）
- 状态字段是数字 code：预约 `1/2/3`，租约 `1~7`，租约来源 `1/2`

---

## 注意

- 房间「已出租」判定与后端一致：`lease_agreement.status in (2,5,7)`
- 签约要求房间**已发布且未被占用**，否则后端会拒绝并提示
- 上传的图片走 `/upload/**`，vite 已配代理到 8081
