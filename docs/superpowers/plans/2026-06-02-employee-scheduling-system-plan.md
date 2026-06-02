# 员工智能排班系统 — 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 构建一个基于 LLM 的 Spring Boot + React 员工排班系统，支持员工自然语言输入需求、管理员一键生成排班并手动调整后发布。

**Architecture:** Spring Boot 3 一体式应用，React SPA 打包为内嵌静态资源，H2 文件数据库零安装，DeepSeek API 负责解析员工需求 + 生成排班表。

**Tech Stack:** Java 17, Spring Boot 3, Spring Security, Spring Data JPA, H2, Maven, React 18, TypeScript, Vite, FullCalendar, Axios, DeepSeek Chat API

---

## File Structure

```
schedule-app/
├── pom.xml
├── src/main/java/com/schedule/
│   ├── ScheduleApplication.java
│   ├── config/
│   │   ├── SecurityConfig.java
│   │   └── WebConfig.java
│   ├── entity/
│   │   ├── User.java
│   │   ├── ShiftRequirement.java
│   │   └── Schedule.java
│   ├── repository/
│   │   ├── UserRepository.java
│   │   ├── ShiftRequirementRepository.java
│   │   └── ScheduleRepository.java
│   ├── dto/
│   │   ├── LoginRequest.java
│   │   ├── RequirementRequest.java
│   │   ├── ScheduleResponse.java
│   │   └── StatsResponse.java
│   ├── service/
│   │   ├── LLMService.java
│   │   ├── AuthService.java
│   │   ├── RequirementService.java
│   │   └── ScheduleService.java
│   ├── controller/
│   │   ├── AuthController.java
│   │   ├── RequirementController.java
│   │   ├── ScheduleController.java
│   │   └── AdminController.java
│   └── util/
│       └── HolidayUtil.java
├── src/main/resources/
│   ├── application.yml
│   └── data.sql
├── src/main/frontend/
│   ├── package.json
│   ├── vite.config.ts
│   ├── tsconfig.json
│   ├── index.html
│   └── src/
│       ├── main.tsx
│       ├── App.tsx
│       ├── api/
│       │   └── client.ts
│       ├── components/
│       │   ├── Layout.tsx
│       │   └── ScheduleCalendar.tsx
│       └── pages/
│           ├── LoginPage.tsx
│           ├── EmployeeRequirements.tsx
│           ├── MySchedule.tsx
│           ├── AdminRequirements.tsx
│           ├── AdminSchedule.tsx
│           └── AdminGenerate.tsx
└── README.md
```

**Key design decisions per file:**
- `LLMService.java` — all DeepSeek API interaction, prompt templates, JSON parsing, retry logic
- `ScheduleService.java` — business rules validation (5 constraints), CRUD, publish workflow
- `RequirementService.java` — parse + store employee requirements, LLM integration
- `ScheduleCalendar.tsx` — FullCalendar wrapper, color-coded shifts
- `ScheduleTable.tsx` — editable grid for admin, shift dropdown, per-cell color
- `Layout.tsx` — role-based nav, protected routes

---

### Task 1: Maven Project Scaffold

**Files:**
- Create: `pom.xml`

- [ ] **Step 1: Create pom.xml with all dependencies**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.0</version>
    </parent>
    <groupId>com.schedule</groupId>
    <artifactId>schedule-app</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>
    <name>schedule-app</name>

    <properties>
        <java.version>17</java.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: Create main application class**

Create `src/main/java/com/schedule/ScheduleApplication.java`:

```java
package com.schedule;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ScheduleApplication {
    public static void main(String[] args) {
        SpringApplication.run(ScheduleApplication.class, args);
    }
}
```

- [ ] **Step 3: Create application.yml**

Create `src/main/resources/application.yml`:

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:h2:file:./data/schedule;DB_CLOSE_DELAY=-1;MODE=MySQL
    driver-class-name: org.h2.Driver
    username: sa
    password:
  h2:
    console:
      enabled: false
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.H2Dialect
  sql:
    init:
      mode: always

deepseek:
  api:
    key: ${DEEPSEEK_API_KEY:your-api-key-here}
    url: https://api.deepseek.com/chat/completions
```

- [ ] **Step 4: Verify Maven can compile**

```bash
mvn compile
```

Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add pom.xml src/main/java/com/schedule/ScheduleApplication.java src/main/resources/application.yml
git commit -m "feat: scaffold Spring Boot project with Maven"
```

---

### Task 2: Entity Classes

**Files:**
- Create: `src/main/java/com/schedule/entity/User.java`
- Create: `src/main/java/com/schedule/entity/ShiftRequirement.java`
- Create: `src/main/java/com/schedule/entity/Schedule.java`

- [ ] **Step 1: Create User entity**

```java
package com.schedule.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Role role;

    public enum Role {
        EMPLOYEE, ADMIN
    }
}
```

- [ ] **Step 2: Create ShiftRequirement entity**

```java
package com.schedule.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "shift_requirements")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShiftRequirement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "year_month", nullable = false, length = 7)
    private String yearMonth;

    @Column(name = "natural_input", columnDefinition = "TEXT")
    private String naturalInput;

    @Column(name = "parsed_unavailable", columnDefinition = "TEXT")
    private String parsedUnavailable; // JSON array string

    @Column(name = "parsed_preferences", columnDefinition = "TEXT")
    private String parsedPreferences; // JSON object string

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

- [ ] **Step 3: Create Schedule entity**

```java
package com.schedule.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;

@Entity
@Table(name = "schedules", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "date"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Schedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Shift shift;

    @Column(name = "year_month", nullable = false, length = 7)
    private String yearMonth;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Status status;

    public enum Shift {
        MORNING, EVENING, OFF
    }

    public enum Status {
        DRAFT, PUBLISHED
    }
}
```

- [ ] **Step 4: Verify compilation**

```bash
mvn compile
```

Expected: BUILD SUCCESS (may show warnings about lombok — that's OK)

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/schedule/entity/
git commit -m "feat: add JPA entities (User, ShiftRequirement, Schedule)"
```

---

### Task 3: Repositories

**Files:**
- Create: `src/main/java/com/schedule/repository/UserRepository.java`
- Create: `src/main/java/com/schedule/repository/ShiftRequirementRepository.java`
- Create: `src/main/java/com/schedule/repository/ScheduleRepository.java`

- [ ] **Step 1: Create UserRepository**

```java
package com.schedule.repository;

import com.schedule.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    List<User> findByRole(User.Role role);
    boolean existsByUsername(String username);
}
```

- [ ] **Step 2: Create ShiftRequirementRepository**

```java
package com.schedule.repository;

import com.schedule.entity.ShiftRequirement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ShiftRequirementRepository extends JpaRepository<ShiftRequirement, Long> {
    Optional<ShiftRequirement> findByUserIdAndYearMonth(Long userId, String yearMonth);
    java.util.List<ShiftRequirement> findByYearMonth(String yearMonth);
}
```

- [ ] **Step 3: Create ScheduleRepository**

```java
package com.schedule.repository;

import com.schedule.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    List<Schedule> findByYearMonthAndStatus(String yearMonth, Schedule.Status status);
    List<Schedule> findByYearMonth(String yearMonth);
    List<Schedule> findByUserIdAndYearMonth(Long userId, String yearMonth);
    void deleteByYearMonthAndStatus(String yearMonth, Schedule.Status status);

    @Modifying
    @Transactional
    @Query("UPDATE Schedule s SET s.status = 'PUBLISHED' WHERE s.yearMonth = :yearMonth AND s.status = 'DRAFT'")
    void publishByYearMonth(String yearMonth);

    @Modifying
    @Transactional
    @Query("UPDATE Schedule s SET s.status = 'DRAFT' WHERE s.yearMonth = :yearMonth AND s.status = 'PUBLISHED'")
    void unpublishByYearMonth(String yearMonth);

    @Query("SELECT COUNT(s) FROM Schedule s WHERE s.user.id = :userId AND s.yearMonth = :yearMonth AND s.shift IN ('MORNING', 'EVENING')")
    long countWorkingDaysByUserIdAndMonth(Long userId, String yearMonth);

    @Query("SELECT s FROM Schedule s WHERE s.user.id = :userId AND s.date = :date")
    Optional<Schedule> findByUserIdAndDate(Long userId, LocalDate date);
}
```

- [ ] **Step 4: Verify compilation**

```bash
mvn compile
```

Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/schedule/repository/
git commit -m "feat: add JPA repositories"
```

---

### Task 4: DTOs

**Files:**
- Create: `src/main/java/com/schedule/dto/LoginRequest.java`
- Create: `src/main/java/com/schedule/dto/RequirementRequest.java`
- Create: `src/main/java/com/schedule/dto/RequirementResponse.java`
- Create: `src/main/java/com/schedule/dto/ScheduleResponse.java`
- Create: `src/main/java/com/schedule/dto/StatsResponse.java`
- Create: `src/main/java/com/schedule/dto/GenerateRequest.java`

- [ ] **Step 1: Create LoginRequest DTO**

```java
package com.schedule.dto;

public class LoginRequest {
    private String username;
    private String password;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
```

- [ ] **Step 2: Create RequirementRequest DTO**

```java
package com.schedule.dto;

public class RequirementRequest {
    private String naturalInput;
    private String yearMonth;

