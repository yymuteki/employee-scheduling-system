package com.schedule.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

@Service
public class LLMService {

    @Value("${deepseek.api.key}")
    private String apiKey;

    @Value("${deepseek.api.url}")
    private String apiUrl;

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    public Map<String, Object> parseRequirement(String naturalInput, String yearMonth) {
        String prompt = """
            你是一个排班系统的助手。请解析员工的自然语言需求，提取以下信息。
            当前排班月份：%s
            
            请从以下文本中提取：
            1. 不可上班的日期（格式：YYYY-MM-DD）
            2. 偏好班次（早班或晚班）
            
            请以JSON格式返回，格式必须严格如下：
            {"unavailableDates": ["2026-07-03"], "preference": "早班", "notes": ""}
            
            如果某字段没有信息，使用空数组或空字符串。
            
            员工输入：%s
            """.formatted(yearMonth, naturalInput);

        try {
            String response = callDeepSeek(prompt);
            return mapper.readValue(extractJson(response), new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("unavailableDates", new ArrayList<>());
            fallback.put("preference", "");
            fallback.put("notes", "解析失败: " + e.getMessage());
            return fallback;
        }
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> generateSchedule(List<Map<String, Object>> requirements,
                                                       String yearMonth,
                                                       List<String> employeeNames,
                                                       List<String> holidays) {
        StringBuilder reqText = new StringBuilder();
        for (int i = 0; i < requirements.size(); i++) {
            reqText.append("员工").append(i + 1).append(": ").append(requirements.get(i).toString()).append("\n");
        }

        String prompt = """
            你是一个智能排班系统。请根据以下条件为%s生成排班表。
            
            ## 员工信息（序号对应）
            %s
            
            ## 法定节假日（不上班）
            %s
            
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
            """.formatted(yearMonth, reqText.toString(), String.join(", ", holidays));

        try {
            String response = callDeepSeek(prompt);
            String json = extractJson(response);
            return mapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            throw new RuntimeException("生成排班失败: " + e.getMessage(), e);
        }
    }

    private String callDeepSeek(String prompt) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("model", "deepseek-chat");
        body.put("messages", List.of(Map.of("role", "user", "content", prompt)));
        body.put("temperature", 0.3);
        body.put("max_tokens", 4096);

        String jsonBody = mapper.writeValueAsString(body);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("DeepSeek API error: " + response.statusCode() + " " + response.body());
        }

        JsonNode node = mapper.readTree(response.body());
        return node.get("choices").get(0).get("message").get("content").asText();
    }

    private String extractJson(String text) {
        text = text.trim();
        int start = text.indexOf('[');
        int end = text.lastIndexOf(']');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        start = text.indexOf('{');
        end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }
}
