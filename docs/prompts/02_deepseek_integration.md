# 02 — DeepSeek API 集成 Prompt

<!-- 用途：指导 AI 生成符合 Spring Boot 规范的 LLM 服务层代码，封装 DeepSeek Chat Completions API 调用、Prompt Engineering、JSON 解析和容错处理 -->

## 任务

在 `com.schedule.service.LLMService` 中封装 DeepSeek Chat Completions API，实现两个核心 LLM 调用场景：解析员工需求 + 生成排班表。

## 配置

从 `application.yml` 读取：
```yaml
deepseek:
  api:
    key: ${DEEPSEEK_API_KEY:your-api-key-here}
    url: https://api.deepseek.com/chat/completions
```

注意：API Key 通过环境变量注入，不要硬编码。

## HTTP 调用规范

### 请求
- URL: `https://api.deepseek.com/chat/completions`
- Method: POST
- Headers:
  - `Content-Type: application/json`
  - `Authorization: Bearer {apiKey}`
- Timeout: connect 30s, request 60s
- 使用 Java 11+ `java.net.http.HttpClient`

### 请求 Body 结构
```json
{
  "model": "deepseek-chat",
  "messages": [
    {"role": "user", "content": "{prompt}"}
  ],
  "temperature": 0.3,
  "max_tokens": 4096
}
```

### 响应解析
```json
{
  "choices": [
    {
      "message": {
        "content": "{LLM 返回的文本}"
      }
    }
  ]
}
```
- 解析路径：`response.choices[0].message.content`

### 错误处理
- HTTP 状态码 ≠ 200 → 抛出 RuntimeException，包含状态码和响应体
- 响应 JSON 解析失败 → 明确错误信息

## 方法1：`parseRequirement(String naturalInput, String yearMonth)`

### 触发场景
员工在需求输入页提交自然语言文本（如「6月5号和6号有事，更喜欢上白班」）

### Prompt 模板
```
你是一个排班系统的助手。请解析员工的自然语言需求，提取以下信息。
当前排班月份：{yearMonth}

请从以下文本中提取：
1. 不可上班的日期（格式：YYYY-MM-DD）
2. 偏好班次（早班或晚班）

请以JSON格式返回，格式必须严格如下：
{"unavailableDates": ["2026-07-03"], "preference": "早班", "notes": ""}

如果某字段没有信息，使用空数组或空字符串。

员工输入：{naturalInput}
```

### 返回解析
- 预期返回 JSON：`{"unavailableDates": [...], "preference": "...", "notes": "..."}`
- 使用 Jackson ObjectMapper 解析
- 需要 `extractJson()` 辅助方法：从 LLM 响应中提取 JSON 部分（处理 LLM 可能在 JSON 前后附加说明文字）

### 容错
- LLM 返回非 JSON / JSON 解析失败 → 返回 fallback 对象（空数组、空字符串、notes 包含错误信息）
- **不抛异常**，因为这个调用是用户操作的一部分，失败不应阻断流程

## 方法2：`generateSchedule(List<Map<String, Object>> requirements, String yearMonth, List<String> employeeNames, List<String> holidays)`

### 触发场景
管理员点击「生成排班」，传入所有员工的需求数据

### 参数说明
- `requirements`: 每个员工的 Map，包含 `name`（姓名）、`unavailable`（不可用日期 JSON 数组）、`preference`（偏好 JSON 对象）
- `yearMonth`: 排班月份
- `employeeNames`: 所有员工姓名列表
- `holidays`: 法定节假日日期列表

### Prompt 模板（核心——必须严格遵守）
```
你是一个智能排班系统。请根据以下条件为{yearMonth}生成排班表。

## 员工信息（序号对应）
{逐条列出：员工N: {name=xxx, unavailable=[...], preference={...}}}

## 法定节假日（不上班）
{逗号分隔的节假日列表}

## 硬性规则（必须遵守）
1. 同一员工不能在同一天上两个班次
2. 任意连续7天内，每个员工工作不超过5天
3. 每天每个班次（早班、晚班）至少安排1人
4. 法定节假日所有人不排班（休息）
5. 每人每月工作总天数不超过20天

## 软约束（尽量满足）
- 优先满足员工的不可上班日期
- 尽量满足员工的班次偏好

请为每一天（1号到月底）的早班和晚班分别安排员工，输出JSON数组：
[{"date": "2026-07-01", "morning": "员工姓名", "evening": "员工姓名"}, ...]

只输出JSON数组，不要有任何其他文字。
```

### 设计要点
- Temperature = 0.3：低温度保证输出一致性，减少随机性
- max_tokens = 4096：足够输出一个完整月份的排班 JSON
- 明确要求「只输出JSON数组，不要有任何其他文字」：降低解析失败概率
- 字段名用中文「早班/晚班」映射到代码中的 MORNING/EVENING

### 返回解析
- 预期返回：`[{"date": "2026-06-01", "morning": "张三", "evening": "李四"}, ...]`
- 使用 `extractJson()` 从响应中提取 JSON 数组
- 解析失败 → 抛出 RuntimeException（此调用是关键路径，失败需要通知管理员）

## 辅助方法

### `extractJson(String text)`
- 去除首尾空白
- 优先查找 `[...]`（JSON 数组）
- 其次查找 `{...}`（JSON 对象）
- 如果 LLM 在 JSON 前后附加了说明文字，自动截取 JSON 部分

### `callDeepSeek(String prompt)` (私有)
- 封装 HTTP 调用逻辑
- 参数组装、请求发送、响应解析
- 统一错误处理

## 安全注意事项

- API Key 通过 `@Value` 注入，不在代码中硬编码
- API Key 只通过 Authorization Header 传递，不出现在 URL 参数中
- 生产环境应将 Key 存储在安全的密钥管理服务中

## 测试验证点

- API Key 无效时：返回明确的 401 错误信息
- API 超时时：友好提示「AI 服务暂时不可用」
- LLM 返回非标准 JSON 时：extractJson 能正确截取
- 15人 × 30天的排班：max_tokens=4096 足够覆盖