    public String getNaturalInput() { return naturalInput; }
    public void setNaturalInput(String naturalInput) { this.naturalInput = naturalInput; }
    public String getYearMonth() { return yearMonth; }
    public void setYearMonth(String yearMonth) { this.yearMonth = yearMonth; }
}
```

- [ ] **Step 3: Create RequirementResponse DTO**

```java
package com.schedule.dto;

public class RequirementResponse {
    private Long id;
    private String yearMonth;
    private String naturalInput;
    private String parsedUnavailable;
    private String parsedPreferences;
    private String notes;

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getYearMonth() { return yearMonth; }
    public void setYearMonth(String yearMonth) { this.yearMonth = yearMonth; }
    public String getNaturalInput() { return naturalInput; }
    public void setNaturalInput(String naturalInput) { this.naturalInput = naturalInput; }
    public String getParsedUnavailable() { return parsedUnavailable; }
    public void setParsedUnavailable(String parsedUnavailable) { this.parsedUnavailable = parsedUnavailable; }
    public String getParsedPreferences() { return parsedPreferences; }
    public void setParsedPreferences(String parsedPreferences) { this.parsedPreferences = parsedPreferences; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
```

- [ ] **Step 4: Create ScheduleResponse DTO**

```java
package com.schedule.dto;

import java.time.LocalDate;

public class ScheduleResponse {
    private Long id;
    private Long userId;
    private String userName;
    private LocalDate date;
    private String shift;
    private String status;

    public ScheduleResponse() {}

    public ScheduleResponse(Long id, Long userId, String userName, LocalDate date, String shift, String status) {
        this.id = id;
        this.userId = userId;
        this.userName = userName;
        this.date = date;
        this.shift = shift;
        this.status = status;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public String getShift() { return shift; }
    public void setShift(String shift) { this.shift = shift; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
```

- [ ] **Step 5: Create StatsResponse DTO**

```java
package com.schedule.dto;

public class StatsResponse {
    private Long userId;
    private String userName;
    private long totalDays;
    private long morningDays;
    private long eveningDays;
    private long offDays;

    public StatsResponse(Long userId, String userName, long totalDays, long morningDays, long eveningDays, long offDays) {
        this.userId = userId;
        this.userName = userName;
        this.totalDays = totalDays;
        this.morningDays = morningDays;
        this.eveningDays = eveningDays;
        this.offDays = offDays;
    }

    public Long getUserId() { return userId; }
    public String getUserName() { return userName; }
    public long getTotalDays() { return totalDays; }
    public long getMorningDays() { return morningDays; }
    public long getEveningDays() { return eveningDays; }
    public long getOffDays() { return offDays; }
}
```

- [ ] **Step 6: Create GenerateRequest DTO**

```java
package com.schedule.dto;

public class GenerateRequest {
    private String yearMonth;

    public String getYearMonth() { return yearMonth; }
    public void setYearMonth(String yearMonth) { this.yearMonth = yearMonth; }
}
```

- [ ] **Step 7: Verify compilation**

```bash
mvn compile
```

Expected: BUILD SUCCESS

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/schedule/dto/
git commit -m "feat: add DTOs for API requests and responses"
```

---

### Task 5: Holiday Utility

**Files:**
- Create: `src/main/java/com/schedule/util/HolidayUtil.java`

- [ ] **Step 1: Create HolidayUtil**

```java
package com.schedule.util;

import java.time.LocalDate;
import java.util.Set;

public class HolidayUtil {
    // 2026 China legal holidays
    private static final Set<LocalDate> HOLIDAYS_2026 = Set.of(
        LocalDate.of(2026, 1, 1),
        LocalDate.of(2026, 1, 2),
        LocalDate.of(2026, 2, 16), LocalDate.of(2026, 2, 17), LocalDate.of(2026, 2, 18),
        LocalDate.of(2026, 2, 19), LocalDate.of(2026, 2, 20),
        LocalDate.of(2026, 4, 5),
        LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 2), LocalDate.of(2026, 5, 3),
        LocalDate.of(2026, 6, 19),
        LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 2), LocalDate.of(2026, 10, 3),
        LocalDate.of(2026, 10, 4), LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 6)
    );

    public static boolean isHoliday(LocalDate date) {
        return HOLIDAYS_2026.contains(date);
    }
}
```

- [ ] **Step 2: Verify compilation**

```bash
mvn compile
```

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/schedule/util/
git commit -m "feat: add holiday utility with 2026 China holidays"
```

---

### Task 6: LLM Service

**Files:**
- Create: `src/main/java/com/schedule/service/LLMService.java`

- [ ] **Step 1: Create LLMService**

```java
package com.schedule.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.util.*;

@Service
public class LLMService {

    @Value("${deepseek.api.key}")
    private String apiKey;

    @Value("${deepseek.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, Object> parseRequirement(String naturalInput, String yearMonth) {
        String year = yearMonth.split("-")[0];
        String month = yearMonth.split("-")[1];
        String prompt = String.format("""
            你是一个排班需求解析器。当前月份是%s年%s月。
            请解析以下员工关于排班需求的自然语言输入，提取出结构化信息。

            输入："%s"

            请只返回JSON，不要有其他文字。格式如下：
            {
              "unavailable_dates": ["YYYY-MM-DD", ...],
              "prefer_morning": true/false,
              "prefer_dates": ["YYYY-MM-DD", ...],
              "notes": "简短摘要"
            }""", year, month, naturalInput);

        String response = callDeepSeek(prompt);
        try {
            String json = extractJson(response);
            JsonNode root = objectMapper.readTree(json);
            Map<String, Object> result = new HashMap<>();
            List<String> unavailableDates = new ArrayList<>();
            if (root.has("unavailable_dates")) {
                root.get("unavailable_dates").forEach(d -> unavailableDates.add(d.asText()));
            }
            result.put("unavailable_dates", unavailableDates);
            result.put("prefer_morning", root.has("prefer_morning") ? root.get("prefer_morning").asBoolean() : false);
            List<String> preferDates = new ArrayList<>();
            if (root.has("prefer_dates")) {
                root.get("prefer_dates").forEach(d -> preferDates.add(d.asText()));
            }
            result.put("prefer_dates", preferDates);
            result.put("notes", root.has("notes") ? root.get("notes").asText() : "");
            return result;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("LLM返回格式无法解析，请重新描述您的需求");
        }
    }

    public List<Map<String, Object>> generateSchedule(String yearMonth, String employeePreferences, String holidays) {
        String[] parts = yearMonth.split("-");
        String prompt = String.format("""
            你是一个排班生成器。请根据以下信息生成%s年%s月的排班表。

            ## 班次类型
            - MORNING: 早班 (08:00-16:00)
            - EVENING: 晚班 (16:00-00:00)
            - OFF: 休息

            ## 硬性规则（必须遵守，违反任何一条都是无效输出）
            1. 同一员工不能在同一天安排两个班次
            2. 同一员工不能连续上班超过4天（第5天必须休息）
            3. 每天每个班次至少安排1人
            4. 法定节假日所有人安排OFF: %s
            5. 每人每月上班天数不超过20天

            ## 员工偏好（尽量满足）
            %s

            请返回JSON：
            {"schedule": [{"user_id": 1, "date": "YYYY-MM-DD", "shift": "MORNING"}, ...]}""",
            parts[0], parts[1], holidays, employeePreferences);

        String response = callDeepSeek(prompt);
        try {
            String json = extractJson(response);
            JsonNode root = objectMapper.readTree(json);
            List<Map<String, Object>> schedule = new ArrayList<>();
            for (JsonNode node : root.get("schedule")) {
                Map<String, Object> entry = new HashMap<>();
                entry.put("user_id", node.get("user_id").asLong());
                entry.put("date", node.get("date").asText());
                entry.put("shift", node.get("shift").asText());
                schedule.add(entry);
            }
            return schedule;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("AI生成的排班格式无法解析");
        }
    }

    private String callDeepSeek(String userPrompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = new HashMap<>();
        body.put("model", "deepseek-chat");
        body.put("temperature", 0.3);
        body.put("max_tokens", 4096);
        body.put("messages", List.of(
            Map.of("role", "system", "content", "你是一个排班助手。只返回要求的JSON格式，不要有其他文字。"),
            Map.of("role", "user", "content", userPrompt)
        ));

        try {
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, request, Map.class);
            Map<String, Object> responseBody = response.getBody();
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            return (String) message.get("content");
        } catch (Exception e) {
            throw new RuntimeException("AI服务暂时不可用，请稍后重试: " + e.getMessage());
        }
    }

    private String extractJson(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        throw new RuntimeException("AI未返回有效的JSON格式");
    }
}
```

- [ ] **Step 2: Verify compilation**

```bash
mvn compile
```

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/schedule/service/LLMService.java
git commit -m "feat: add LLM service with DeepSeek integration"
```

---

### Task 7: Auth Service

**Files:**
- Create: `src/main/java/com/schedule/service/AuthService.java`

- [ ] **Step 1: Create AuthService**

```java
package com.schedule.service;

import com.schedule.dto.LoginRequest;
import com.schedule.entity.User;
import com.schedule.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Map<String, Object> login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
            .orElseThrow(() -> new RuntimeException("账号或密码错误"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("账号或密码错误");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("id", user.getId());
        result.put("username", user.getUsername());
        result.put("name", user.getName());
        result.put("role", user.getRole().name());
        return result;
    }
}
```

- [ ] **Step 2: Verify compilation**

```bash
mvn compile
```

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/schedule/service/AuthService.java
git commit -m "feat: add auth service with BCrypt login"
```

---

### Task 8: Requirement Service

**Files:**
- Create: `src/main/java/com/schedule/service/RequirementService.java`

- [ ] **Step 1: Create RequirementService**

```java
package com.schedule.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schedule.dto.RequirementRequest;
import com.schedule.dto.RequirementResponse;
import com.schedule.entity.ShiftRequirement;
import com.schedule.entity.User;
import com.schedule.repository.ShiftRequirementRepository;
import com.schedule.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class RequirementService {

    private final ShiftRequirementRepository requirementRepository;
    private final UserRepository userRepository;
    private final LLMService llmService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RequirementService(ShiftRequirementRepository requirementRepository,
                              UserRepository userRepository,
                              LLMService llmService) {
        this.requirementRepository = requirementRepository;
        this.userRepository = userRepository;
        this.llmService = llmService;
    }

    public Map<String, Object> submit(RequirementRequest request, Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("用户不存在"));

        // If empty input, treat as no special requirements
        if (request.getNaturalInput() == null || request.getNaturalInput().trim().isEmpty()) {
            ShiftRequirement req = requirementRepository
                .findByUserIdAndYearMonth(userId, request.getYearMonth())
                .orElse(new ShiftRequirement());
            req.setUser(user);
            req.setYearMonth(request.getYearMonth());
            req.setNaturalInput("");
            req.setParsedUnavailable("[]");
            req.setParsedPreferences("{\"prefer_morning\": false, \"prefer_dates\": []}");
            requirementRepository.save(req);
            Map<String, Object> result = new HashMap<>();
            result.put("parsed", Map.of("unavailable_dates", List.of(), "prefer_morning", false, "notes", "无特殊要求"));
            return result;
        }

        // Call LLM to parse
        Map<String, Object> parsed = llmService.parseRequirement(
            request.getNaturalInput(), request.getYearMonth());

        // Save or update
        ShiftRequirement req = requirementRepository
            .findByUserIdAndYearMonth(userId, request.getYearMonth())
            .orElse(new ShiftRequirement());

        req.setUser(user);
        req.setYearMonth(request.getYearMonth());
        req.setNaturalInput(request.getNaturalInput());

        try {
            req.setParsedUnavailable(objectMapper.writeValueAsString(parsed.get("unavailable_dates")));
            Map<String, Object> prefs = new HashMap<>();
            prefs.put("prefer_morning", parsed.get("prefer_morning"));
            prefs.put("prefer_dates", parsed.get("prefer_dates"));
            req.setParsedPreferences(objectMapper.writeValueAsString(prefs));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("数据序列化失败");
        }

        requirementRepository.save(req);

        Map<String, Object> result = new HashMap<>();
        result.put("parsed", parsed);
        return result;
    }

    public RequirementResponse getMyRequirement(Long userId, String yearMonth) {
        return requirementRepository.findByUserIdAndYearMonth(userId, yearMonth)
            .map(this::toResponse)
            .orElse(null);
    }

    public List<Map<String, Object>> getAllRequirements(String yearMonth) {
        List<Map<String, Object>> results = new ArrayList<>();
        for (ShiftRequirement req : requirementRepository.findByYearMonth(yearMonth)) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", req.getId());
            item.put("userName", req.getUser().getName());
            item.put("userId", req.getUser().getId());
            item.put("naturalInput", req.getNaturalInput());
            item.put("parsedUnavailable", req.getParsedUnavailable());
            item.put("parsedPreferences", req.getParsedPreferences());
            item.put("createdAt", req.getCreatedAt());
            results.add(item);
        }
        return results;
    }

    private RequirementResponse toResponse(ShiftRequirement req) {
        RequirementResponse r = new RequirementResponse();
        r.setId(req.getId());
        r.setYearMonth(req.getYearMonth());
        r.setNaturalInput(req.getNaturalInput());
        r.setParsedUnavailable(req.getParsedUnavailable());
        r.setParsedPreferences(req.getParsedPreferences());

        try {
            if (req.getParsedPreferences() != null) {
                JsonNode prefs = objectMapper.readTree(req.getParsedPreferences());
                r.setNotes(prefs.has("prefer_morning") && prefs.get("prefer_morning").asBoolean()
                    ? "偏好早班" : "");
            }
        } catch (Exception e) { /* ignore */ }
        return r;
    }
}
```

- [ ] **Step 2: Verify compilation**

```bash
mvn compile
```

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/schedule/service/RequirementService.java
git commit -m "feat: add requirement service with LLM parsing"
```

---

### Task 9: Schedule Service

**Files:**
- Create: `src/main/java/com/schedule/service/ScheduleService.java`

- [ ] **Step 1: Create ScheduleService**

```java
package com.schedule.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schedule.dto.ScheduleResponse;
import com.schedule.dto.StatsResponse;
import com.schedule.entity.Schedule;
import com.schedule.entity.User;
import com.schedule.entity.ShiftRequirement;
import com.schedule.repository.ScheduleRepository;
import com.schedule.repository.UserRepository;
import com.schedule.repository.ShiftRequirementRepository;
import com.schedule.util.HolidayUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final UserRepository userRepository;
    private final ShiftRequirementRepository requirementRepository;
    private final LLMService llmService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ScheduleService(ScheduleRepository scheduleRepository,
                           UserRepository userRepository,
                           ShiftRequirementRepository requirementRepository,
                           LLMService llmService) {
        this.scheduleRepository = scheduleRepository;
        this.userRepository = userRepository;
        this.requirementRepository = requirementRepository;
        this.llmService = llmService;
    }

    public List<ScheduleResponse> getPublishedSchedule(String yearMonth) {
        return scheduleRepository.findByYearMonthAndStatus(yearMonth, Schedule.Status.PUBLISHED)
            .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<ScheduleResponse> getAllSchedules(String yearMonth) {
        return scheduleRepository.findByYearMonth(yearMonth)
            .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<ScheduleResponse> generate(String yearMonth) {
        // Check if generation is already in progress (simple idempotency)
        List<Schedule> existing = scheduleRepository.findByYearMonthAndStatus(yearMonth, Schedule.Status.DRAFT);
        if (!existing.isEmpty()) {
            throw new RuntimeException("已有未发布的草稿排班，请先处理后再重新生成");
        }

        List<User> employees = userRepository.findByRole(User.Role.EMPLOYEE);
        if (employees.isEmpty()) {
            throw new RuntimeException("没有员工账号，请先添加员工");
        }

        List<ShiftRequirement> requirements = requirementRepository.findByYearMonth(yearMonth);

        // Build employee preferences string for LLM
        StringBuilder prefsBuilder = new StringBuilder();
        for (ShiftRequirement req : requirements) {
            prefsBuilder.append(String.format("员工 %s (ID:%d): 不可上班日期: %s, 偏好: %s\n",
                req.getUser().getName(), req.getUser().getId(),
                req.getParsedUnavailable(), req.getParsedPreferences()));
        }
        String employeePrefs = prefsBuilder.toString();

        // Build holidays string
        String[] parts = yearMonth.split("-");
        int year = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]);
        LocalDate firstDay = LocalDate.of(year, month, 1);
        LocalDate lastDay = firstDay.plusMonths(1).minusDays(1);

        StringBuilder holidaysBuilder = new StringBuilder();
        for (LocalDate d = firstDay; !d.isAfter(lastDay); d = d.plusDays(1)) {
            if (HolidayUtil.isHoliday(d)) {
                holidaysBuilder.append(d.toString()).append(", ");
            }
        }
        String holidays = holidaysBuilder.toString();

        // Call LLM (with retry)
        List<Map<String, Object>> llmResult = null;
        String lastError = null;
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                llmResult = llmService.generateSchedule(yearMonth, employeePrefs, holidays);
                // Validate result
                validateSchedule(llmResult, employees, yearMonth);
                lastError = null;
                break;
            } catch (Exception e) {
                lastError = e.getMessage();
            }
        }

        if (lastError != null) {
            throw new RuntimeException("AI生成排班失败（已重试）: " + lastError);
        }

        // Delete old drafts
        scheduleRepository.deleteByYearMonthAndStatus(yearMonth, Schedule.Status.DRAFT);

        // Save new schedules
        for (Map<String, Object> entry : llmResult) {
            Long userId = ((Number) entry.get("user_id")).longValue();
            LocalDate date = LocalDate.parse((String) entry.get("date"));
            Schedule.Shift shift = Schedule.Shift.valueOf((String) entry.get("shift"));

            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("员工ID不存在: " + userId));

            Schedule schedule = new Schedule();
            schedule.setUser(user);
            schedule.setDate(date);
            schedule.setShift(shift);
            schedule.setYearMonth(yearMonth);
            schedule.setStatus(Schedule.Status.DRAFT);
            scheduleRepository.save(schedule);
        }

        return scheduleRepository.findByYearMonthAndStatus(yearMonth, Schedule.Status.DRAFT)
            .stream().map(this::toResponse).collect(Collectors.toList());
    }

    private void validateSchedule(List<Map<String, Object>> entries, List<User> employees, String yearMonth) {
        // Rule 1: No duplicate (user_id + date) - enforced by DB unique constraint

        // Rule 3: Each shift has at least 1 person per day
        String[] parts = yearMonth.split("-");
        int year = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]);
        LocalDate firstDay = LocalDate.of(year, month, 1);
        LocalDate lastDay = firstDay.plusMonths(1).minusDays(1);

        for (LocalDate d = firstDay; !d.isAfter(lastDay); d = d.plusDays(1)) {
            if (HolidayUtil.isHoliday(d)) continue;
            final LocalDate date = d;
            long morningCount = entries.stream()
                .filter(e -> e.get("date").equals(date.toString()) && "MORNING".equals(e.get("shift")))
                .count();
            long eveningCount = entries.stream()
                .filter(e -> e.get("date").equals(date.toString()) && "EVENING".equals(e.get("shift")))
                .count();
            if (morningCount < 1) throw new RuntimeException(date + " 早班没人");
            if (eveningCount < 1) throw new RuntimeException(date + " 晚班没人");
        }

        // Rule 2: No employee works 5+ consecutive days
        for (User user : employees) {
            List<Map<String, Object>> userEntries = entries.stream()
                .filter(e -> ((Number) e.get("user_id")).longValue() == user.getId())
                .sorted(Comparator.comparing(e -> (String) e.get("date")))
                .collect(Collectors.toList());

            int consecutive = 0;
            LocalDate prevDate = null;
            for (Map<String, Object> e : userEntries) {
                String shift = (String) e.get("shift");
                LocalDate date = LocalDate.parse((String) e.get("date"));
                if (!shift.equals("OFF")) {
                    if (prevDate != null && date.equals(prevDate.plusDays(1))) {
                        consecutive++;
                    } else {
                        consecutive = 1;
                    }
                    if (consecutive >= 5) throw new RuntimeException(
                        user.getName() + " 连续上班超过4天");
                } else {
                    consecutive = 0;
                }
                prevDate = date;
            }
        }

        // Rule 4: Holidays are OFF
        for (Map<String, Object> e : entries) {
            LocalDate date = LocalDate.parse((String) e.get("date"));
            if (HolidayUtil.isHoliday(date) && !"OFF".equals(e.get("shift"))) {
                throw new RuntimeException(date + " 是法定节假日，必须休息");
            }
        }

        // Rule 5: Max 20 working days
        for (User user : employees) {
            long workingDays = entries.stream()
                .filter(e -> ((Number) e.get("user_id")).longValue() == user.getId()
                    && !"OFF".equals(e.get("shift")))
                .count();
            if (workingDays > 20) throw new RuntimeException(
                user.getName() + " 上班天数超过20天: " + workingDays);
        }
    }

    public ScheduleResponse updateSchedule(Long scheduleId, String newShift) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
            .orElseThrow(() -> new RuntimeException("排班记录不存在"));
        if (schedule.getStatus() == Schedule.Status.PUBLISHED) {
            throw new RuntimeException("已发布的排班不能直接修改，请先取消发布");
        }
        schedule.setShift(Schedule.Shift.valueOf(newShift));
        scheduleRepository.save(schedule);
        return toResponse(schedule);
    }

    public void publish(String yearMonth) {
        scheduleRepository.publishByYearMonth(yearMonth);
    }

    public void unpublish(String yearMonth) {
        scheduleRepository.unpublishByYearMonth(yearMonth);
    }

    public List<StatsResponse> getStats(String yearMonth) {
        List<User> employees = userRepository.findByRole(User.Role.EMPLOYEE);
        List<StatsResponse> stats = new ArrayList<>();
        String[] parts = yearMonth.split("-");
        int year = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]);
        int daysInMonth = LocalDate.of(year, month, 1).plusMonths(1).minusDays(1).getDayOfMonth();

        for (User user : employees) {
            List<Schedule> schedules = scheduleRepository.findByUserIdAndYearMonth(user.getId(), yearMonth);
            long morning = schedules.stream().filter(s -> s.getShift() == Schedule.Shift.MORNING).count();
            long evening = schedules.stream().filter(s -> s.getShift() == Schedule.Shift.EVENING).count();
            long off = schedules.stream().filter(s -> s.getShift() == Schedule.Shift.OFF).count();
            stats.add(new StatsResponse(user.getId(), user.getName(), morning + evening, morning, evening, off));
        }
        return stats;
    }

    private ScheduleResponse toResponse(Schedule s) {
        return new ScheduleResponse(s.getId(), s.getUser().getId(), s.getUser().getName(),
            s.getDate(), s.getShift().name(), s.getStatus().name());
    }
}
```

- [ ] **Step 2: Verify compilation**

```bash
mvn compile
```

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/schedule/service/ScheduleService.java src/main/java/com/schedule/service/RequirementService.java
git commit -m "feat: add schedule service with generation, validation, publish"
```

---

### Task 10: Security Configuration

**Files:**
- Create: `src/main/java/com/schedule/config/SecurityConfig.java`
- Create: `src/main/java/com/schedule/config/WebConfig.java`
- Modify: `src/main/resources/application.yml`

- [ ] **Step 1: Create SecurityConfig**

```java
package com.schedule.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/**").hasAnyRole("EMPLOYEE", "ADMIN")
                .requestMatchers("/", "/index.html", "/static/**", "/assets/**", "/*.js", "/*.css").permitAll()
                .anyRequest().permitAll()
            )
            .formLogin(form -> form.disable())
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessHandler((req, res, auth) -> {
                    res.setStatus(HttpServletResponse.SC_OK);
                    res.getWriter().write("{\"message\": \"logged out\"}");
                    res.setContentType("application/json");
                })
                .invalidateHttpSession(true)
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, auth) -> {
                    if (req.getRequestURI().startsWith("/api/")) {
                        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        res.getWriter().write("{\"error\": \"请先登录\"}");
                        res.setContentType("application/json");
                    }
                })
            );
        return http.build();
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

- [ ] **Step 2: Create WebConfig for SPA routing**

```java
package com.schedule.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("forward:/index.html");
        registry.addViewController("/login").setViewName("forward:/index.html");
        registry.addViewController("/requirements").setViewName("forward:/index.html");
        registry.addViewController("/schedule").setViewName("forward:/index.html");
        registry.addViewController("/admin/requirements").setViewName("forward:/index.html");
        registry.addViewController("/admin/schedule").setViewName("forward:/index.html");
        registry.addViewController("/admin/generate").setViewName("forward:/index.html");
    }
}
```

- [ ] **Step 3: Add session config to application.yml**

Append to `application.yml`:
```yaml
  session:
    timeout: 86400

logging:
  level:
    com.schedule: DEBUG
```

- [ ] **Step 4: Verify compilation**

```bash
mvn compile
```

Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/schedule/config/ src/main/resources/application.yml
git commit -m "feat: add security and web configuration"
```

---

### Task 11: Controllers

**Files:**
- Create: `src/main/java/com/schedule/controller/AuthController.java`
- Create: `src/main/java/com/schedule/controller/RequirementController.java`
- Create: `src/main/java/com/schedule/controller/ScheduleController.java`
- Create: `src/main/java/com/schedule/controller/AdminController.java`

- [ ] **Step 1: Create AuthController**

```java
package com.schedule.controller;

import com.schedule.dto.LoginRequest;
import com.schedule.service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpSession session) {
        try {
            Map<String, Object> user = authService.login(request);
            // Set Spring Security context
            List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + user.get("role"))
            );
            UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(user.get("username"), null, authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);
            session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());
            session.setAttribute("user", user);

            Map<String, Object> result = new HashMap<>(user);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(HttpSession session) {
        Object user = session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "未登录"));
        }
        return ResponseEntity.ok(user);
    }
}
```

- [ ] **Step 2: Create RequirementController**

```java
package com.schedule.controller;

import com.schedule.dto.RequirementRequest;
import com.schedule.service.RequirementService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/requirements")
public class RequirementController {

    private final RequirementService requirementService;

    public RequirementController(RequirementService requirementService) {
        this.requirementService = requirementService;
    }

    @GetMapping("/my")
    public ResponseEntity<?> getMyRequirement(@RequestParam String month, HttpSession session) {
        Map<String, Object> user = (Map<String, Object>) session.getAttribute("user");
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "未登录"));
        Long userId = ((Number) user.get("id")).longValue();
        return ResponseEntity.ok(requirementService.getMyRequirement(userId, month));
    }

    @PostMapping
    public ResponseEntity<?> submit(@RequestBody RequirementRequest request, HttpSession session) {
        try {
            Map<String, Object> user = (Map<String, Object>) session.getAttribute("user");
            if (user == null) return ResponseEntity.status(401).body(Map.of("error", "未登录"));
            Long userId = ((Number) user.get("id")).longValue();
            Map<String, Object> result = requirementService.submit(request, userId);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
```

- [ ] **Step 3: Create ScheduleController**

```java
package com.schedule.controller;

import com.schedule.service.ScheduleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/schedule")
public class ScheduleController {

    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @GetMapping
    public ResponseEntity<?> getSchedule(@RequestParam String month) {
        return ResponseEntity.ok(scheduleService.getPublishedSchedule(month));
    }
}
```

- [ ] **Step 4: Create AdminController**

```java
package com.schedule.controller;

import com.schedule.dto.GenerateRequest;
import com.schedule.service.RequirementService;
import com.schedule.service.ScheduleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final RequirementService requirementService;
    private final ScheduleService scheduleService;

    public AdminController(RequirementService requirementService, ScheduleService scheduleService) {
        this.requirementService = requirementService;
        this.scheduleService = scheduleService;
    }

    @GetMapping("/requirements")
    public ResponseEntity<?> getRequirements(@RequestParam String month) {
        return ResponseEntity.ok(requirementService.getAllRequirements(month));
    }

    @GetMapping("/schedule")
    public ResponseEntity<?> getSchedule(@RequestParam String month) {
        return ResponseEntity.ok(scheduleService.getAllSchedules(month));
    }

    @PostMapping("/schedule/generate")
    public ResponseEntity<?> generateSchedule(@RequestBody GenerateRequest request) {
        try {
            return ResponseEntity.ok(scheduleService.generate(request.getYearMonth()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/schedule/{id}")
    public ResponseEntity<?> updateSchedule(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(scheduleService.updateSchedule(id, body.get("shift")));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/schedule/publish")
    public ResponseEntity<?> publish(@RequestBody GenerateRequest request) {
        try {
            scheduleService.publish(request.getYearMonth());
            return ResponseEntity.ok(Map.of("message", "发布成功"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/schedule/unpublish")
    public ResponseEntity<?> unpublish(@RequestBody GenerateRequest request) {
        try {
            scheduleService.unpublish(request.getYearMonth());
            return ResponseEntity.ok(Map.of("message", "已取消发布"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/schedule/stats")
    public ResponseEntity<?> stats(@RequestParam String month) {
        return ResponseEntity.ok(scheduleService.getStats(month));
    }
}
```

- [ ] **Step 5: Verify compilation**

```bash
mvn compile
```

Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/schedule/controller/
git commit -m "feat: add REST API controllers"
```

---

### Task 12: Initial Data & Data Loader

**Files:**
- Create: `src/main/resources/data.sql`
- Create: `src/main/java/com/schedule/config/DataInitializer.java`

- [ ] **Step 1: Create DataInitializer**

```java
package com.schedule.config;

import com.schedule.entity.User;
import com.schedule.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initData(UserRepository userRepository) {
        return args -> {
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

            if (!userRepository.existsByUsername("admin")) {
                User admin = new User();
                admin.setUsername("admin");
                admin.setPassword(encoder.encode("admin123"));
                admin.setName("管理员");
                admin.setRole(User.Role.ADMIN);
                userRepository.save(admin);
            }

            // Pre-create some employee accounts for testing
            String[] employees = {"张三", "李四", "王五", "赵六", "钱七"};
            for (int i = 0; i < employees.length; i++) {
                String username = "emp" + (i + 1);
                if (!userRepository.existsByUsername(username)) {
                    User emp = new User();
                    emp.setUsername(username);
                    emp.setPassword(encoder.encode("123456"));
                    emp.setName(employees[i]);
                    emp.setRole(User.Role.EMPLOYEE);
                    userRepository.save(emp);
                }
            }
            System.out.println("Initialized " + (1 + employees.length) + " users");
        };
    }
}
```

- [ ] **Step 2: Verify compilation**

```bash
mvn compile
```

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/schedule/config/DataInitializer.java
git commit -m "feat: add initial data with admin and test employee accounts"
```

---

### Task 13: Backend Smoke Test

- [ ] **Step 1: Start the application**

```bash
mvn spring-boot:run
```

Wait for "Started ScheduleApplication" message (about 5-10 seconds).

- [ ] **Step 2: Test login API**

In another terminal:
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' \
  -c cookies.txt
```

Expected: `{"id":1,"username":"admin","name":"管理员","role":"ADMIN"}`

- [ ] **Step 3: Test employee login**

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"emp1","password":"123456"}' \
  -c cookies.txt
```

Expected: `{"id":2,"username":"emp1","name":"张三","role":"EMPLOYEE"}`

- [ ] **Step 4: Stop the server**

Press Ctrl+C in the server terminal.

- [ ] **Step 5: Commit**

```bash
git commit -m "test: verify backend APIs work correctly" --allow-empty
```

---

### Task 14: Frontend Project Setup

**Files:**
- Create: `src/main/frontend/package.json`
- Create: `src/main/frontend/tsconfig.json`
- Create: `src/main/frontend/vite.config.ts`
- Create: `src/main/frontend/index.html`
- Create: `src/main/frontend/src/main.tsx`
- Create: `src/main/frontend/src/App.tsx`
- Create: `src/main/frontend/src/api/client.ts`

- [ ] **Step 1: Create package.json**

```json
{
  "name": "schedule-frontend",
  "private": true,
  "version": "1.0.0",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "tsc && vite build",
    "preview": "vite preview"
  },
  "dependencies": {
    "react": "^18.2.0",
    "react-dom": "^18.2.0",
    "react-router-dom": "^6.20.0",
    "axios": "^1.6.0",
    "@fullcalendar/core": "^6.1.10",
    "@fullcalendar/daygrid": "^6.1.10",
    "@fullcalendar/react": "^6.1.10"
  },
  "devDependencies": {
    "@types/react": "^18.2.40",
    "@types/react-dom": "^18.2.17",
    "@vitejs/plugin-react": "^4.2.0",
    "typescript": "^5.3.2",
    "vite": "^5.0.0"
  }
}
```

- [ ] **Step 2: Create tsconfig.json**

```json
{
  "compilerOptions": {
    "target": "ES2020",
    "useDefineForClassFields": true,
    "lib": ["ES2020", "DOM", "DOM.Iterable"],
    "module": "ESNext",
    "skipLibCheck": true,
    "moduleResolution": "bundler",
    "allowImportingTsExtensions": true,
    "resolveJsonModule": true,
    "isolatedModules": true,
    "noEmit": true,
    "jsx": "react-jsx",
    "strict": true,
    "noUnusedLocals": false,
    "noUnusedParameters": false,
    "noFallthroughCasesInSwitch": true
  },
  "include": ["src"]
}
```

- [ ] **Step 3: Create vite.config.ts**

```typescript
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  build: {
    outDir: '../resources/static',
    emptyOutDir: true,
  },
});
```

- [ ] **Step 4: Create index.html**

```html
<!DOCTYPE html>
<html lang="zh-CN">
  <head>
    <meta charset="UTF-8" />
    <link rel="icon" type="image/svg+xml" href="/vite.svg" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>员工排班系统</title>
  </head>
  <body>
    <div id="root"></div>
    <script type="module" src="/src/main.tsx"></script>
  </body>
</html>
```

- [ ] **Step 5: Create main.tsx**

```tsx
import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
```

- [ ] **Step 6: Create API client**

Create `src/main/frontend/src/api/client.ts`:

```typescript
import axios from 'axios';

const client = axios.create({
  baseURL: '/api',
  withCredentials: true,
  headers: { 'Content-Type': 'application/json' },
});

client.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401) {
      window.location.href = '/login';
    }
    return Promise.reject(err);
  }
);

export default client;
```

- [ ] **Step 7: Create App.tsx with router**

```tsx
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import Layout from './components/Layout';
import LoginPage from './pages/LoginPage';
import EmployeeRequirements from './pages/EmployeeRequirements';
import MySchedule from './pages/MySchedule';
import AdminRequirements from './pages/AdminRequirements';
import AdminSchedule from './pages/AdminSchedule';
import AdminGenerate from './pages/AdminGenerate';

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route element={<Layout />}>
          <Route path="/requirements" element={<EmployeeRequirements />} />
          <Route path="/schedule" element={<MySchedule />} />
          <Route path="/admin/requirements" element={<AdminRequirements />} />
          <Route path="/admin/schedule" element={<AdminSchedule />} />
          <Route path="/admin/generate" element={<AdminGenerate />} />
        </Route>
        <Route path="/" element={<Navigate to="/login" />} />
      </Routes>
    </BrowserRouter>
  );
}
```

- [ ] **Step 8: Install dependencies**

```bash
cd src/main/frontend && npm install
```

- [ ] **Step 9: Verify dev server starts**

```bash
cd src/main/frontend && npm run dev
```

Expected: Vite dev server on port 3000 (will show blank page, but proxy works)

- [ ] **Step 10: Stop dev server and commit**

```bash
cd src/main/frontend && git add package.json package-lock.json tsconfig.json vite.config.ts index.html src/main.tsx src/App.tsx src/api/client.ts
git commit -m "feat: set up React frontend with Vite, routing, and API client"
```

---

### Task 15: Layout Component

**Files:**
- Create: `src/main/frontend/src/components/Layout.tsx`

- [ ] **Step 1: Create Layout component with role-based nav**

```tsx
import { useEffect, useState } from 'react';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import client from '../api/client';

interface UserInfo {
  id: number;
  username: string;
  name: string;
  role: 'EMPLOYEE' | 'ADMIN';
}

export default function Layout() {
  const [user, setUser] = useState<UserInfo | null>(null);
  const navigate = useNavigate();
  const location = useLocation();

  useEffect(() => {
    client.get('/auth/me')
      .then((res) => setUser(res.data))
      .catch(() => navigate('/login'));
  }, []);

  const handleLogout = async () => {
    await client.post('/auth/logout');
    setUser(null);
    navigate('/login');
  };

  if (!user) return <div style={{ padding: 40, textAlign: 'center' }}>Loading...</div>;

  const isAdmin = user.role === 'ADMIN';

  return (
    <div style={{ minHeight: '100vh', background: '#f5f5f5' }}>
      <header style={{
        background: '#1e3a5f', color: '#fff', padding: '0 24px',
        display: 'flex', alignItems: 'center', justifyContent: 'space-between', height: 56
      }}>
        <div style={{ display: 'flex', gap: 24, alignItems: 'center' }}>
          <span style={{ fontSize: 18, fontWeight: 'bold' }}>排班系统</span>
          <nav style={{ display: 'flex', gap: 16 }}>
            {isAdmin ? (
              <>
                <NavLink to="/admin/requirements" current={location.pathname}>员工需求</NavLink>
                <NavLink to="/admin/schedule" current={location.pathname}>排班管理</NavLink>
                <NavLink to="/admin/generate" current={location.pathname}>生成排班</NavLink>
              </>
            ) : (
              <>
                <NavLink to="/requirements" current={location.pathname}>我的需求</NavLink>
                <NavLink to="/schedule" current={location.pathname}>排班表</NavLink>
              </>
            )}
          </nav>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <span>{user.name} ({isAdmin ? '管理员' : '员工'})</span>
          <button onClick={handleLogout}
            style={{ background: 'transparent', border: '1px solid #fff', color: '#fff',
              padding: '4px 12px', borderRadius: 4, cursor: 'pointer' }}>
            退出
          </button>
        </div>
      </header>
      <main style={{ maxWidth: 1200, margin: '24px auto', padding: '0 16px' }}>
        <Outlet />
      </main>
    </div>
  );
}

function NavLink({ to, current, children }: { to: string; current: string; children: React.ReactNode }) {
  const active = current === to;
  return (
    <a href={to} style={{
      color: active ? '#60a5fa' : '#cbd5e1',
      textDecoration: 'none',
      fontSize: 15,
      fontWeight: active ? 600 : 400,
      borderBottom: active ? '2px solid #60a5fa' : '2px solid transparent',
      paddingBottom: 2
    }}>
      {children}
    </a>
  );
}
```

- [ ] **Step 2: Verify compilation**

```bash
cd src/main/frontend && npx tsc --noEmit
```

Expected: No errors (may warn about unused variables)

- [ ] **Step 3: Commit**

```bash
git add src/main/frontend/src/components/Layout.tsx
git commit -m "feat: add layout with role-based navigation"
```

---

### Task 16: Login Page

**Files:**
- Create: `src/main/frontend/src/pages/LoginPage.tsx`

- [ ] **Step 1: Create LoginPage**

```tsx
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import client from '../api/client';

export default function LoginPage() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    try {
      const res = await client.post('/auth/login', { username, password });
      const role = res.data.role;
      navigate(role === 'ADMIN' ? '/admin/schedule' : '/schedule');
    } catch (err: any) {
      setError(err.response?.data?.error || '登录失败');
    }
  };

  return (
    <div style={{
      minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center',
      background: 'linear-gradient(135deg, #1e3a5f 0%, #3b82f6 100%)'
    }}>
      <div style={{
        background: '#fff', borderRadius: 12, padding: '40px 36px',
        width: 380, boxShadow: '0 20px 60px rgba(0,0,0,0.3)'
      }}>
        <h2 style={{ margin: '0 0 4px', fontSize: 24, textAlign: 'center' }}>员工排班系统</h2>
        <p style={{ textAlign: 'center', color: '#6b7280', marginBottom: 28, fontSize: 14 }}>
          请登录您的账号
        </p>
        {error && (
          <div style={{
            background: '#fef2f2', color: '#dc2626', padding: '10px 14px',
            borderRadius: 6, marginBottom: 16, fontSize: 14
          }}>
            {error}
          </div>
        )}
        <form onSubmit={handleSubmit}>
          <div style={{ marginBottom: 16 }}>
            <label style={{ display: 'block', marginBottom: 6, fontSize: 14, fontWeight: 500 }}>账号</label>
            <input
              type="text" value={username} onChange={(e) => setUsername(e.target.value)}
              placeholder="请输入账号"
              style={{
                width: '100%', padding: '10px 12px', border: '1px solid #d1d5db',
                borderRadius: 6, fontSize: 14, boxSizing: 'border-box'
              }}
              required
            />
          </div>
          <div style={{ marginBottom: 20 }}>
            <label style={{ display: 'block', marginBottom: 6, fontSize: 14, fontWeight: 500 }}>密码</label>
            <input
              type="password" value={password} onChange={(e) => setPassword(e.target.value)}
              placeholder="请输入密码"
              style={{
                width: '100%', padding: '10px 12px', border: '1px solid #d1d5db',
                borderRadius: 6, fontSize: 14, boxSizing: 'border-box'
              }}
              required
            />
          </div>
          <button type="submit" style={{
            width: '100%', padding: '12px', background: '#2563eb', color: '#fff',
            border: 'none', borderRadius: 6, fontSize: 15, fontWeight: 600,
            cursor: 'pointer'
          }}>
            登 录
          </button>
        </form>
        <div style={{ marginTop: 20, fontSize: 12, color: '#9ca3af', textAlign: 'center' }}>
          管理员: admin/admin123 · 员工: emp1/123456
        </div>
      </div>
    </div>
  );
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/frontend/src/pages/LoginPage.tsx
git commit -m "feat: add login page"
```

---

### Task 17: Employee Pages

**Files:**
- Create: `src/main/frontend/src/pages/EmployeeRequirements.tsx`
- Create: `src/main/frontend/src/pages/MySchedule.tsx`
- Create: `src/main/frontend/src/components/ScheduleCalendar.tsx`

- [ ] **Step 1: Create EmployeeRequirements page**

```tsx
import { useState, useEffect } from 'react';
import client from '../api/client';

export default function EmployeeRequirements() {
  const [input, setInput] = useState('');
  const [yearMonth, setYearMonth] = useState(() => {
    const now = new Date();
    now.setMonth(now.getMonth() + 1);
    return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
  });
  const [saved, setSaved] = useState<any>(null);
  const [parsing, setParsing] = useState(false);
  const [confirmResult, setConfirmResult] = useState<any>(null);
  const [message, setMessage] = useState('');

  useEffect(() => {
    loadSaved();
  }, [yearMonth]);

  const loadSaved = async () => {
    try {
      const res = await client.get(`/requirements/my?month=${yearMonth}`);
      if (res.data) setSaved(res.data);
      else setSaved(null);
    } catch { setSaved(null); }
  };

  const handleSubmit = async () => {
    if (!input.trim()) return;
    setParsing(true);
    setMessage('');
    try {
      const res = await client.post('/requirements', { naturalInput: input, yearMonth });
      setConfirmResult(res.data.parsed);
    } catch (err: any) {
      setMessage('❌ ' + (err.response?.data?.error || '解析失败'));
    } finally {
      setParsing(false);
    }
  };

  const handleConfirm = async () => {
    // Already saved by backend, just refresh
    setConfirmResult(null);
    setInput('');
    setMessage('✅ 需求已保存');
    loadSaved();
  };

  const handleCancel = () => {
    setConfirmResult(null);
  };

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
        <h2 style={{ margin: 0 }}>我的排班需求</h2>
        <select value={yearMonth} onChange={(e) => setYearMonth(e.target.value)}
          style={{ padding: '8px 12px', border: '1px solid #d1d5db', borderRadius: 6, fontSize: 14 }}>
          {generateMonths().map(m => <option key={m} value={m}>{m}</option>)}
        </select>
      </div>

      {saved && (
        <div style={{
          background: '#f0fdf4', border: '1px solid #86efac', borderRadius: 8,
          padding: '16px 20px', marginBottom: 20
        }}>
          <div style={{ fontWeight: 600, marginBottom: 4, color: '#166534' }}>📋 已提交的需求</div>
          <div style={{ fontSize: 14, color: '#374151' }}>
            {saved.naturalInput || '(无特殊要求)'}
          </div>
        </div>
      )}

      {confirmResult ? (
        <div style={{
          background: '#eff6ff', border: '1px solid #93c5fd', borderRadius: 8,
          padding: '20px', marginBottom: 20
        }}>
          <div style={{ fontWeight: 600, marginBottom: 8 }}>🤖 AI 理解了您的需求：</div>
          <div style={{ fontSize: 14, lineHeight: 1.8 }}>
            {confirmResult.unavailable_dates?.length > 0 && (
              <div>📅 不可上班日期：{confirmResult.unavailable_dates.join(', ')}</div>
            )}
            {confirmResult.prefer_morning !== undefined && (
              <div>🌅 偏好班次：{confirmResult.prefer_morning ? '早班' : '无特别偏好'}</div>
            )}
            <div style={{ color: '#6b7280', marginTop: 4 }}>备注：{confirmResult.notes || '已理解您的需求'}</div>
          </div>
          <div style={{ marginTop: 14, display: 'flex', gap: 10 }}>
            <button onClick={handleConfirm} style={btnPrimary}>确认保存</button>
            <button onClick={handleCancel} style={btnSecondary}>重新输入</button>
          </div>
        </div>
      ) : (
        <div style={{ marginBottom: 20 }}>
          <textarea
            value={input}
            onChange={(e) => setInput(e.target.value)}
            placeholder="描述你的下月排班需求，比如：&#10;我2月3号有事情不能上班，更希望上白班"
            rows={4}
            style={{
              width: '100%', padding: '12px', border: '1px solid #d1d5db',
              borderRadius: 8, fontSize: 14, resize: 'vertical', boxSizing: 'border-box'
            }}
          />
          <button onClick={handleSubmit} disabled={parsing || !input.trim()} style={{
            ...btnPrimary, marginTop: 12, opacity: parsing ? 0.7 : 1
          }}>
            {parsing ? 'AI 正在理解...' : '提交并让 AI 理解'}
          </button>
        </div>
      )}

      {message && (
        <div style={{ padding: '12px 16px', background: message.startsWith('✅') ? '#f0fdf4' : '#fef2f2',
          color: message.startsWith('✅') ? '#166534' : '#dc2626', borderRadius: 6, fontSize: 14 }}>
          {message}
        </div>
      )}
    </div>
  );
}

const btnPrimary: React.CSSProperties = {
  padding: '10px 20px', background: '#2563eb', color: '#fff',
  border: 'none', borderRadius: 6, fontSize: 14, fontWeight: 600, cursor: 'pointer'
};

const btnSecondary: React.CSSProperties = {
  padding: '10px 20px', background: '#fff', color: '#374151',
  border: '1px solid #d1d5db', borderRadius: 6, fontSize: 14, cursor: 'pointer'
};

function generateMonths(): string[] {
  const months: string[] = [];
  const now = new Date();
  for (let i = 0; i < 6; i++) {
    const d = new Date(now.getFullYear(), now.getMonth() + i, 1);
    months.push(`${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`);
  }
  return months;
}
```

- [ ] **Step 2: Create ScheduleCalendar component**

```tsx
import FullCalendar from '@fullcalendar/react';
import dayGridPlugin from '@fullcalendar/daygrid';

interface ScheduleEvent {
  title: string;
  start: string;
  backgroundColor: string;
  textColor: string;
}

interface Props {
  events: ScheduleEvent[];
}

export default function ScheduleCalendar({ events }: Props) {
  return (
    <FullCalendar
      plugins={[dayGridPlugin]}
      initialView="dayGridMonth"
      locale="zh-cn"
      height="auto"
      events={events}
      headerToolbar={{
        left: 'prev,next today',
        center: 'title',
        right: 'dayGridMonth'
      }}
      buttonText={{ today: '今天', month: '月' }}
      dayCellContent={(arg) => arg.dayNumberText.replace('日', '')}
    />
  );
}
```

- [ ] **Step 3: Create MySchedule page**

```tsx
import { useState, useEffect } from 'react';
import client from '../api/client';
import ScheduleCalendar from '../components/ScheduleCalendar';

export default function MySchedule() {
  const [yearMonth, setYearMonth] = useState(() => {
    const now = new Date();
    return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
  });
  const [events, setEvents] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [empty, setEmpty] = useState(false);

  useEffect(() => {
    setLoading(true);
    client.get(`/schedule?month=${yearMonth}`)
      .then((res) => {
        if (!res.data || res.data.length === 0) {
          setEmpty(true);
          setEvents([]);
        } else {
          setEmpty(false);
          setEvents(res.data.map((s: any) => ({
            title: `${s.userName} ${shiftLabel(s.shift)}`,
            start: s.date,
            backgroundColor: s.shift === 'MORNING' ? '#22c55e' : s.shift === 'EVENING' ? '#3b82f6' : '#d1d5db',
            textColor: '#fff'
          })));
        }
      })
      .catch(() => setEmpty(true))
      .finally(() => setLoading(false));
  }, [yearMonth]);

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
        <h2 style={{ margin: 0 }}>排班表</h2>
        <select value={yearMonth} onChange={(e) => setYearMonth(e.target.value)}
          style={{ padding: '8px 12px', border: '1px solid #d1d5db', borderRadius: 6, fontSize: 14 }}>
          {generateMonths().map(m => <option key={m} value={m}>{m}</option>)}
        </select>
      </div>

      {loading ? (
        <div style={{ textAlign: 'center', padding: 40, color: '#6b7280' }}>加载中...</div>
      ) : empty ? (
        <div style={{
          textAlign: 'center', padding: 80, color: '#9ca3af',
          background: '#fff', borderRadius: 8, border: '1px solid #e5e7eb'
        }}>
          <div style={{ fontSize: 48, marginBottom: 12 }}>📅</div>
          <div style={{ fontSize: 16 }}>排班尚未发布</div>
          <div style={{ fontSize: 13, marginTop: 4 }}>管理员发布排班后将在此显示</div>
        </div>
      ) : (
        <div style={{ background: '#fff', padding: 20, borderRadius: 8, border: '1px solid #e5e7eb' }}>
          <ScheduleCalendar events={events} />
        </div>
      )}

      <div style={{ marginTop: 16, display: 'flex', gap: 16, fontSize: 13, color: '#6b7280' }}>
        <span>🟢 早班</span>
        <span>🔵 晚班</span>
        <span>⚪ 休息</span>
      </div>
    </div>
  );
}

function shiftLabel(shift: string): string {
  switch (shift) {
    case 'MORNING': return '早';
    case 'EVENING': return '晚';
    default: return '休';
  }
}

function generateMonths(): string[] {
  const months: string[] = [];
  const now = new Date();
  for (let i = -1; i < 6; i++) {
    const d = new Date(now.getFullYear(), now.getMonth() + i, 1);
    months.push(`${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`);
  }
  return months;
}
```

- [ ] **Step 4: Commit**

```bash
git add src/main/frontend/src/pages/EmployeeRequirements.tsx src/main/frontend/src/pages/MySchedule.tsx src/main/frontend/src/components/ScheduleCalendar.tsx
git commit -m "feat: add employee pages - requirements input and schedule view"
```

---

### Task 18: Admin Pages

**Files:**
- Create: `src/main/frontend/src/pages/AdminRequirements.tsx`
- Create: `src/main/frontend/src/pages/AdminGenerate.tsx`
- Create: `src/main/frontend/src/pages/AdminSchedule.tsx`
- [ ] **Step 1: Create AdminRequirements page**

```tsx
import { useState, useEffect } from 'react';
import client from '../api/client';

export default function AdminRequirements() {
  const [yearMonth, setYearMonth] = useState(() => {
    const now = new Date();
    now.setMonth(now.getMonth() + 1);
    return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
  });
  const [requirements, setRequirements] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    client.get(`/admin/requirements?month=${yearMonth}`)
      .then((res) => setRequirements(res.data || []))
      .catch(() => setRequirements([]))
      .finally(() => setLoading(false));
  }, [yearMonth]);

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
        <h2 style={{ margin: 0 }}>员工排班需求汇总</h2>
        <select value={yearMonth} onChange={(e) => setYearMonth(e.target.value)}
          style={{ padding: '8px 12px', border: '1px solid #d1d5db', borderRadius: 6, fontSize: 14 }}>
          {generateMonths().map(m => <option key={m} value={m}>{m}</option>)}
        </select>
      </div>

      {loading ? (
        <div style={{ textAlign: 'center', padding: 40 }}>加载中...</div>
      ) : requirements.length === 0 ? (
        <div style={{ textAlign: 'center', padding: 60, color: '#9ca3af', background: '#fff', borderRadius: 8 }}>
          暂无员工提交需求
        </div>
      ) : (
        <div style={{ background: '#fff', borderRadius: 8, border: '1px solid #e5e7eb', overflow: 'hidden' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse' }}>
            <thead>
              <tr style={{ background: '#f9fafb', borderBottom: '1px solid #e5e7eb' }}>
                <th style={th}>员工姓名</th>
                <th style={th}>不可上班日期</th>
                <th style={th}>偏好</th>
                <th style={th}>原始输入</th>
              </tr>
            </thead>
            <tbody>
              {requirements.map((req: any) => {
                let unavailable: string[] = [];
                let preferMorning = false;
                try {
                  unavailable = JSON.parse(req.parsedUnavailable || '[]');
                  const prefs = JSON.parse(req.parsedPreferences || '{}');
                  preferMorning = prefs.prefer_morning;
                } catch {}
                return (
                  <tr key={req.id} style={{ borderBottom: '1px solid #f3f4f6' }}>
                    <td style={td}>{req.userName}</td>
                    <td style={td}>{unavailable.length > 0 ? unavailable.join(', ') : '无'}</td>
                    <td style={td}>{preferMorning ? '偏好早班' : '无特别偏好'}</td>
                    <td style={td}>{req.naturalInput || '(空)'}</td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

const th: React.CSSProperties = { textAlign: 'left', padding: '12px 16px', fontSize: 14, fontWeight: 600, color: '#374151' };
const td: React.CSSProperties = { padding: '10px 16px', fontSize: 14, color: '#4b5563' };

function generateMonths(): string[] {
  const months: string[] = [];
  const now = new Date();
  for (let i = 0; i < 6; i++) {
    const d = new Date(now.getFullYear(), now.getMonth() + i, 1);
    months.push(`${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`);
  }
  return months;
}
```

- [ ] **Step 2: Create AdminGenerate page**

```tsx
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import client from '../api/client';

export default function AdminGenerate() {
  const [yearMonth, setYearMonth] = useState(() => {
    const now = new Date();
    now.setMonth(now.getMonth() + 1);
    return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
  });
  const [generating, setGenerating] = useState(false);
  const [result, setResult] = useState<any>(null);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const handleGenerate = async () => {
    setGenerating(true);
    setError('');
    setResult(null);
    try {
      const res = await client.post('/admin/schedule/generate', { yearMonth });
      setResult(res.data);
    } catch (err: any) {
      setError(err.response?.data?.error || '生成失败');
    } finally {
      setGenerating(false);
    }
  };

  return (
    <div>
      <h2 style={{ marginBottom: 20 }}>AI 生成排班</h2>

      <div style={{
        background: '#fff', borderRadius: 8, border: '1px solid #e5e7eb',
        padding: '32px', textAlign: 'center'
      }}>
        <div style={{ marginBottom: 20 }}>
          <label style={{ marginRight: 12, fontWeight: 500 }}>选择月份：</label>
          <select value={yearMonth} onChange={(e) => setYearMonth(e.target.value)}
            style={{ padding: '8px 12px', border: '1px solid #d1d5db', borderRadius: 6, fontSize: 14 }}>
            {generateMonths().map(m => <option key={m} value={m}>{m}</option>)}
          </select>
        </div>

        <button onClick={handleGenerate} disabled={generating} style={{
          padding: '14px 40px', background: generating ? '#9ca3af' : '#2563eb',
          color: '#fff', border: 'none', borderRadius: 8, fontSize: 16,
          fontWeight: 600, cursor: generating ? 'not-allowed' : 'pointer'
        }}>
          {generating ? '🤖 AI 正在生成排班...' : '🚀 开始生成排班'}
        </button>

        {error && (
          <div style={{
            marginTop: 20, padding: '16px', background: '#fef2f2',
            color: '#dc2626', borderRadius: 8, textAlign: 'left'
          }}>
            <strong>生成失败：</strong>{error}
          </div>
        )}

        {result && (
          <div style={{
            marginTop: 20, padding: '20px', background: '#f0fdf4',
            borderRadius: 8, textAlign: 'left'
          }}>
            <div style={{ fontWeight: 600, color: '#166534', marginBottom: 12, fontSize: 16 }}>
              ✅ 排班生成成功！
            </div>
            <div style={{ color: '#374151', fontSize: 14, marginBottom: 16 }}>
              已生成 {result.length} 条排班记录（草稿状态），请到排班管理页查看和调整。
            </div>
            <button onClick={() => navigate('/admin/schedule')} style={{
              padding: '10px 24px', background: '#16a34a', color: '#fff',
              border: 'none', borderRadius: 6, fontSize: 14, fontWeight: 600, cursor: 'pointer'
            }}>
              前往排班管理 →
            </button>
          </div>
        )}
      </div>
    </div>
  );
}

function generateMonths(): string[] {
  const months: string[] = [];
  const now = new Date();
  for (let i = 0; i < 6; i++) {
    const d = new Date(now.getFullYear(), now.getMonth() + i, 1);
    months.push(`${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`);
  }
  return months;
}
```

- [ ] **Step 3: Create AdminSchedule page with ScheduleTable**

```tsx
import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import client from '../api/client';

export default function AdminSchedule() {
  const [yearMonth, setYearMonth] = useState(() => {
    const now = new Date();
    return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
  });
  const [schedules, setSchedules] = useState<any[]>([]);
  const [stats, setStats] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [published, setPublished] = useState(false);
  const [message, setMessage] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    loadData();
  }, [yearMonth]);

  const loadData = async () => {
    setLoading(true);
    try {
      const [scheduleRes, statsRes] = await Promise.all([
        client.get(`/admin/schedule?month=${yearMonth}`),
        client.get(`/admin/schedule/stats?month=${yearMonth}`)
      ]);
      setSchedules(scheduleRes.data || []);
      setStats(statsRes.data || []);
      setPublished((scheduleRes.data || []).some((s: any) => s.status === 'PUBLISHED'));
    } catch {
      setSchedules([]);
      setStats([]);
    } finally {
      setLoading(false);
    }
  };

  const handleShiftChange = async (id: number, newShift: string) => {
    try {
      await client.put(`/admin/schedule/${id}`, { shift: newShift });
      setSchedules((prev) => prev.map((s) => s.id === id ? { ...s, shift: newShift } : s));
      setMessage('');
    } catch (err: any) {
      setMessage('❌ ' + (err.response?.data?.error || '修改失败'));
    }
  };

  const handlePublish = async () => {
    try {
      await client.post('/admin/schedule/publish', { yearMonth });
      setPublished(true);
      setMessage('✅ 排班已发布，员工现在可以查看');
    } catch (err: any) {
      setMessage('❌ ' + (err.response?.data?.error || '发布失败'));
    }
  };

  const handleUnpublish = async () => {
    try {
      await client.post('/admin/schedule/unpublish', { yearMonth });
      setPublished(false);
      setMessage('✅ 已取消发布，可以重新编辑');
    } catch (err: any) {
      setMessage('❌ ' + (err.response?.data?.error || '取消发布失败'));
    }
  };

  // Build grid: rows=employees, cols=days
  const grouped = groupByEmployee(schedules);
  const days = getDaysInMonth(yearMonth);
  const employees = [...new Set(schedules.map((s) => s.userName))];

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <h2 style={{ margin: 0 }}>排班管理</h2>
        <div style={{ display: 'flex', gap: 10, alignItems: 'center' }}>
          <select value={yearMonth} onChange={(e) => setYearMonth(e.target.value)}
            style={{ padding: '8px 12px', border: '1px solid #d1d5db', borderRadius: 6, fontSize: 14 }}>
            {generateMonths().map(m => <option key={m} value={m}>{m}</option>)}
          </select>
          <button onClick={() => navigate('/admin/generate')} style={{
            padding: '8px 16px', background: '#2563eb', color: '#fff',
            border: 'none', borderRadius: 6, fontSize: 14, cursor: 'pointer'
          }}>
            🤖 生成排班
          </button>
          {!published ? (
            <button onClick={handlePublish} disabled={schedules.length === 0} style={{
              padding: '8px 16px', background: schedules.length > 0 ? '#16a34a' : '#d1d5db',
              color: '#fff', border: 'none', borderRadius: 6, fontSize: 14,
              cursor: schedules.length > 0 ? 'pointer' : 'not-allowed'
            }}>
              📢 发布
            </button>
          ) : (
            <button onClick={handleUnpublish} style={{
              padding: '8px 16px', background: '#f59e0b', color: '#fff',
              border: 'none', borderRadius: 6, fontSize: 14, cursor: 'pointer'
            }}>
              ↩ 取消发布
            </button>
          )}
        </div>
      </div>

      {message && (
        <div style={{
          padding: '10px 16px', marginBottom: 16, borderRadius: 6,
          background: message.startsWith('✅') ? '#f0fdf4' : '#fef2f2',
          color: message.startsWith('✅') ? '#166534' : '#dc2626', fontSize: 14
        }}>
          {message}
        </div>
      )}

      {loading ? (
        <div style={{ textAlign: 'center', padding: 40 }}>加载中...</div>
      ) : schedules.length === 0 ? (
        <div style={{ textAlign: 'center', padding: 60, color: '#9ca3af', background: '#fff', borderRadius: 8 }}>
          暂无排班数据，请先生成排班
        </div>
      ) : (
        <>
          {/* Stats bar */}
          <div style={{
            display: 'flex', gap: 12, marginBottom: 16, flexWrap: 'wrap'
          }}>
            {stats.map((s: any) => (
              <div key={s.userId} style={{
                background: '#fff', borderRadius: 6, padding: '10px 16px',
                border: '1px solid #e5e7eb', fontSize: 13, minWidth: 140
              }}>
                <strong>{s.userName}</strong>
                <div style={{ color: '#6b7280', marginTop: 2 }}>
                  早{s.morningDays} 晚{s.eveningDays} 休{s.offDays} · 共{s.totalDays}天
                </div>
              </div>
            ))}
          </div>

