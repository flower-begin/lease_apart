# 归心寓 · 角色与权限（RBAC）设计方案

> 本文回答三个问题：**租房系统还缺哪些功能**、**有哪些角色**、**每个角色能看到哪些页面和功能**。
> 最后给出 RBAC 的落地方案（表结构 / 权限码 / 后端鉴权 / 前端菜单过滤）。

---

## 一、现状盘点

| 已有能力 | 说明 |
| --- | --- |
| 房源 | 公寓、房间、图片、属性、配套、标签、杂费、支付方式、租期 |
| 获客 | 预约看房（C 端提交 → 后台处理状态） |
| 交易 | 租约（签约、录入） |
| 用户 | C 端 `user_info`、后台员工 `system_user`、岗位 `system_post` |
| 字典 | 标签 / 配套 / 属性 / 杂费 / 支付方式 / 租期 |
| 后台账号 | `admin`（管理员）、`operator`、`cleaner`，靠 `system_user.type` 区分 |

**问题**：没有角色概念，只有「管理员 / 普通用户」两档；`type` 一改就全有或全无，做不到「财务只能看账单」「维修工只能看工单」。

---

## 二、租房系统还缺哪些功能

按业务闭环排列，✅ 已实现、🔨 本次补（文档级）、📋 规划中。

### 1. 房源域

| 功能 | 说明 | 状态 |
| --- | --- | --- |
| 公寓 / 房间 CRUD | 已有 | ✅ |
| 房源**审核** | 经纪人提报 → 运营审核 → 上架 | 📋 |
| **上架 / 下架 / 推荐位** | 控制 C 端是否可见 | 📋 |
| **房态看板** | 可租 / 已租 / 维修中 / 待保洁，按房间维度 | 📋 |
| **价格日历** | 不同租期、节假日的差异化定价 | 📋 |
| 房源图片、属性、配套、标签 | 已有 | ✅ |
| 房源**批量导入 / 导出** | Excel 导入几百间房 | 📋 |

### 2. 获客域

| 功能 | 说明 | 状态 |
| --- | --- | --- |
| 预约看房 | 已有 | ✅ |
| **带看记录** | 谁带看、结果（成交 / 再看看 / 流失） | 📋 |
| **客户线索池** | 预约 → 线索 → 成交转化漏斗 | 📋 |
| **收藏 / 浏览历史** | C 端 | ✅（有表，弱化） |
| **房源分享** | 生成带经纪人 id 的分享链接，成交算业绩 | 📋 |

### 3. 签约域

| 功能 | 说明 | 状态 |
| --- | --- | --- |
| 租约录入 | 已有 | ✅ |
| **电子合同签署** | C 端在线签、后台归档 PDF | ✅（C 端有签署页） |
| **续约** | 到期前一键续租 | 📋 |
| **转租 / 换房** | 合同变更 | 📋 |
| **退租结算** | 押金扣除项（水电、维修、违约金） | 📋 |
| **合同模板管理** | 不同城市 / 房源类型用不同模板 | 📋 |

### 4. 费用域

| 功能 | 说明 | 状态 |
| --- | --- | --- |
| **账单生成** | 按租期自动生成租金 / 水电 / 物业账单 | 📋 |
| **在线缴费** | 微信 / 支付宝回调 | 📋 |
| **催缴** | 逾期自动短信 / 站内信 | 📋 |
| **退款** | 押金退还、超收退款 | 📋 |
| **发票** | 开票申请与状态 | 📋 |
| **对账报表** | 月度收入、按门店/经纪人拆分 | 📋 |

### 5. 租后服务域

| 功能 | 说明 | 状态 |
| --- | --- | --- |
| **报修工单** | 租客提单 → 派单 → 维修 → 验收 → 评价 | 📋 |
| **保洁工单** | 退租保洁、周期保洁 | 📋 |
| **投诉建议** | 工单的一个类型 | 📋 |
| **评价管理** | 租客对房源 / 服务的评价，后台审核与回复 | 📋 |
| **公告 / 站内信** | 停水停电、活动通知 | 📋 |

### 6. 运营 & 系统域

| 功能 | 说明 | 状态 |
| --- | --- | --- |
| **数据看板** | 已有首页，可扩：出租率、空置天数、GMV、转化漏斗 | ✅ / 📋 |
| **营销** | 优惠券、首月减免、老带新 | 📋 |
| **RBAC 角色权限** | 本次落地 | 🔨 |
| **操作日志 / 登录日志** | 谁在什么时候改了什么 | 📋 |
| 字典、岗位、文件上传 | 已有 | ✅ |

---

## 三、角色定义

后台侧 6 个角色 + C 端 2 个身份。

