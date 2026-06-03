# CLAUDE.md

本项目 100% 使用 Claude Code 辅助开发。所有代码由 AI 生成初稿后人工审查/测试迭代修正。

## 项目概述
员工智能排班系统 — Spring Boot 3 + React 18 + DeepSeek LLM。
AI 辅助开发 + AI 驱动业务（LLM 解析员工需求、生成排班）。

## 常用命令
```bash
# 启动（Windows）
D:\table\schedule-app\start.bat

# 编译打包
cd schedule-app && mvn package -DskipTests

# 前端构建
cd schedule-app/src/main/frontend && npm run build

# 清理数据库重建
rm -f schedule-app/data/schedule.mv.db
```

## 架构
```
浏览器 → Spring Boot (:8080) → H2 文件数据库
         ├── SecurityConfig (Session + CSRF)
         ├── LLMService → DeepSeek API
         └── React SPA (Vite, 打包到 /static)
```

## AI 开发工作流
1. 设计文档写清楚规则 → `docs/superpowers/specs/`
2. 拆成结构化 Prompt → `docs/prompts/`
3. Claude Code 生成代码 → 人工审查 → 测试 → 提交

## 关键文件
| 层 | 文件 |
|---|---|
| 安全 | `config/SecurityConfig.java` |
| LLM | `service/LLMService.java` (Prompt Engineering 核心) |
| 排班 | `service/ScheduleService.java` (5条硬约束 + 验证) |
| 审计 | `service/AuditLogService.java` (独立事务) |
| 前端 | `frontend/src/pages/AdminGenerate.tsx` (按钮灰显逻辑) |

## 注意
- Java 17 (E:\JAVA)，不是系统默认 JDK 8
- H2 Console: http://localhost:8080/h2-console (sa/空密码)
- DeepSeek API Key 由 start.bat 设置环境变量传入
