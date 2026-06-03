# 03 — 前端页面与 API 对接 Prompt

<!-- 用途：指导 AI 生成 React + TypeScript 前端页面，对接 Spring Boot 后端 API，包含双端路由、CSRF 防护、状态管理 -->

## 任务

为员工排班系统构建 React 18 + TypeScript 前端，包含员工端和管理员端共 6 个页面，对接 Spring Boot 后端 REST API。

## 技术约束

- React 18 + TypeScript + Vite
- 不使用第三方 UI 库（纯 CSS-in-JS，所有样式内联）
- 路由：React Router v6，根据用户 role 拆分路由
- HTTP 请求：Axios 封装，withCredentials 开启（Session Cookie）
- 打包到 `src/main/resources/static/`，Spring Boot 直接 serve

## 页面清单

### 员工端（2 页）

| 路径 | 页面 | 核心功能 |
|---|---|---|
| `/requirements` | EmployeeRequirements | 自然语言输入需求 → POST /api/requirements；查看已保存需求 → GET /api/requirements |
| `/schedule` | EmployeeSchedule (MySchedule) | FullCalendar 月历视图，显示已发布排班 → GET /api/schedule |

### 管理员端（3 页）

| 路径 | 页面 | 核心功能 |
|---|---|---|
| `/admin/requirements` | AdminRequirements | 表格查看所有员工需求（不可上班日期 + 偏好 + 解析备注）→ GET /api/admin/requirements |
| `/admin/schedule` | AdminSchedule | 排班表格（行=员工，列=日期），手动调整班次 → PUT /api/admin/schedule/{id}；发布/取消发布 |
| `/admin/generate` | AdminGenerate | 一键 LLM 生成排班 → POST /api/admin/generate；已生成月份按钮灰显 |

### 公共

| 路径 | 页面 |
|---|---|
| `/` | LoginPage → POST /api/auth/login |

## CSRF 防护

- 应用启动时调用 `GET /api/csrf` 获取 token
- 每次 POST/PUT 请求带 `X-XSRF-TOKEN` header
- Cookie 自动携带（withCredentials: true）

## 状态管理

- App.tsx 管理全局 `user` 状态（登录后设置，登出后清空）
- 根据 `user.role` 渲染不同路由组：
  - `ROLE_ADMIN` → `/admin/*` 路由
  - `ROLE_EMPLOYEE` → `/requirements` + `/schedule`
- 未登录 → 所有路由重定向到 `/`

## 关键交互细节

### AdminGenerate（生成排班页）
- 页面加载时调用 `GET /api/schedule/all?yearMonth=xxx` 检查是否已有排班
- `data.length > 0` → 按钮变灰显示「x月已生成排班」，不可点击
- `data.length === 0` → 显示蓝色「开始生成排班」按钮
- 切换月份时重新检查
- 生成成功后自动设 `alreadyGenerated = true`

### AdminRequirements（需求汇总页）
- 表格列：员工姓名 | 不可上班日期 | 偏好班次 | 解析备注 | 原始输入
- `parsedUnavailable` 和 `parsedPreferences` 是 JSON 字符串，前端 `JSON.parse` 后展示
- `parsedNotes` 非空时红色显示（表示 LLM 解析异常）
- 统计卡片：已提交人数 / 总人数

### AdminSchedule（排班管理页）
- 矩阵表格：行为员工姓名，列为日期（1号到月底）
- 每格显示 MORNING / EVENING / OFF，用颜色区分
- 点击格子弹出下拉选择框，调用 `PUT /api/admin/schedule/{id}` 修改
- 发布后表格锁定，取消发布后可重新编辑

### EmployeeRequirements（需求输入页）
- 文本输入框 + 月份选择器
- 提交后展示已保存的解析结果（确认卡片）
- 支持覆盖提交（同一员工同一月只保留一条记录）

## 导航栏

- 深蓝底白字（`#1e3a5f`）
- 左侧：系统名称 + 页面 Tab（当前页高亮蓝底）
- 右侧：`{user.name}` + 退出按钮
- 使用 `<a href="...">` 而非 `<Link>`（避免 SPA 路由与 Spring Boot 静态资源冲突）

## 边界处理

- 排班未发布 → 员工端显示「排班尚未发布」
- 需求未提交 → 显示「尚未提交」
- API 异常 → catch 后设置 error state，不弹窗
- 生成排班 loading → 显示进度动画 + 预计时间提示
