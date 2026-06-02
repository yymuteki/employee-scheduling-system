# 员工排班系统 — 设计文档

**日期**: 2026-06-02
**状态**: 已确认

---

## 1. 项目概述

一个基于 LLM 的智能排班系统。员工通过自然语言输入下月排班需求，大模型自动解析并结合预设规则生成排班表。分员工端和管理员端两个端口。

### 核心功能

- **员工端**：自然语言输入下月排班需求（请假日期、班次偏好），查看已发布的排班表
- **管理员端**：查看所有员工需求，一键触发 LLM 生成排班，手动调整排班表，发布排班
- **可视化**：员工端日历视图，管理员端表格视图 + 统计

---

## 2. 技术架构

### 架构选型：Spring Boot 极简一体式

```
浏览器（员工端 / 管理员端）
       │
       ▼
Spring Boot Application (port 8080)
  ├── Auth Layer        —— 登录认证 + 角色校验
  ├── Schedule API      —— 排班 CRUD
  ├── LLM Service       —— DeepSeek API 调用
  ├── Stats Service     —— 统计查询
  ├── H2 Database       —— 内嵌文件数据库
  └── React SPA         —— 打包在 /static 目录
```

### 组件选型

| 组件 | 选型 | 说明 |
|------|------|------|
| 后端框架 | Spring Boot 3 + JDK 17 | Java，按用户要求 |
| 数据库 | H2 (文件模式) | 零安装，数据存 `data/schedule.mv.db`，10-20 人规模足够 |
| 前端 | React (Vite) + FullCalendar | 日历可视化核心，打包后放 jar 内 |
| LLM | DeepSeek API (Chat Completions) | 用户已有 API Key |
| 认证 | Spring Security + Session | 登录后区分角色 |
| 部署 | `java -jar app.jar` | 单文件启动，零依赖 |

---

## 3. 数据模型

### 3.1 user（用户表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT (PK) | 自增主键 |
| username | VARCHAR(50) UNIQUE | 登录账号 |
| password | VARCHAR(255) | BCrypt 加密 |
| name | VARCHAR(50) | 显示姓名 |
| role | ENUM('EMPLOYEE', 'ADMIN') | 角色 |

### 3.2 shift_requirement（排班需求表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT (PK) | 自增主键 |
| user_id | BIGINT (FK → user) | 员工 |
| year_month | VARCHAR(7) | 月份，如 "2026-07" |
| natural_input | TEXT | 员工原始自然语言输入 |
| parsed_unavailable | JSON | LLM 解析出的不可上班日期，如 `["2026-07-03", "2026-07-15"]` |
| parsed_preferences | JSON | LLM 解析出的偏好，如 `{"prefer_morning": true, "prefer_dates": ["2026-07-10"]}` |
| created_at | TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | 最后更新时间 |

每个员工每月一条记录，可多次编辑覆盖。

### 3.3 schedule（排班表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT (PK) | 自增主键 |
| user_id | BIGINT (FK → user) | 员工 |
| date | DATE | 具体日期 |
| shift | ENUM('MORNING', 'EVENING', 'OFF') | 班次 |
| year_month | VARCHAR(7) | 月份 |
| status | ENUM('DRAFT', 'PUBLISHED') | 状态 |

每天每个员工一条记录。对于 MORNING/EVENING/OFF，标准节假日所有员工为 OFF。

---

## 4. 页面与路由

| 路径 | 页面 | 员工 | 管理员 | 说明 |
|------|------|:---:|:---:|------|
| `/login` | 登录页 | ✓ | ✓ | 输入账号密码 |
| `/requirements` | 需求输入 | ✓ | | 对话框 + 已保存需求确认 |
| `/schedule` | 排班查看 | ✓ | ✓ | 员工看已发布，管理员看全部 |
| `/admin/requirements` | 员工需求汇总 | | ✓ | 列表展示所有员工需求 |
| `/admin/schedule` | 排班管理 | | ✓ | 表格视图 + 手动调整 + 发布 |
| `/admin/generate` | 生成排班 | | ✓ | 触发生成 + 查看生成结果 |

登录后根据 role 自动跳转对应首页：管理员 → `/admin/schedule`，员工 → `/schedule`。

---

## 5. API 设计

### 5.1 认证

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/login` | 登录，返回用户信息（含 role） |
| POST | `/api/auth/logout` | 登出 |

### 5.2 员工端

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/requirements/my?month=2026-07` | 获取我的需求 |
| POST | `/api/requirements` | 提交/更新需求，触发 LLM 解析 |
| GET | `/api/schedule?month=2026-07` | 查看已发布排班 |

