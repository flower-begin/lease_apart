# 尚庭公寓（lease）

一个完整的房屋租赁平台，包含 **后台管理系统**、**用户端（H5）** 和 **两个 Spring Boot 服务**。

> 本 README 面向「clone 下来就能跑」。按下面顺序操作，15 分钟内可以全部启动。

---

## 一、模块一览

| 目录 | 说明 | 端口 | 技术栈 |
| --- | --- | --- | --- |
| `common/` | 公共模块：统一返回、全局异常、JWT、Redis/缓存、MyBatis-Plus 配置、阿里云短信 | — | Spring Boot 3 |
| `model/` | 实体与枚举（被两个 web 模块共用） | — | MyBatis-Plus |
| `web/web-admin/` | **后台管理服务**（B 端接口，前缀 `/admin`） | 8080 | Spring Boot 3 + MyBatis-Plus + Knife4j |
| `web/web-app/` | **用户端服务**（C 端接口，前缀 `/app`） | 8081 | Spring Boot 3 + MyBatis-Plus + Knife4j |
| `web-app-frontend/` | **用户端 H5**（官方版，13 个页面；由 `rentHouseH5` 改名而来） | 8888 | Vue 3 + Vite + Vant 4 + Pinia + Less + Tailwind |
| `web-admin-frontend/` | **后台管理界面**（视觉/布局按 `rentHouseAdmin` 规范重做） | 5174 | Vue 3 + Vite + Element Plus + TS |
| `rentHouseAdmin/`（项目外，可选） | 官方后台模板，接口与本项目完全兼容，可替换上一行 | 5174 | Vue 3 + Vite + Element Plus + ECharts + TS |
| `sql/lease.sql` | 建库建表 + 初始化数据（可重复执行） | — | MySQL 8 |
| `log/` | 运行日志（自动创建） | — | — |

---

## 一.5、最快上手（不想敲命令就用这个）

`tools/` 下有几个**双击就能跑**的 bat（已自动绕过 PowerShell 执行策略）：

| 文件 | 作用 |
| --- | --- |
| `tools\finalize-web-app.bat` | **第一次跑这个**：把 `rentHouseH5` 改名成 `web-app-frontend`、删掉旧的、并删除后端 AI 问答代码 |
| `tools\start-all.bat` | 一次起 4 个窗口：8080 / 8081 / 5174 / 8888 |
| `tools\verify.bat` | 环境 + 编译 + 类型检查自检 |

顺序：**先双击 `finalize-web-app.bat` → 再双击 `start-all.bat`**。

等价的手写命令：

```powershell
cd D:\JavaProject\lease
.\tools\finalize-web-app.ps1 -KeepBackup   # 改名 + 删旧(先备份) + 清 AI 代码
.\tools\start-all.ps1                      # 起四个服务
```

> 如果提示「禁止运行脚本」，用 bat 版本即可（它内部用的是 `-ExecutionPolicy Bypass`）。

---

## 二、环境要求

| 依赖 | 版本 | 是否必须 |
| --- | --- | --- |
| JDK | 17+ | 必须 |
| Maven | 3.8+ | 必须 |
| MySQL | 8.0+ | 必须（可用 `docker compose up -d` 代替，见第三节） |
| Node.js | 18+ | 必须（两个前端） |
| Redis | 6+ | **可选**，未启动时验证码自动降级为进程内缓存 |
| Docker | 任意 | 可选，用来一键起 MySQL + Redis |

---

## 三、初始化数据库

### 方式一：用本机 MySQL

```powershell
mysql -uroot -p --default-character-set=utf8mb4 < D:\JavaProject\lease\sql\lease.sql
```

### 方式二：用 Docker 一键起依赖（推荐，含 Redis）

如果本机没装 MySQL/Redis，或者不想污染本机环境：

```powershell
docker compose up -d
```

它会起两个容器：

| 容器 | 端口 | 说明 |
| --- | --- | --- |
| `lease-mysql` | 3306 | root / `123456`，**首次启动自动执行 `sql/lease.sql`** |
| `lease-redis` | 6379 | 无密码，验证码会存这里（不装也能跑，会自动降级到内存） |

等 MySQL 健康后即可直接启动后端。想重新初始化数据：

