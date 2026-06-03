# 01 — 排班服务层核心逻辑 Prompt

<!-- 用途：指导 AI 生成符合 Spring Boot 规范的排班服务层代码，包含硬约束验证、LLM 结果解析、发布/取消发布等全部核心逻辑 -->

## 任务

在 `com.schedule.service.ScheduleService` 中实现排班系统的全部核心业务逻辑。

## 数据模型

### Schedule 实体 (`com.schedule.entity.Schedule`)
- `id` (Long, PK)
- `user` (ManyToOne → User)
- `date` (LocalDate)
- `shift` (Enum: MORNING, EVENING, OFF)
- `yearMonth` (String, 格式 "2026-06")
- `status` (Enum: DRAFT, PUBLISHED)

### 数据库约束
- `(user_id, date, year_month)` 联合唯一约束，防止同一员工同一天出现两条记录

## 5 条硬约束（必须全部满足，违反任一条的排班无效）

1. **一人一天一班次**：同一员工不能在同一天上两个班次（由数据库 unique constraint + 代码双重保证）
2. **连续工作上限**：任意连续 7 天内，每个员工工作不超过 5 天；同时检查连续工作不超过 4 天
3. **每班次最少人数**：每个工作日（周一至周五，非法定节假日）的早班和晚班至少各安排 1 人
4. **法定节假日全员休息**：法定节假日所有员工安排 OFF
5. **月度工作上限**：每人每月工作总天数（MORNING + EVENING）不超过 20 天

## 需要实现的方法

### 1. `generate(String yearMonth)` — 核心排班生成流程
- 入参：月份字符串，如 "2026-06"
- 流程：
  a. 删除该月所有现有排班记录（DRAFT + PUBLISHED 都删除，用 `@Transactional`）
  b. 查询所有 EMPLOYEE 角色的员工
  c. 查询该月所有员工需求（ShiftRequirement）
  d. 获取该月法定节假日列表
  e. 组装员工数据（姓名、不可用日期、偏好）传给 LLMService
  f. 调用 `llmService.generateSchedule(reqData, yearMonth, employeeNames, holidays)` 获取 LLM 生成的排班
  g. 解析 LLM 返回的 JSON → 转换为 Schedule 实体列表
  h. 运行 `validateHardConstraints()` 验证
  i. 验证失败 → 重试（最多2次），传递上一次失败的违规信息给 LLM
  j. 验证通过 → 批量保存到数据库，返回排班列表
  k. 2次重试后仍失败 → 抛出 RuntimeException，包含具体违规信息

### 2. `parseGeneratedSchedule()` — 解析 LLM 返回
- LLM 返回格式：`[{"date": "2026-06-01", "morning": "张三", "evening": "李四"}, ...]`
- 处理逻辑：
  - 法定节假日：当天所有员工设为 OFF（跳过 LLM 的排班结果）
  - 周末（周六/周日）：同样全员 OFF
  - 工作日：morning 员工 → MORNING；evening 员工 → EVENING；其余员工 → OFF
- 注意：需要处理 LLM 返回的姓名与实际员工姓名不匹配的情况

### 3. `validateHardConstraints()` — 硬约束验证
- 返回 `List<String>` 违规描述列表
- 逐条检查 5 条硬约束：
  - Rule 2：对每个员工按日期排序后遍历，连续工作天数 > 4 则违规
  - Rule 3：遍历工作日，统计每天 MORNING/EVENING 人数，< 1 则违规
  - Rule 4：已在 parse 阶段强制处理（节假日全员 OFF）
  - Rule 5：统计每人工作时间，> 20 天则违规
- 规则1由数据库约束保证，此方法不再重复检查

### 4. `publish(String yearMonth)` / `unpublish(String yearMonth)`
- 批量更新：`UPDATE Schedule SET status = PUBLISHED/DRAFT WHERE yearMonth = ?`
- 用 `@Modifying @Query` 在 Repository 层实现，避免逐条更新

### 5. `updateShift(Long scheduleId, String shiftStr)` — 手动调整单条排班
- 管理员修改某个员工的班次
- 需要检查冲突：如果新班次不是 OFF，需要确保该员工当天没有其他班次

### 6. `assignShift(String yearMonth, LocalDate date, Shift shiftType, String userName)` — 手动分配
- 管理员为某个日期+班次指定员工
- 需要处理：
  a. 清除该班次原有员工（设为 OFF）
  b. 如果该员工当天已有其他班次，清除旧班次
  c. 如果该日期还没有任何排班记录，先为所有员工创建 OFF 记录

### 7. `getStats(String yearMonth)` — 排班统计
- 返回：早班总数、晚班总数、人均工作天数、涉及员工数

## 技术要点

- 使用 `@Transactional` 确保数据一致性
- LLM 返回的 JSON 解析失败时给出明确错误信息
- 法定节假日使用 `HolidayUtil.isHoliday(date)` 工具方法判断
- 所有数据库操作使用 Repository 层，不直接写 SQL
- 验证失败的重试要将错误信息传递给 LLM（附加到 prompt 中），引导其修正

## 边界情况处理

- 该月无任何员工需求 → 仅按硬约束随机分配排班
- LLM 返回的姓名不在系统中 → 跳过该条，记录警告
- 员工数为奇数 → 公平轮转，保证人均工作天数相差不超过 1 天
