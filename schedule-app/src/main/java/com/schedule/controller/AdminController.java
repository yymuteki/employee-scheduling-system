package com.schedule.controller;

import com.schedule.dto.GenerateRequest;
import com.schedule.entity.AuditLog;
import com.schedule.entity.Schedule;
import com.schedule.entity.ShiftRequirement;
import com.schedule.repository.AuditLogRepository;
import com.schedule.service.RequirementService;
import com.schedule.service.ScheduleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final RequirementService requirementService;
    private final ScheduleService scheduleService;
    private final AuditLogRepository auditLogRepository;

    public AdminController(RequirementService requirementService, ScheduleService scheduleService,
                           AuditLogRepository auditLogRepository) {
        this.requirementService = requirementService;
        this.scheduleService = scheduleService;
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping("/requirements")
    public ResponseEntity<?> getRequirements(@RequestParam String yearMonth) {
        List<ShiftRequirement> reqs = requirementService.getByMonth(yearMonth);
        List<Map<String, Object>> result = new ArrayList<>();
        for (ShiftRequirement req : reqs) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("id", req.getId());
            entry.put("userId", req.getUser().getId());
            entry.put("userName", req.getUser().getName());
            entry.put("yearMonth", req.getYearMonth());
            entry.put("naturalInput", req.getNaturalInput());
            entry.put("parsedUnavailable", req.getParsedUnavailable());
            entry.put("parsedPreferences", req.getParsedPreferences());
            entry.put("parsedNotes", req.getParsedNotes());
            result.add(entry);
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generateSchedule(@RequestBody GenerateRequest request) {
        try {
            List<Schedule> schedules = scheduleService.generate(request.getYearMonth());
            return ResponseEntity.ok(Map.of("message", "排班生成成功", "count", schedules.size()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "生成失败: " + e.getMessage()));
        }
    }

    @PutMapping("/schedule/{id}")
    public ResponseEntity<?> updateShift(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            Schedule updated = scheduleService.updateShift(id, body.get("shift"));
            return ResponseEntity.ok(Map.of("message", "修改成功", "id", updated.getId()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/schedule/assign")
    public ResponseEntity<?> assignShift(@RequestBody Map<String, String> body) {
        try {
            String yearMonth = body.get("yearMonth");
            LocalDate date = LocalDate.parse(body.get("date"));
            Schedule.Shift shiftType = Schedule.Shift.valueOf(body.get("shift"));
            String userName = body.get("userName");
            scheduleService.assignShift(yearMonth, date, shiftType, userName);
            return ResponseEntity.ok(Map.of("message", "排班已更新"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/publish")
    public ResponseEntity<?> publish(@RequestBody Map<String, String> body) {
        String yearMonth = body.get("yearMonth");
        scheduleService.publish(yearMonth);
        return ResponseEntity.ok(Map.of("message", "排班已发布"));
    }

    @PostMapping("/unpublish")
    public ResponseEntity<?> unpublish(@RequestBody Map<String, String> body) {
        String yearMonth = body.get("yearMonth");
        scheduleService.unpublish(yearMonth);
        return ResponseEntity.ok(Map.of("message", "已取消发布"));
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<?> getAuditLogs(@RequestParam(required = false) String entityName) {
        List<AuditLog> logs;
        if (entityName != null && !entityName.isEmpty()) {
            logs = auditLogRepository.findByEntityNameOrderByTimestampDesc(entityName);
        } else {
            logs = auditLogRepository.findAllByOrderByTimestampDesc();
        }
        return ResponseEntity.ok(Map.of("data", logs));
    }
}
