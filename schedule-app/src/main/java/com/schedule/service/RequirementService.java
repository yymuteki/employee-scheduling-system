package com.schedule.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schedule.entity.AuditLog;
import com.schedule.entity.ShiftRequirement;
import com.schedule.entity.User;
import com.schedule.repository.ShiftRequirementRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class RequirementService {

    private final ShiftRequirementRepository repository;
    private final LLMService llmService;
    private final AuditLogService auditLogService;
    private final ObjectMapper mapper = new ObjectMapper();

    public RequirementService(ShiftRequirementRepository repository, LLMService llmService,
                              AuditLogService auditLogService) {
        this.repository = repository;
        this.llmService = llmService;
        this.auditLogService = auditLogService;
    }

    public ShiftRequirement save(User user, String yearMonth, String naturalInput) {
        Map<String, Object> parsed = llmService.parseRequirement(naturalInput, yearMonth);

        ShiftRequirement existing = repository.findByUserIdAndYearMonth(user.getId(), yearMonth).orElse(null);
        ShiftRequirement req = existing != null ? existing : new ShiftRequirement();

        req.setUser(user);
        req.setYearMonth(yearMonth);
        req.setNaturalInput(naturalInput);
        try {
            req.setParsedUnavailable(mapper.writeValueAsString(parsed.get("unavailableDates")));
            req.setParsedPreferences(mapper.writeValueAsString(Map.of("preference", parsed.getOrDefault("preference", ""))));
        } catch (Exception e) {
            req.setParsedUnavailable("[]");
            req.setParsedPreferences("{}");
        }
        req.setParsedNotes((String) parsed.getOrDefault("notes", ""));

        ShiftRequirement saved = repository.save(req);
        auditLogService.log(AuditLog.Action.SUBMIT, "Requirement", saved.getId(),
                Map.of("yearMonth", yearMonth,
                       "userId", user.getId(),
                       "naturalInput", naturalInput,
                       "parsedUnavailable", saved.getParsedUnavailable(),
                       "parsedPreferences", saved.getParsedPreferences()));
        return saved;
    }

    public ShiftRequirement getByUserAndMonth(Long userId, String yearMonth) {
        return repository.findByUserIdAndYearMonth(userId, yearMonth).orElse(null);
    }

    public List<ShiftRequirement> getByMonth(String yearMonth) {
        return repository.findByYearMonth(yearMonth);
    }
}
