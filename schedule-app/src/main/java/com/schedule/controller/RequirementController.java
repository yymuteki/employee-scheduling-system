package com.schedule.controller;

import com.schedule.dto.RequirementRequest;
import com.schedule.dto.RequirementResponse;
import com.schedule.entity.ShiftRequirement;
import com.schedule.entity.User;
import com.schedule.service.RequirementService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/requirements")
public class RequirementController {

    private final RequirementService requirementService;

    public RequirementController(RequirementService requirementService) {
        this.requirementService = requirementService;
    }

    @PostMapping
    public ResponseEntity<?> save(@Valid @RequestBody RequirementRequest request) {
        User user = getCurrentUser();
        ShiftRequirement req = requirementService.save(user, request.getYearMonth(), request.getNaturalInput());
        return ResponseEntity.ok(Map.of("message", "需求已提交", "id", req.getId()));
    }

    @GetMapping
    public ResponseEntity<?> get(@RequestParam String yearMonth) {
        User user = getCurrentUser();
        ShiftRequirement req = requirementService.getByUserAndMonth(user.getId(), yearMonth);
        if (req == null) {
            return ResponseEntity.ok(Map.of("found", false));
        }
        RequirementResponse resp = new RequirementResponse();
        resp.setId(req.getId());
        resp.setYearMonth(req.getYearMonth());
        resp.setNaturalInput(req.getNaturalInput());
        resp.setParsedUnavailable(req.getParsedUnavailable());
        resp.setParsedPreferences(req.getParsedPreferences());
        resp.setUserName(user.getName());
        return ResponseEntity.ok(Map.of("found", true, "data", resp));
    }

    private User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User user) {
            return user;
        }
        throw new RuntimeException("用户未登录");
    }
}