| # | 角色 | 编码 | 定位 | 一句话职责 |
| --- | --- | --- | --- | --- |
| 1 | 超级管理员 | `super_admin` | 平台 owner | 全部权限，含账号与权限配置 |
| 2 | 运营专员 | `ops` | 平台运营 | 管房源内容、字典、预约、公告；**不碰账号和财务** |
| 3 | 门店店长 | `manager` | 区域负责人 | 管本店房源、租约、账单、工单，可看本店员工业绩 |
| 4 | 经纪人 / 管家 | `agent` | 一线销售与租后 | 只管**自己名下**的房源、预约、租约、工单 |
| 5 | 财务 | `finance` | 财务岗 | 账单、缴费、退款、对账；租约只读 |
| 6 | 维修 / 保洁 | `maintainer` | 执行岗 | 只有工单，看派给自己的单 |
| 7 | 租客 | `tenant` | C 端用户（`user_info`） | 找房、预约、签约、缴费、报修、评价 |
| 8 | 房东 | `landlord` | 托管房源的业主（可选） | 只看自己房源的出租情况与收益 |

> **房东**是可选角色：如果你是自营公寓（把房子租进来再租出去），就不需要它。代码里预留了 `landlord` 数据范围，但菜单先不挂。

### 数据范围（Scope）

角色不只决定「能看哪些页面」，还决定「能看哪些数据」：

| 范围 | 编码 | 含义 |
| --- | --- | --- |
| 全部 | `ALL` | 不限 |
| 本门店 | `STORE` | 只能看 `store_id` 与自己相同的 |
| 仅本人 | `SELF` | 只能看 `create_by` / `agent_id` 是自己的 |

字段落在 `sys_role.data_scope`。**本期实现到「菜单 + 按钮 + 接口」三级，数据范围作为下一期**（需要给业务表补 `store_id` / `agent_id`，改动面较大）。

---

## 四、权限模型

标准 RBAC：**用户 → 角色 → 权限**，权限挂在菜单上（菜单类型区分目录/菜单/按钮）。

```
system_user ──< sys_user_role >── sys_role ──< sys_role_menu >── sys_menu
                                                                  ├─ type=1 目录（只用于菜单分组）
                                                                  ├─ type=2 菜单（一个页面）
                                                                  └─ type=3 按钮（页面内的操作，perms 落接口）
```

设计要点：

1. **菜单即权限**：一个页面被授权 = 对应 `sys_menu` 行被插入 `sys_role_menu`。前端菜单树、路由守卫、按钮显示都从这份数据推导，不会出现「菜单藏了但接口还能调」。
2. **接口也校验**：`sys_menu.perms` 是权限串，接口上用 `@RequirePermission("apartment:save")` 声明。拦截器从 token 拿 userId → 查角色权限集合 → 比对。前端隐藏只是体验，后端拦截才是安全边界。
3. **超级管理员短路**：`sys_role.code = 'super_admin'` 直接放行，避免新增菜单后忘了给超管授权。
4. **不用 `system_user.type` 做鉴权**：`type` 只保留「是否管理员」的历史语义，鉴权统一走 RBAC，避免两套逻辑打架。

### 权限码规范

`模块:子模块:动作`，全小写，动作用 `list / detail / save / delete / export / assign`。

| 示例 | 含义 |
| --- | --- |
| `apartment:list` | 看公寓列表 |
| `apartment:save` | 新增/编辑公寓 |
| `agreement:list` | 看租约 |
| `system:role:assign` | 给角色分配菜单 |

---

## 五、页面 × 角色矩阵

`✅` 完整读写　`👁` 只读　`—` 不可见

### 后台（web-admin-frontend，5174）

| 页面 | 超级管理员 | 运营 | 店长 | 经纪人 | 财务 | 维修 |
| --- | --- | --- | --- | --- | --- | --- |
| 首页 / 数据看板 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| 公寓管理 | ✅ | ✅ | 👁 | 👁 | — | — |
| 房间管理 | ✅ | ✅ | ✅ | 👁 | — | — |
| 预约看房 | ✅ | ✅ | ✅ | ✅ | — | — |
| 租约管理 | ✅ | 👁 | ✅ | ✅ | 👁 | — |
| 用户管理 | ✅ | ✅ | 👁 | 👁 | 👁 | — |
| 字典管理（标签/配套/属性/杂费/支付/租期） | ✅ | ✅ | — | — | — | — |
| 系统管理 · 员工管理 | ✅ | — | 👁 | — | — | — |
| 系统管理 · 岗位管理 | ✅ | — | — | — | — | — |
| 系统管理 · **角色管理** | ✅ | — | — | — | — | — |

### 租客端（web-app-frontend / web-app-pc）

| 页面 | 租客 | 未登录游客 |
| --- | --- | --- |
| 首页 / 找房 / 搜索 | ✅ | ✅ |
| 房源详情 | ✅ | ✅ |
| 预约看房 | ✅ | ❌（先登录） |
| 我的预约 | ✅ | ❌ |
| 我的租约 / 在线签约 | ✅ | ❌ |
| 我的账单 / 缴费 | ✅ | ❌ |
| 我的报修 | ✅ | ❌ |
| 收藏 / 浏览历史 | ✅ | ❌ |
| 个人中心 / 修改密码 | ✅ | ❌ |
| 评价 | ✅ | ❌ |