```powershell
docker compose down -v
docker compose up -d
```

> 端口冲突时改 `docker-compose.yml` 里的 `ports`（例如 `"3307:3306"`），
> 并同步改两个后端 `application.yaml` 里的 `url` / `spring.data.redis.port`。

### 初始化数据说明

脚本会创建数据库 `lease`、28 张表 + 1 个只读视图，并写入种子数据：

- 公寓 6 个（望京SOHO / 中关村 / 回龙观 / 陆家嘴 / 天河 / 深圳湾）
- 房间 12 间（每公寓 2 间）
- 后台账号：`admin` / `123456`（另有 `operator`、`cleaner`，密码同）
- App 测试用户：`13800000001` 张三、`13800000002` 李四、`13800000003` 王五

> ⚠️ 脚本开头是 `DROP TABLE IF EXISTS`，**会清空同名表**，不要在生产库执行。

数据库连接配置在：

- `web/web-admin/src/main/resources/application.yaml`
- `web/web-app/src/main/resources/application.yaml`

默认都是 `root` / `123456`，按需修改。

---

## 四、启动后端

```powershell
# 在项目根目录
mvn clean install -DskipTests

# 终端 1：后台管理服务（8080）
mvn -pl web/web-admin spring-boot:run

# 终端 2：用户端服务（8081）
mvn -pl web/web-app spring-boot:run
```

接口文档（Knife4j）：

- 后台：<http://localhost:8080/doc.html>
- 用户端：<http://localhost:8081/doc.html>

> 图片上传目录由 `upload.dir` 控制，默认 `D:/upload`。
> 两个服务都会把 `/upload/**` 映射到该目录，前端直接用它拼图片地址即可。

---

## 五、启动前端

### 1. 用户端 H5（`web-app-frontend`，端口 8888）

