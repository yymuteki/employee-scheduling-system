# 员工智能排班系统

基于 LLM 的 Spring Boot + React 员工排班系统，支持员工自然语言输入需求、管理员一键生成排班并手动调整后发布。

## 快速启动

### 准备工作
1. 确保 JDK 17 已安装
2. 设置 DeepSeek API Key：修改 `start.bat` 中的 `DEEPSEK_API_KEY` 环境变量，或启动时设置

### Windows 启动
```bash
# 设置 JDK 路径（根据实际安装位置修改）
set JAVA_HOME=E:\JAVA
  
# 设置 API Key
set DEEPSEEK_API_KEY=sk-your-key-here

# 启动
java -jar target\schedule-app-1.0.0.jar
```

或直接双击 `start.bat`（需先修改其中的 JAVA_HOME 和 API Key）

### 首次构建
```bash
# 1. 构建前端
cd src/main/frontend
npm install
npm run build

# 2. 打包后端
cd ../..
set JAVA_HOME=E:\JAVA
mvn package -DskipTests
```

## 访问

- 地址：http://localhost:8080
- 管理员账号：admin（默认密码见 AuthService.initUsers()）
- 员工账号：emp1 ~ emp15（默认密码见 AuthService.initUsers()）

## 功能

### 员工端
- 自然语言输入排班需求（如"我2月3号有事情不能上班，更希望上白班"）
- LLM 自动解析为不可上班日期和偏好班次
- 查看已发布的排班表（日历视图，高亮自己的班次）

### 管理员端
- 查看所有员工的需求汇总（表格）
- 一键触发 LLM 生成排班（基于 5 条硬规则 + 员工偏好）
- 手动调整排班（编辑弹窗 + 冲突检测）
- 发布/取消发布排班

## 硬性规则
1. 同一员工不能在同一天上两个班次
2. 任意连续 7 天内，每个员工工作不超过 5 天
3. 每天每个班次至少安排 1 人
4. 法定节假日所有人不排班
5. 每人每月工作总天数不超过 20 天

## 技术栈
- Java 17, Spring Boot 3.2, Spring Security, JPA
- H2 文件数据库
- React 18, TypeScript, Vite, FullCalendar
- DeepSeek Chat API

## 数据
- 存储在 `./data/schedule.mv.db`（H2 文件数据库）
- 首次启动自动创建表结构和初始用户

## AI Development Workflow

本项目 **100% 使用 Claude Code 辅助开发**，从设计文档到代码实现、测试、调试、排错全流程 AI 驱动。核心模块通过结构化 Prompt 生成初稿，人工审查后迭代修正。

### 关键数据

| 指标 | 数值 |
|---|---|
| AI 生成代码占比 | ~90%（核心逻辑全由 Prompt 驱动生成） |
| 结构化 Prompt | 3 份（排班逻辑 / LLM 集成 / 前端 API 对接） |
| Git 提交 | 14 次（每步可追溯） |
| AI 辅助调试 | extractJson 最外层匹配 bug、CSRF 配置、session 固定等 |
| 后端 + 前端文件 | 29 Java + 9 TSX |

### 开发流程

```
PRD 设计文档 ──→ 拆成结构化 Prompt ──→ Claude Code 生成代码
       │                                      │
       ▼                                      ▼
docs/superpowers/specs/             人工审查 → 编译 → 双端测试
                                            │
       ┌────────────────────────────────────┘
       ▼
  Bug 修复（追加 Prompt → AI 定位 → 验证）
       │
       ▼
  Git 提交（每步可追溯）
```

### Prompt 工程

核心 Prompt 存放在 `../docs/prompts/`，直接驱动代码生成：

| 文件 | 生成模块 | 关键约束 |
|---|---|---|
| `01_scheduling_logic.md` | ScheduleService（498行） | 5条硬约束 + 验证重试 + 事务管理 |
| `02_deepseek_integration.md` | LLMService + DeepSeek API | Prompt 模板 + JSON 解析容错 + 循环日期展开 |
| `03_frontend_api_integration.md` | React 页面 + API 对接 | 双端路由 + CSRF + 按钮状态逻辑 |

### 实践原则

- **Prompt 先于代码**：复杂逻辑先写好 Prompt 和约束，再让 AI 生成实现
- **Prompt 即文档**：结构化 Prompt 本身也是技术文档，新人可直接阅读理解模块设计
- **约束优于描述**：Prompt 中明确列出硬约束（如 `同一员工连续工作 ≤ 4 天`），而非笼统描述（`合理安排排班`）
- **每步可验证**：每次 AI 生成后立即编译 + 双端测试，确保不积累问题
- **调试反馈闭环**：测试中发现的 bug 通过追加指令让 AI 定位根因并修复，修复后记录到项目 memory