          {/* Schedule grid */}
          <div style={{ background: '#fff', borderRadius: 8, border: '1px solid #e5e7eb', overflow: 'auto' }}>
            <table style={{ borderCollapse: 'collapse', minWidth: 800 }}>
              <thead>
                <tr style={{ background: '#f9fafb' }}>
                  <th style={th}>员工</th>
                  {days.map((d) => (
                    <th key={d} style={{ ...th, textAlign: 'center', minWidth: 50 }}>{d}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {employees.map((name) => {
                  const empSchedules = grouped[name] || [];
                  const scheduleMap: Record<string, string> = {};
                  empSchedules.forEach((s: any) => { scheduleMap[s.date] = s.shift; });
                  return (
                    <tr key={name} style={{ borderBottom: '1px solid #f3f4f6' }}>
                      <td style={td}>{name}</td>
                      {days.map((d) => {
                        const date = `${yearMonth}-${String(d).padStart(2, '0')}`;
                        const shift = scheduleMap[date] || 'OFF';
                        const entry = empSchedules.find((s: any) => s.date === date);
                        return (
                          <td key={d} style={{ ...td, textAlign: 'center', padding: '2px' }}>
                            <select
                              value={shift}
                              disabled={published}
                              onChange={(e) => entry && handleShiftChange(entry.id, e.target.value)}
                              style={{
                                padding: '4px', fontSize: 12, borderRadius: 4,
                                border: '1px solid #d1d5db',
                                background: shift === 'MORNING' ? '#dcfce7' : shift === 'EVENING' ? '#dbeafe' : '#f3f4f6',
                                color: shift === 'MORNING' ? '#166534' : shift === 'EVENING' ? '#1e40af' : '#6b7280',
                                cursor: published ? 'default' : 'pointer'
                              }}
                            >
                              <option value="MORNING">早</option>
                              <option value="EVENING">晚</option>
                              <option value="OFF">休</option>
                            </select>
                          </td>
                        );
                      })}
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </>
      )}
    </div>
  );
}

const th: React.CSSProperties = { textAlign: 'left', padding: '8px 10px', fontSize: 13, fontWeight: 600, color: '#374151', whiteSpace: 'nowrap' };
const td: React.CSSProperties = { padding: '6px 10px', fontSize: 13, color: '#4b5563' };

function groupByEmployee(schedules: any[]): Record<string, any[]> {
  const map: Record<string, any[]> = {};
  for (const s of schedules) {
    if (!map[s.userName]) map[s.userName] = [];
    map[s.userName].push(s);
  }
  return map;
}

function getDaysInMonth(yearMonth: string): number[] {
  const [y, m] = yearMonth.split('-').map(Number);
  const days = new Date(y, m, 0).getDate();
  return Array.from({ length: days }, (_, i) => i + 1);
}

function generateMonths(): string[] {
  const months: string[] = [];
  const now = new Date();
  for (let i = -1; i < 6; i++) {
    const d = new Date(now.getFullYear(), now.getMonth() + i, 1);
    months.push(`${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`);
  }
  return months;
}
```

- [ ] **Step 4: Verify TypeScript compilation**

```bash
cd src/main/frontend && npx tsc --noEmit
```

Expected: No errors

- [ ] **Step 5: Commit**

```bash
git add src/main/frontend/src/pages/AdminRequirements.tsx src/main/frontend/src/pages/AdminGenerate.tsx src/main/frontend/src/pages/AdminSchedule.tsx
git commit -m "feat: add admin pages - requirements, schedule management, generation"
```

---

### Task 19: Frontend Build & Backend Integration

**Files:**
- Modify: `pom.xml` — add frontend-maven-plugin
- Modify: `src/main/frontend/src/api/client.ts` — handle production API path

- [ ] **Step 1: Add frontend-maven-plugin to pom.xml**

In `pom.xml`, inside `<build><plugins>` block (after `spring-boot-maven-plugin`), add:

```xml
<plugin>
    <groupId>com.github.eirslett</groupId>
    <artifactId>frontend-maven-plugin</artifactId>
    <version>1.15.0</version>
    <configuration>
        <workingDirectory>src/main/frontend</workingDirectory>
        <installDirectory>${project.build.directory}</installDirectory>
    </configuration>
    <executions>
        <execution>
            <id>install node and npm</id>
            <goals><goal>install-node-and-npm</goal></goals>
            <phase>generate-resources</phase>
            <configuration>
                <nodeVersion>v18.18.0</nodeVersion>
            </configuration>
        </execution>
        <execution>
            <id>npm install</id>
            <goals><goal>npm</goal></goals>
            <phase>generate-resources</phase>
            <configuration>
                <arguments>install</arguments>
            </configuration>
        </execution>
        <execution>
            <id>npm build</id>
            <goals><goal>npm</goal></goals>
            <phase>generate-resources</phase>
            <configuration>
                <arguments>run build</arguments>
            </configuration>
        </execution>
    </executions>
</plugin>
```

- [ ] **Step 2: Update client.ts for production base URL**

Edit `src/main/frontend/src/api/client.ts`, change `baseURL`:

```typescript
const client = axios.create({
  baseURL: import.meta.env.DEV ? '/api' : '/api',
  withCredentials: true,
  headers: { 'Content-Type': 'application/json' },
});
```

- [ ] **Step 3: Build frontend separately to verify**

```bash
cd src/main/frontend && npm run build
```

Expected: Build completes, files in `src/main/resources/static/`

- [ ] **Step 4: Full Maven build**

```bash
mvn clean package -DskipTests
```

Expected: BUILD SUCCESS, jar in `target/schedule-app-1.0.0.jar`

- [ ] **Step 5: Commit**

```bash
git add pom.xml src/main/frontend/src/api/client.ts
git commit -m "feat: add frontend-maven-plugin for integrated build"
```

---

### Task 20: README & Final Integration Test

**Files:**
- Create: `README.md`

- [ ] **Step 1: Create README**

````markdown
# 员工智能排班系统

基于 LLM（DeepSeek）的智能排班系统，支持员工自然语言输入需求、AI 一键生成排班、管理员手动调整发布。

## 技术栈

- Java 17 + Spring Boot 3
- React 18 + TypeScript + FullCalendar
- H2 内嵌数据库
- DeepSeek Chat API

## 快速启动

### 1. 配置 DeepSeek API Key

```bash
export DEEPSEEK_API_KEY=your-api-key
```

Windows:
```cmd
set DEEPSEEK_API_KEY=your-api-key
```

### 2. 启动应用

```bash
java -jar target/schedule-app-1.0.0.jar
```

或开发模式：
```bash
mvn spring-boot:run
```

### 3. 打开浏览器

http://localhost:8080

### 预置账号

| 角色 | 账号 | 密码 |
|------|------|------|
| 管理员 | admin | admin123 |
| 员工1 | emp1 | 123456 |
| 员工2 | emp2 | 123456 |
| 员工3 | emp3 | 123456 |
| 员工4 | emp4 | 123456 |
| 员工5 | emp5 | 123456 |

## 使用流程

1. **员工登录** → 输入下月排班需求（自然语言）→ AI 解析确认 → 保存
2. **管理员登录** → 查看员工需求 → 点击生成排班 → AI 自动排班
3. **管理员** → 在表格中手动调整 → 点击发布
4. **员工** → 查看已发布排班（日历视图）

## 开发

```bash
# 后端
mvn spring-boot:run

# 前端（开发模式，独立运行）
cd src/main/frontend
npm run dev
# 前端 dev server 运行在 http://localhost:3000，自动代理 API 到 8080
```
````

- [ ] **Step 2: Build the final jar**

```bash
cd src/main/frontend && npm run build && cd ../../.. && mvn clean package -DskipTests
```

- [ ] **Step 3: Start the application and smoke test**

```bash
java -jar target/schedule-app-1.0.0.jar
```

Wait for startup, then open http://localhost:8080 in browser.

Manual check:
1. Login as admin/admin123 → should see admin dashboard
2. Login as emp1/123456 → should see employee dashboard
3. As emp1, submit a requirement → should get AI parsed response
4. As admin, go to admin schedule → generate schedule → adjust → publish

- [ ] **Step 4: Commit and push**

```bash
git add README.md
git commit -m "docs: add README with setup and usage instructions"
git push origin main
```

---

## Verification Checklist

After completing all tasks, verify:

- [ ] `java -jar target/schedule-app-1.0.0.jar` starts without errors
- [ ] http://localhost:8080 opens the login page
- [ ] Admin (admin/admin123) can login and see all admin pages
- [ ] Employee (emp1/123456) can login and see employee pages
- [ ] Employee can submit natural language requirement and see AI parsing result
- [ ] Admin can view all employee requirements
- [ ] Admin can generate schedule (requires DEEPSEEK_API_KEY)
- [ ] Generated schedule respects all 5 hard constraints
- [ ] Admin can manually adjust a schedule entry
- [ ] Admin can publish, then employee can view published schedule in calendar
- [ ] Admin can unpublish and re-edit
