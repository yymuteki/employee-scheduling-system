package com.schedule.controller;

import com.schedule.dto.GenerateRequest;
import com.schedule.entity.Schedule;
import com.schedule.entity.ShiftRequirement;
import com.schedule.entity.User;
import com.schedule.service.RequirementService;
import com.schedule.service.ScheduleService;
import jakarta.servlet.http.HttpSession;
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
    public ResponseEntity<?> getRequirements(@RequestParam String yearMonth, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || user.getRole() != User.Role.ADMIN) {
            return ResponseEntity.status(403).body(Map.of("error", "权限不足"));
        }
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
            result.add(entry);
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generateSchedule(@RequestBody GenerateRequest request, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || user.getRole() != User.Role.ADMIN) {
            return ResponseEntity.status(403).body(Map.of("error", "权限不足"));
        }
        try {
            List<Schedule> schedules = scheduleService.generate(request.getYearMonth());
            return ResponseEntity.ok(Map.of("message", "排班生成成功", "count", schedules.size()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "生成失败: " + e.getMessage()));
        }
    }

    @PutMapping("/schedule/{id}")
    public ResponseEntity<?> updateShift(@PathVariable Long id, @RequestBody Map<String, String> body, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || user.getRole() != User.Role.ADMIN) {
            return ResponseEntity.status(403).body(Map.of("error", "权限不足"));
        }
        try {
            Schedule updated = scheduleService.updateShift(id, body.get("shift"));
            return ResponseEntity.ok(Map.of("message", "修改成功", "id", updated.getId()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/publish")
    public ResponseEntity<?> publish(@RequestBody Map<String, String> body, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || user.getRole() != User.Role.ADMIN) {
            return ResponseEntity.status(403).body(Map.of("error", "权限不足"));
        }
        String yearMonth = body.get("yearMonth");
        scheduleService.publish(yearMonth);
        return ResponseEntity.ok(Map.of("message", "排班已发布"));
    }

    @PostMapping("/unpublish")
    public ResponseEntity<?> unpublish(@RequestBody Map<String, String> body, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || user.getRole() != User.Role.ADMIN) {
            return ResponseEntity.status(403).body(Map.of("error", "权限不足"));
        }
        String yearMonth = body.get("yearMonth");
        scheduleService.unpublish(yearMonth);
        return ResponseEntity.ok(Map.of("message", "已取消发布"));
    }
}
