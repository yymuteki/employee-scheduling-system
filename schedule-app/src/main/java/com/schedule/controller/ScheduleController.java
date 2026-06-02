package com.schedule.controller;

import com.schedule.dto.ScheduleResponse;
import com.schedule.entity.Schedule;
import com.schedule.entity.User;
import com.schedule.service.ScheduleService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/schedule")
public class ScheduleController {

    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @GetMapping
    public ResponseEntity<?> getMySchedule(@RequestParam String yearMonth) {
        User user = getCurrentUser();
        boolean published = scheduleService.isPublished(yearMonth);
        if (!published) {
            return ResponseEntity.ok(Map.of("published", false));
        }
        List<Schedule> schedules = scheduleService.getByUserAndMonth(user.getId(), yearMonth);
        List<ScheduleResponse> list = new ArrayList<>();
        for (Schedule s : schedules) {
            ScheduleResponse r = new ScheduleResponse();
            r.setId(s.getId());
            r.setUserId(s.getUser().getId());
            r.setUserName(s.getUser().getName());
            r.setDate(s.getDate().toString());
            r.setShift(s.getShift().name());
            r.setYearMonth(s.getYearMonth());
            list.add(r);
        }
        return ResponseEntity.ok(Map.of("published", true, "myUserId", user.getId(), "data", list));
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAll(@RequestParam String yearMonth) {
        List<Schedule> schedules = scheduleService.getByMonth(yearMonth);
        List<ScheduleResponse> list = new ArrayList<>();
        for (Schedule s : schedules) {
            ScheduleResponse r = new ScheduleResponse();
            r.setId(s.getId());
            r.setUserId(s.getUser().getId());
            r.setUserName(s.getUser().getName());
            r.setDate(s.getDate().toString());
            r.setShift(s.getShift().name());
            r.setYearMonth(s.getYearMonth());
            list.add(r);
        }
        boolean published = scheduleService.isPublished(yearMonth);
        Map<String, Object> stats = scheduleService.getStats(yearMonth);
        return ResponseEntity.ok(Map.of("published", published, "data", list, "stats", stats));
    }

    private User getCurrentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