用的是尚硅谷配套的官方 H5（Vue3 + Vite + Vant4 + Pinia + Less + Tailwind，**13 个页面**）。
最终它就在本项目的 `web-app-frontend\` 里。

**如果你还没改名**（`lease\` 下现在还叫 `rentHouseH5`），双击一次即可：

```
tools\finalize-web-app.bat
```

它做三件事：

1. 把 `rentHouseH5` **重命名**为 `web-app-frontend`
2. 删除旧的（自建版）`web-app-frontend`，可选 `-KeepBackup` 先压缩备份
3. 删除后端 AI 问答的 8 个 Java 文件 + 清空对应目录

命令等价写法：

```powershell
cd D:\JavaProject\lease
.\tools\finalize-web-app.ps1 -KeepBackup
```

**启动**

```powershell
cd D:\JavaProject\lease\web-app-frontend
npm run dev
```

打开 <http://localhost:8888>

> 它的 `.env.development` 已经是 `VITE_APP_BASE_API='/app'` + `VITE_APP_BASE_URL='http://localhost:8081'`，
> 和本项目 `web-app` **零改动**对接。成功码 `200`、失效码 `[501,601,602]`、token 头 `ACCESS_TOKEN` 全部一致。
> 想改端口就改这里的 `VITE_APP_PORT`。
>
> 13 个页面：找房 `search`、公寓详情 `apartmentDetail`、房间详情 `roomDetail`、
> 预约 `appointment`、我的预约 `myAppointment`、签约 `agreement`、我的租约 `myAgreement`、
> 我的房间 `myRoom`、浏览历史 `browsingHistory`、个人中心 `userCenter`、登录 `login`、
> 合租 `group`、消息 `message`。
>
> `group`（合租）和 `message`（消息）是模板自带页面，后端没有对应接口，点进去没有数据；
> 主流程（找房 / 详情 / 预约 / 签约 / 我的租约 / 浏览历史 / 个人中心）都有后端支持。

也可以直接用一键脚本启动（它会自动认出 `web-app-frontend` 并读取它自己的端口）：

```powershell
.\tools\start-all.ps1
```

#### 关于移除的 AI 问答

官方 H5 本来就没有 AI 页面，所以按你的要求把后端的 AI 也一起删了：

| 删除项 | 说明 |
| --- | --- |
| `controller/ai/AiChatController.java` | `/app/ai/chat`、`/app/ai/recommend` 两个接口 |
| `service/AiService.java`、`service/impl/AiServiceImpl.java` | DeepSeek 调用逻辑 |
| `vo/ai/ChatRequestVo.java`、`ChatResponseVo.java`、`RecommendRequestVo.java` | AI 的出入参 |
| `custom/config/DeepSeekConfig.java`、`DeepSeekConfigProperties.java` | `RestTemplate` 与配置绑定 |
| `application.yaml` 的 `deepseek.*` 段 | API key 等配置 |
| `application.yaml` 里 `/app/ai/**` 的免登录放行 | — |
| `Knife4jConfiguration` 的 `aiAPI()` 分组 | 接口文档里的「AI助手」分组 |

> 前 8 个文件由 `finalize-web-app.ps1` 删除；yaml 和 Knife4j 我已经直接改好了。
> **删完必须重新编译并重启 `web-app`**，否则 8081 还跑着旧代码。

> 另外官方 H5 没有「确认签约 / 申请退租 / 续约」「改昵称 / 上传头像」这些界面，
> 但**对应后端接口都还在**（`/app/agreement/updateStatusById`、`/app/agreement/renew`、
> `/app/user/update` 等），只是暂时没有前端入口，需要时随时可以在 Knife4j 里调。

### 2. 后台管理界面（5174）

本项目自带的 `web-admin-frontend` 已经**按官方 rentHouseAdmin 的视觉规范重做**，直接用：

```powershell
cd web-admin-frontend
npm install
npm run dev
```

打开 <http://localhost:5174>，用 `admin` / `123456` 登录。

已对齐的设计规范（取自 rentHouseAdmin 的 `variable.scss`）：

| 项 | 值 |
| --- | --- |
| 主色 | `#1890ff` |
| 侧边栏背景 | `#001529`（选中项 `#1890ff`，二级菜单 `#000c17`） |
| 侧边栏宽度 | 256px / 折叠 64px，Logo 区高 75px |
| 顶部导航条 | 60px（折叠按钮 + 面包屑 + 刷新 + 全屏 + 用户下拉） |
| 多标签页 | 55px，可关闭 / 关闭其他 / 关闭全部 |
| 内容区 | `padding: 20px`，背景 `#f0f2f5` |
| 页脚 | 55px，居中版权 |
| 圆角 / 阴影 | `4px` / `0 1px 4px rgb(0 21 41 / 8%)` |
| 登录页 | 左品牌 + 右玻璃拟态登录卡，`hello !` 大标题、48px 输入框、整宽登录按钮 |

#### 也可以换成官方 rentHouseAdmin（可选）

`D:\尚庭公寓\前端环境\前端环境\rentHouseAdmin` 这份**不用改任何代码**就能对接本项目后端（它已经指向 `http://localhost:8080`）：

```powershell
cd D:\尚庭公寓\前端环境\前端环境\rentHouseAdmin
npm run dev
```

> 若用 `D:\尚硅谷java笔记\...\完成版代码\rentHouseAdmin`（纯源码、无 node_modules），
> 先 `npm install`，并**必须把 `.env.development` 里的**
> `VITE_APP_BASE_URL` 从 `'http://139.198.127.41:8001'` 改成 `'http://localhost:8080'`，
> 否则会连到尚硅谷的远程服务器。保持 `VITE_APP_BASE_API='/admin'` 不变。

两种后台前端与本项目后端的约定已核对一致：

| 项目 | 前端期望 | 本项目 web-admin |
| --- | --- | --- |
| 接口前缀 | `/admin/**` | `/admin/**` ✅ |
| 成功码 | `200` | `Result.code = 200` ✅ |
| 登录失效码 | `[305, 601, 602]` | `ADMIN_LOGIN_AUTH / TOKEN_EXPIRED / TOKEN_INVALID` ✅ |
| token 请求头 | `access_token` | `access_token` ✅ |
| 验证码返回 | `{ image, key }` | `CaptchaVo { image, key }` ✅ |
| 登录用户信息 | `{ name, avatarUrl }` | `SystemUserInfoVo { name, avatarUrl }` ✅ |
| 各类状态枚举 | `0/1`、`1~7`、`1/2/3` 等 | `ReleaseStatus / LeaseStatus / AppointmentStatus` ✅ |

> rentHouseAdmin 内置的 mock 全部挂在 `/mock/**` 前缀下，**不会拦截真实接口**。

### 3. 代理说明

三个前端的接口都走 vite 代理，所以不用处理跨域：

| 前端 | 代理前缀 | 转发到 | 配置文件 |
| --- | --- | --- | --- |
| web-app-frontend（H5） | `/app/**` | `VITE_APP_BASE_URL`（默认 `http://localhost:8081`） | `.env.development` |
| rentHouseAdmin | `/admin/**` | `VITE_APP_BASE_URL`（默认 `http://localhost:8080`） | `.env.development` |
| web-admin-frontend | `/api/**` | `http://localhost:8080/admin/**` | `vite.config.ts` |
| web-admin-frontend | `/upload/**` | `http://localhost:8080/upload/**` | `vite.config.ts` |

**`web-admin-frontend` 的端口/后端地址可配置**（不用改代码）：把 `web-admin-frontend/.env.example`
复制成 `.env.local` 再改：

```ini
VITE_API_TARGET=http://localhost:8080   # 后端地址
VITE_PORT=5174                          # 前端端口
```

---

## 六、登录说明（重要）

### 用户端：短信验证码

默认开启了 **本地 mock 模式**（`web-app/src/main/resources/application.yaml` 里的 `lease.sms.mock: true`）：

- 不会真的调用阿里云短信
- 验证码会打印在 `web-app` 的**控制台日志**里，搜索 `短信 mock` 即可看到
- 也可以直接用固定码 **`123456`** 登录（`lease.sms.fixed-code: "123456"`）

想接真实短信：把 `lease.sms.mock` 改为 `false`，并配置自己的 `aliyun.sms.*`。

### 后台管理：账号密码 + 图形验证码

`admin` / `123456`，验证码图片点击可刷新。

> 密码在库里存的是 `md5Hex`（`e10adc3949ba59abbe56e057f20f883e`）。

---

## 七、功能清单

### 后台管理界面（`web-admin-frontend`，已按 rentHouseAdmin 风格重做）

> 下表同时适用于本项目自带的 `web-admin-frontend` 和官方 `rentHouseAdmin`。

- **布局**：深色侧边栏（`#001529`）+ 顶部导航条 + 多标签页 + 页脚，窄屏自动折叠
- **公寓管理**：分页查询（省市区 + 名称）、新增/编辑（含省市区级联、图片、标签、配套、杂费）、发布/下架、删除
- **房间管理**：按公寓筛选、新增/编辑（含属性、配套、标签、支付方式、租期、图片）、发布/下架、删除
- **预约看房**：分页查询、标记已看房/取消
- **租约管理**：分页查询、详情、新增/编辑、状态变更、删除
- **用户管理**：App 用户分页、启用/禁用
- **属性/字典管理**：标签、配套、属性（两级）、杂费（两级）、支付方式、租期
- **系统管理**：员工（增删改查 + 启用禁用）、岗位

> 官方 `rentHouseAdmin` 里还带有「角色 / 菜单权限 / 部门 / 操作日志」等模板页面，
> 这些接口**尚庭公寓后端没有实现**（属于模板遗留），点进去会 404，属正常现象。
> 本项目自带的 `web-admin-frontend` 没有这些无效菜单。

### 用户端 H5（`web-app-frontend`，官方版）

页面（13 个）：

| 页面 | 路径 | 说明 |
| --- | --- | --- |
| 找房 | `search` | 省市区三级联动、租金区间、支付方式、排序、上拉分页 |
| 公寓详情 | `apartmentDetail` | 图片、配套、标签、在租房源、预约入口 |
| 房间详情 | `roomDetail` | 图片、属性、支付方式、租期、配套、标签、杂费 |
| 预约 | `appointment` | 提交/修改预约 |
| 我的预约 | `myAppointment` | 预约列表与详情 |
| 签约 | `agreement` | 提交租约 |
| 我的租约 | `myAgreement` | 租约列表与详情 |
| 我的房间 | `myRoom` | 已租房间 |
| 浏览历史 | `browsingHistory` | 自动记录、分页 |
| 个人中心 | `userCenter` | 用户资料 |
| 登录 | `login` | 手机号 + 短信验证码 |
| 合租 | `group` | 模板页面，后端无接口 |
| 消息 | `message` | 模板页面，后端无接口 |

> **关于省市区树接口**：H5 的 `api/search/index.ts` 里声明了一个 `getRegionList()`
> 调用 `GET /app/region/listAsTree`，但**当前 UI 并没有用它**（SearchBar 用的是
> `province/list` + `city/listByProvinceId` + `district/listByCityId` 三个独立接口）。
> 为了让这个声明不至于 404，后端也把树接口实现了：
>
> ```json
> GET /app/region/listAsTree
> [{ "id": 1, "name": "北京市", "children": [
>     { "id": 1, "name": "北京市", "children": [
>         { "id": 1, "name": "朝阳区" }
>     ]}
> ]}]
> ```
>
> 如果你的页面想改成一次拉全量树，把 SearchBar 里的三个请求换成 `getRegionList()` 即可，
> 后端已经支持。

---

## 八、接口约定

- 所有接口返回 `Result<T>`：`{ code, message, data }`，`code === 200` 表示成功
- 登录态通过请求头 `access_token` 传递（JWT，有效期 1 小时）
- **枚举一律用 code 传输**：JSON 里是数字（`@JsonValue`），查询参数也接受数字
  （两个模块都注册了 `StringToBaseEnumConverterFactory`，例如 `?leaseStatus=2`，也兼容 `?leaseStatus=SIGNED`）
- **房间"已出租"的判定口径**：`lease_agreement.status in (2,5,7)`
  （已签约 / 退租待确认 / 续约待确认）。`签约待确认(1)` 不占用房间，前后端保持一致
- 用户端与后台**各自一套拦截器**：
  - `/app/login/**`、`/app/ai/**` 完全放行
  - `/app/room/**`、`/app/apartment/**` 可选登录（登录了才记录浏览历史）
  - `/app/history/**`、`/app/appointment/**`、`/app/agreement/**`、`/app/info`、`/app/user/**` 必须登录
  - `/admin/login/**` 放行，其余 `/admin/**` 必须登录

---

## 九、常见问题

**1. 后台登录提示「验证码已过期」**

验证码存在 Redis 里（60 秒）。若本机没装 Redis，会自动降级到进程内缓存——但**重启服务后缓存失效**，需要重新获取验证码。

**2. 图片显示不出来**

确认 `upload.dir` 目录存在且可写（默认 `D:/upload`），并且访问的是对应服务的端口（8080 或 8081）。前端已配置 `/upload` 代理。

**3. 用户端登录收不到验证码**

看 `web-app` 控制台日志里的 `【短信 mock】` 那一行；或者直接用 `123456`。

**4. 首次启动报「表不存在」**

没有执行建库脚本，回到第三步。

**5. 端口被占用**

改 `application.yaml` 的 `server.port`，并同步修改对应前端 `vite.config.ts` 的 proxy target。

---

## 十、开发约定

- 新增接口：Controller → Service/Impl → Mapper(+XML) → Entity，两个 web 模块各自独立
- 状态字段全部是枚举 + `@EnumValue`，JSON 里是数字 code，前端用 `utils/dict.ts` 统一映射
- 所有表继承 `BaseEntity`：`id / create_time / update_time / is_deleted`，逻辑删除全局生效
- 分页统一 `current`（从 1 开始）+ `size`（服务端上限 200）

---

## 十一、跑起来之后的自测清单

### 先跑一键自检（推荐）

```powershell
.\tools\verify.ps1              # 检查环境 + 后端编译 + 两个前端类型检查 + 数据库/目录体检
.\tools\verify.ps1 -Install     # 前端缺 node_modules 时自动 npm install
.\tools\verify.ps1 -SkipFrontend  # 只查后端
```

它会依次执行 `mvn -DskipTests compile`、两个前端的 `vue-tsc --noEmit`，并探测数据库是否已初始化。
**如果编译不过，请把它的输出贴出来**——这是最快定位问题的入口。

> 因为仓库代码在交付环境里没有实际编译过（开发环境 shell 不可用），这个脚本就是用来补上这一步的。

### 再按下面的清单点一遍

建议按下面顺序点一遍，覆盖所有主链路：

**后端**
1. `GET http://localhost:8080/doc.html` 与 `http://localhost:8081/doc.html` 能打开
2. `GET http://localhost:8081/app/apartment/list` 返回 6 条公寓，且 `graphVoList` 有 url、`minRent` 有值
3. `GET http://localhost:8081/app/room/pageItem?current=1&size=5` 返回 5 条，`graphVoList` / `labelInfoList` 不为空
4. `GET http://localhost:8081/app/room/pageItem?current=1&size=5&keyword=望京` 能筛出结果
5. `GET http://localhost:8081/upload/xxx.jpg` 能返回图片（先用后台上传一张）

**后台管理（5174）**
6. `admin/123456` + 图形验证码能登录，进入后是深色侧边栏 + 顶部导航 + 标签页的布局
7. 左侧菜单依次是：首页 / 公寓管理 / 房间管理 / 预约看房 / 租约管理 / 用户管理 / 字典管理（6 个子项）/ 系统管理（员工、岗位）；点顶部折叠按钮侧边栏收成 64px；切过两个以上页面后顶部出现可关闭的标签页
8. 公寓管理：列表有 6 条种子数据 → 新增一个公寓（选省市区 + 传图 + 选标签/配套/杂费）→ 列表能看到 → 编辑能回显
9. 公寓管理：点「下架」→ 状态变未发布；再点「发布」→ 变已发布
10. 房间管理：按公寓筛选 → 新增房间（选属性/配套/标签/支付方式/租期 + 传图）→ 列表出现
11. 预约看房：列表有 4 条种子数据，公寓名称不为空；点「已看房」状态变化
12. 租约管理：列表有 4 条；点「详情」能打开（自建版走 `/admin/agreement/getById`，官方版走 `/admin/agreement?id=`，后端两个路径都支持）；按姓名搜「张三」能筛出
13. 用户管理：3 条 App 用户，能禁用/启用
14. 字典管理下的 6 个页面（标签/配套/属性/杂费/支付方式/租期）都能新增、编辑、删除

> 如果你用的是官方 `rentHouseAdmin`，菜单里的「角色管理 / 菜单权限 / 部门 / 操作日志」是模板遗留页面，
> 后端未实现，点进去 404 属正常；本项目自带的 `web-admin-frontend` 没有这些菜单。

**用户端 H5（8888，web-app-frontend）**
15. 首页/找房页能正常渲染，底部 Tabbar 可切换
16. 地区选择能选到「省 → 市 → 区」三层（前端实际走 3 个接口：`province/list`、`city/listByProvinceId`、`district/listByCityId`）
    - 树形接口也可以直接测：`GET http://localhost:8081/app/region/listAsTree`，应返回 4 个省、其下若干市和区
17. 找房：选省市区 + 租金区间 + 排序，列表能筛出结果；上拉能加载第二页
18. 房间详情：图片、属性、支付方式、租期、配套、标签、杂费都有内容
19. 登录：手机号 `13800000001` + 验证码 `123456` 能登录（或看 8081 控制台日志）
20. 预约：提交预约 → 「我的预约」出现记录
21. 签约：提交租约 → 「我的租约」出现记录，状态为「签约待确认」
22. 浏览历史：看过房间后进「浏览历史」有记录
23. 个人中心：能显示当前登录用户
24. 合租/消息两个页面没有后端接口，空白属正常

> 合租/消息两个页面后端没有接口，空白属正常。

---

## 十二、本次补全说明（重要）

本次改动覆盖：

1. **修复了仓库原有的多个阻断性 bug**（详见下节）
2. 补全用户端后端：签约流程、预约校验与取消、浏览历史去重、房间/公寓列表图片与标签、用户资料接口、文件上传、枚举参数转换、**省市区树接口 `GET /app/region/listAsTree`**
3. 用户端 H5：**整体换成官方 H5**（13 个页面），最终目录名就是 `web-app-frontend`，由 `tools/finalize-web-app.ps1` 把 `rentHouseH5` 改名而来
4. **移除 AI 问答功能**：后端的 `controller/ai`、`AiService(+Impl)`、`vo/ai`、`DeepSeekConfig(+Properties)` 共 8 个文件删除，yaml 的 `deepseek.*` 与 `/app/ai/**` 放行、Knife4j 的 AI 分组一并清理
5. 后台管理界面 `web-admin-frontend`：**已按官方 `rentHouseAdmin` 的视觉规范重做**（深色侧边栏 `#001529`、主色 `#1890ff`、顶部导航 + 多标签页 + 页脚、玻璃拟态登录页）。官方 `rentHouseAdmin` 本身也无需改代码即可对接本项目后端，二者可互换
6. 新增 `sql/lease.sql` 数据库初始化脚本

### 修复的原有 bug

| 位置 | 问题 | 影响 |
| --- | --- | --- |
| `web-admin/.../controller/login/LoginController.java` | 登录与 info 接口 `return Result.ok()` 丢掉了 token / VO | 后台**永远登录不进去** |
| `web-admin/.../service/impl/LoginServiceImpl.java` | `DigestUtils.md5()` 返回 `byte[]`，与 String 比较恒为 false | 后台**任何密码都登不上** |
| `web-admin/.../controller/apartment/RoomController.java` | `updateReleaseStatusById` 用了 `eq` 而非 `set` | 房间发布/下架**永远不生效** |
| `web-admin/.../service/impl/RoomInfoServiceImpl.java` | 保存房间图片时 `graphInfo.setId(roomId)` | 图片写错主键，**创建房间报重复主键** |
| `web-admin/.../resources/mapper/ApartmentInfoMapper.xml` | 动态条件缺 `and`；`freeRoomCount` 关联错表 | 公寓列表一加筛选就 **SQL 语法错误** |
| `web-admin/.../resources/mapper/RoomInfoMapper.xml` | `la.id is not null is_check_in` 语法错误、公寓名未映射 | 房间列表 **SQL 报错 / 公寓名空白** |
| `web-admin/.../resources/mapper/ViewAppointmentMapper.xml` | `ai.apartment_id` 列不存在；公寓信息未映射 | 预约列表 **SQL 报错 / 公寓名空白** |
| `web-admin/.../resources/mapper/LeaseAgreementMapper.xml` | `concat('%','#{queryVo.name}','%')` 把参数写进了引号；`paymentType` 的 id 映射到了 `pt_name`；房间号/租期月数未映射 | 按姓名/手机号搜索**永远无结果**，详情里支付方式/租期**错乱或空白** |
| `web-admin/.../resources/mapper/AttrKeyMapper.xml` | `av_attr_key_id` 别名与 select 不一致 | 属性值的父 id 取不到 |
| `web-admin/.../resources/mapper/FeeKeyMapper.xml` | 未查询 `fee_value.unit` | 杂费单位永远空白 |
| `web-admin/.../controller/system/SystemPostController.java` | 分页方法是 `private` | 岗位分页可能**无法被 Spring MVC 映射** |
| `web-admin/.../controller/lease/LeaseAgreementController.java` | `@GetMapping(name = "getById")` 中的 `name` 不是路径属性 | ⚠️ **更正：这一条原先被我误判成 bug。** 官方前端 `rentHouseAdmin` 调的正是 `GET /admin/agreement?id=`（类根路径），所以这种写法是**有意为之**。已改为 `@GetMapping({"", "getById"})`，两种路径都支持，既兼容官方前端也兼容自建前端 |
| `common/.../mybatisplus/MybatisPlusObjectHandler.java` | `updateFill` 里用了 `strictInsertFill` | 更新时 `update_time` 不自动填充 |

另外：`SystemPost` 的「岗位编码」实体字段名是 **`postCode`**（映射到数据库列 `code`），
前端表单/表格用的也是 `postCode`，注意别写成 `code`。

### 已知限制

本次开发环境 **shell 不可用**（`pwsh` 报 `0xC0000142`），无法执行 `mvn` / `npm` / `mysql`，
因此代码是**静态编写 + 人工复核**，没有经过编译和运行验证。
如果启动时遇到编译报错，通常是依赖版本或某个 import 的小问题，按提示修正即可。