### 5.3 管理员端

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/requirements?month=2026-07` | 查看所有员工需求 |
| POST | `/api/admin/schedule/generate?month=2026-07` | 触发 LLM 生成排班 |
| PUT | `/api/admin/schedule/{id}` | 手动修改单条排班记录 |
| POST | `/api/admin/schedule/publish?month=2026-07` | 发布排班（DRAFT → PUBLISHED） |
| GET | `/api/admin/schedule/stats?month=2026-07` | 排班统计（每人上班天数等） |

### 5.4 认证方式

Spring Security + Session，登录成功后 cookie 维持会话。所有 `/api/admin/**` 路径需要 ADMIN 角色。

---

## 6. LLM 交互设计

### 6.1 调用1：解析员工自然语言输入

**触发时机**：员工提交需求文本

**Prompt 模板**：
```
你是一个排班需求解析器。当前月份是 {year_month}。
请解析以下员工关于排班需求的自然语言输入，提取出结构化信息。

输入："{user_input}"

请只返回 JSON，格式如下：
{
  "unavailable_dates": ["YYYY-MM-DD", ...],  // 不能上班的日期
  "prefer_morning": true/false,               // 是否偏好早班
  "prefer_dates": ["YYYY-MM-DD", ...],        // 偏好上班的日期（可选）
  "notes": "简短摘要"                          // 对输入的理解总结
}
```

**处理**：解析结果在保存前展示给员工确认（前端弹窗显示"我们理解您的需求是：2月3日不能上班，偏好早班。确认吗？"）。

**容错**：LLM 返回非 JSON 时，提示员工重新描述。

### 6.2 调用2：生成排班表

**触发时机**：管理员点击「生成排班」

**输入上下文**：
1. 所有员工的 `shift_requirement` 记录（结构化后的需求）
2. 当月节假日列表（法定节假日）
3. 当月日历信息（天数、周末分布）

**Prompt 模板**：
```
你是一个排班生成器。请根据以下信息生成 {year_month} 的排班表。

## 班次类型
- MORNING: 早班 (08:00-16:00)
- EVENING: 晚班 (16:00-00:00)
- OFF: 休息

## 硬性规则（必须遵守，违反任何一条都是无效输出）
1. 同一员工不能在同一天安排两个班次
2. 同一员工不能连续上班超过 4 天（第 5 天必须休息）
3. 每天每个班次至少安排 1 人
4. 法定节假日所有人安排 OFF
5. 每人每月上班天数不超过 20 天

## 员工偏好（尽量满足，无法全部满足时优先保证公平分配）
{员工偏好列表}

## 法定节假日
{节假日列表}

请返回 JSON：
{
  "schedule": [
    {"user_id": 1, "date": "2026-07-01", "shift": "MORNING"},
    ...
  ]
}
```

**后处理**：
1. 后端验证返回结果是否满足 5 条硬约束
2. 不满足 → 重试一次（共 2 次机会）
3. 仍失败 → 返回管理员错误信息，标明具体违反的规则
4. 验证通过 → 写入 schedule 表，状态 DRAFT
5. 管理员在界面查看、手动调整，满意后点「发布」

---

## 7. 业务规则

### 7.1 硬性规则（系统强制）

| # | 规则 | 验证方式 |
|---|------|---------|
| 1 | 同一员工不能在同一天安排两个班次 | 数据唯一约束 + 后端校验 |
| 2 | 同一员工不能连续上班 5 天及以上 | 后端遍历校验 |
| 3 | 每天每个班次至少安排 1 人 | 后端统计校验 |
| 4 | 法定节假日所有人 OFF | 生成时硬编码 |
| 5 | 每人每月上班天数 ≤ 20 | 后端统计校验 |

### 7.2 软约束（LLM 尽量满足）

- 员工标注的不可上班日期，尽量不安排
- 员工偏好的班次类型，优先满足
- 同级别员工之间上班天数尽量平均
- 员工偏好的上班日期，尽量满足

---

## 8. 排班生成流程

```
管理员点击「生成排班」
        │
        ▼
 后端拉取本月所有员工需求 + 规则 + 节假日
        │
        ▼
 组装 Prompt → 调用 DeepSeek API
        │
        ▼
 接收 JSON → 后端验证 5 条硬约束
        │
    ┌───┴───┐
    │ 通过？  │
    └───┬───┘
   否   │   是
    ▼        ▼
 重试1次  写入 schedule (DRAFT)
    │        │
    ▼        ▼
 仍失败    前端展示排班表
    │        │
    ▼        ▼
 提示管理员  管理员检查 + 手动调整
              │
              ▼
         点击「发布」
              │
              ▼
         DRAFT → PUBLISHED
              │
              ▼
         员工端可见
```

---

## 9. 错误处理

| 场景 | 处理方式 |
|------|---------|
| LLM 解析输入失败（返回非 JSON） | 提示员工重新描述 |
| LLM 生成排班不满足硬约束 | 自动重试 1 次；仍失败则提示管理员具体规则冲突 |
| DeepSeek API 不可用 | 返回 "AI 服务暂时不可用，请稍后重试" |
| 员工提交空需求 | 视为无特殊要求，LLM 只按规则排 |
| 生成中重复点击 | 前端按钮禁用 + 后端幂等检查 |
| 该月无任何员工需求 | 仅按规则生成排班 |

---

## 10. 页面级设计

### 10.1 登录页
- 居中卡片，账号 + 密码输入框，登录按钮
- 无注册入口（账号由管理员预置或系统初始化）

### 10.2 员工端 — 需求输入页
- 顶部：当前月份选择器
- 中部：对话式输入框，"描述你的下月排班需求..."
- 下方：已提交需求的确认卡片（如有），显示解析结果
- 状态提示："需求已保存" / "尚未提交"

### 10.3 员工端 — 排班查看页
- FullCalendar 月历视图
- 每天格子显示员工姓名 + 班次标签（早班绿色 / 晚班蓝色 / 休息灰色）
- 仅显示已发布排班；未发布时显示"排班尚未发布"

### 10.4 管理员端 — 员工需求汇总页
- 表格：员工姓名 | 不可上班日期 | 偏好班次 | 备注 | 提交时间
- 支持按月份筛选

### 10.5 管理员端 — 排班管理页
- 表格视图：行=员工，列=日期（1号到月底）
- 每格下拉选择：早班 / 晚班 / 休息，用颜色区分
- 顶部工具栏：「生成排班」按钮 + 「发布」按钮 + 月份选择
- 统计面板：每人当月上班天数、早/晚班次数
- 发布后表格锁定，可取消发布重新编辑

### 10.6 管理员端 — 生成排班页
- 触发按钮，生成进度提示（等待 LLM 返回）
- 生成完成后展示排班概览（统计摘要）
- 提示跳转到排班管理页查看详情

---

## 11. 测试策略

由于开发时间只有一天，聚焦以下核心验收点：

1. **启动验证**：`java -jar app.jar` 一条命令启动，浏览器可访问
2. **主链路测试**（手动走通）：
   - 员工登录 → 输入自然语言需求 → 确认解析结果 → 保存
   - 管理员登录 → 查看需求 → 生成排班 → 手动调整 → 发布
   - 员工查看已发布排班 → 日历正确显示
3. **规则验证**：3 员工 × 7 天小数据集，人工验证 5 条硬约束全部满足
4. **边界测试**：全员请假时提示冲突、无需求时正常生成
5. **错误测试**：DeepSeek key 无效时有友好提示、非法输入有错误反馈

---

## 12. 初始化数据

系统启动时自动创建：

- **管理员账号**：admin / admin123
- **员工账号**：通过管理员手动添加或配置文件预置
- **节假日数据**：2026 年中国法定节假日（硬编码或配置文件）
- **班次定义**：MORNING (08:00-16:00), EVENING (16:00-00:00)

---

## 13. 项目结构

```
schedule-app/
├── pom.xml                          # Maven 配置
├── src/main/java/com/schedule/
│   ├── ScheduleApplication.java     # 启动类
│   ├── config/
│   │   ├── SecurityConfig.java      # Spring Security 配置
│   │   └── WebConfig.java           # 静态资源 + SPA 路由
│   ├── entity/
│   │   ├── User.java
│   │   ├── ShiftRequirement.java
│   │   └── Schedule.java
│   ├── repository/                  # JPA Repository
│   │   ├── UserRepository.java
│   │   ├── ShiftRequirementRepository.java
│   │   └── ScheduleRepository.java
│   ├── service/
│   │   ├── AuthService.java
│   │   ├── RequirementService.java
│   │   ├── ScheduleService.java
│   │   └── LLMService.java          # DeepSeek API 调用
│   ├── controller/
│   │   ├── AuthController.java
│   │   ├── RequirementController.java
│   │   ├── ScheduleController.java
│   │   └── AdminController.java
│   └── dto/                         # 请求/响应 DTO
├── src/main/resources/
│   ├── application.yml              # 配置（含 DeepSeek key）
│   └── data.sql                     # 初始化数据
├── src/main/frontend/               # React 前端源码
│   ├── src/
│   │   ├── pages/
│   │   │   ├── LoginPage.tsx
│   │   │   ├── EmployeeRequirements.tsx
│   │   │   ├── EmployeeSchedule.tsx
│   │   │   ├── AdminRequirements.tsx
│   │   │   ├── AdminSchedule.tsx
│   │   │   └── AdminGenerate.tsx
│   │   ├── components/
│   │   │   ├── ScheduleCalendar.tsx  # FullCalendar 封装
│   │   │   ├── ScheduleTable.tsx     # 管理员表格视图
│   │   │   ├── RequirementDialog.tsx # 对话输入框
│   │   │   └── Layout.tsx           # 布局 + 导航
│   │   ├── api/                     # Axios 封装
│   │   └── App.tsx
│   └── package.json
└── README.md
```