---

## 六、接口 × 权限码（本期落地部分）

| 接口 | 方法 | 权限码 |
| --- | --- | --- |
| `/admin/system/role/list` | GET | `system:role:list` |
| `/admin/system/role/saveOrUpdate` | POST | `system:role:save` |
| `/admin/system/role/delete` | DELETE | `system:role:delete` |
| `/admin/system/role/menuIds` | GET | `system:role:list` |
| `/admin/system/role/assignMenu` | PUT | `system:role:assign` |
| `/admin/system/menu/tree` | GET | `system:role:list` |
| `/admin/system/user/roles` | GET | `system:user:list` |
| `/admin/system/user/assignRole` | PUT | `system:user:assign` |

房源、字典等已有接口的权限码已经在 `sys_menu` 种子里给了，后续在 Controller 上逐个加 `@RequirePermission` 即可（见第七节示例）。

---

## 七、落地实现

### 7.1 表结构（`sql/upgrade-02-rbac.sql`）

```sql
sys_role       (id, name, code, description, data_scope, sort, status, ...)
sys_menu       (id, parent_id, name, type, path, component, icon, perms, sort, visible, status, ...)
sys_role_menu  (id, role_id, menu_id)   -- 唯一键 (role_id, menu_id)
sys_user_role  (id, user_id, role_id)   -- 唯一键 (user_id, role_id)
```

种子数据：

- 6 个角色（`super_admin / ops / manager / agent / finance / maintainer`）
- 菜单树：首页 → 房源（公寓/房间）→ 租务（预约/租约）→ 客户（用户）→ 字典（6 项）→ 系统（员工/岗位/角色）
- 每个菜单下的按钮权限（`*:save`、`*:delete` 等）
- 角色授权：超管全量；运营 = 除系统管理外；店长 = 房源只读 + 租约/预约/房间可写；经纪人 = 本人相关只读为主；财务 = 租约/用户只读；维修 = 只有首页
- 员工授权：`admin → super_admin`、`operator → ops`、`cleaner → maintainer`

### 7.2 后端

```
controller/system/SysRoleController      角色 CRUD + 分配菜单
controller/system/SysMenuController      菜单树（给前端角色授权弹窗用）
controller/system/SysUserRoleController  给员工分配角色
service/PermissionService                查某用户的 权限集合 / 菜单树
custom/annotation/RequirePermission      接口权限注解
custom/interceptors/PermissionInterceptor 鉴权拦截器（LoginInterceptor 之后）
```

`/admin/info` 的返回体扩展为：

```json
{
  "id": 1,
  "name": "管理员",
  "avatarUrl": null,
  "roles": ["super_admin"],
  "perms": ["apartment:list", "apartment:save", "..."],
  "menus": [{ "id": 10, "name": "房源管理", "path": "/", "children": [...] }]
}
```

给任意接口加权限只需要一行：

```java
@RequirePermission("apartment:save")
@PostMapping("saveOrUpdate")
public Result saveOrUpdate(@RequestBody ApartmentSubmitVo vo) { ... }
```

### 7.3 前端

1. 登录后调 `/admin/info`，把 `roles / perms / menus` 存进 Pinia + localStorage。
2. 侧边栏菜单**由 `menus` 渲染**（不再从静态路由推导），没授权的菜单压根不出现。
3. 路由守卫：`meta.perm` 不在 `perms` 里就跳 403/404，防止手敲 URL。
4. `v-perm="'apartment:save'"` 指令控制按钮显隐。
5. 新增页面：**系统管理 → 角色管理**（角色列表 + 菜单树勾选授权 + 菜单树预览）。

---

## 八、验收清单

- [ ] 用 `admin` 登录：能看到全部 8 个菜单组，角色管理可增删改、可分配菜单
- [ ] 用 `operator` 登录（账号 `operator`）：侧边栏没有「系统管理」
- [ ] 用 `cleaner` 登录：侧边栏只有「首页」
- [ ] 手敲 `#/system/role`：被守卫拦下（无权限）
- [ ] 用 `operator` 的 token 直接请求 `/admin/system/role/list`：返回 `309 无访问权限`
- [ ] 在角色管理里给 `ops` 勾上「系统管理 → 角色管理」→ 重新登录后 operator 能看到该菜单

---

## 九、下一步建议

1. **数据范围**：给 `apartment_info` / `lease_agreement` / `appointment` 补 `store_id`、`agent_id`，`PermissionService` 返回 `data_scope`，Mapper 统一追加 `AND` 条件。
2. **操作日志**：AOP 切 `@RequirePermission` 的写接口，记 `who / what / before / after`。
3. **账单与工单**：这两个是租房系统真正的业务重心，建议优先于营销功能实现。
