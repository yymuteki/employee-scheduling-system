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

本项目使用 AI 驱动的开发流程。核心业务逻辑通过结构化的 Prompt 交给 AI 辅助实现。

### 开发流程

```
设计文档 (PRD/SPEC)
       │
       ▼
拆分 Prompt ──────→ AI 生成代码 ──────→ 人工审查 + 测试
       │                                      │
       ▼                                      ▼
docs/prompts/                          验证通过 → 提交
```

### Prompt 文件

所有核心 Prompt 存放在 `../docs/prompts/` 目录下：

| 文件 | 作用 |
|------|------|
| `01_scheduling_logic.md` | 排班服务层逻辑：硬约束验证、LLM 结果解析、发布/取消发布 |
| `02_deepseek_integration.md` | DeepSeek API 集成：HTTP 调用封装、Prompt Engineering、JSON 解析容错 |

### 使用方式

1. **编写设计文档**：先用自然语言描述需求和架构（参考 `docs/superpowers/specs/` 下的 SPEC 文档）
2. **拆分 Prompt**：从设计文档中提取核心逻辑，拆分为结构化的 Prompt 文件（参考 `docs/prompts/`）
3. **驱动 AI 编码**：将 Prompt 提供给 AI 工具（Claude Code / Cursor 等），生成初始代码
4. **人工审查**：审查生成的代码是否正确、是否符合项目规范
5. **运行测试**：启动应用，走通主链路，验证约束条件（参考本 README 中「硬性规则」）
6. **迭代修正**：测试中发现的问题通过追加指令修复，重要修复记录在项目 memory 中

### 实践原则

- **Prompt 先于代码**：复杂逻辑先写好 Prompt，再让 AI 生成实现
- **Prompt 即文档**：Prompt 本身也是技术文档，新人可通过 Prompt 快速理解模块设计
- **保留核心 Prompt**：关键的 Prompt 提交到 `docs/prompts/`，便于追溯和复盘
- **约束优于描述**：Prompt 中明确列出硬约束（如"同一员工连续工作不超过 4 天"），而非笼统描述（"合理安排排班"）
