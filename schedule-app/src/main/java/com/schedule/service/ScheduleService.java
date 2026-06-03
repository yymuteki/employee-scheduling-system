package com.schedule.service;

import com.schedule.dto.StatsResponse;
import com.schedule.entity.AuditLog;
import com.schedule.entity.Schedule;
import com.schedule.entity.ShiftRequirement;
import com.schedule.entity.User;
import com.schedule.repository.ScheduleRepository;
import com.schedule.repository.UserRepository;
import com.schedule.util.HolidayUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final UserRepository userRepository;
    private final RequirementService requirementService;
    private final LLMService llmService;
    private final AuditLogService auditLogService;

    public ScheduleService(ScheduleRepository scheduleRepository, UserRepository userRepository,
                           RequirementService requirementService, LLMService llmService,
                           AuditLogService auditLogService) {
        this.scheduleRepository = scheduleRepository;
        this.userRepository = userRepository;
        this.requirementService = requirementService;
        this.llmService = llmService;
        this.auditLogService = auditLogService;
    }

    public List<Schedule> getByMonth(String yearMonth) {
        return scheduleRepository.findByYearMonth(yearMonth);
    }

    public List<Schedule> getByUserAndMonth(Long userId, String yearMonth) {
        return scheduleRepository.findByUserIdAndYearMonth(userId, yearMonth);
    }

    public boolean isPublished(String yearMonth) {
        List<Schedule> schedules = scheduleRepository.findByYearMonthAndStatus(yearMonth, Schedule.Status.PUBLISHED);
        return !schedules.isEmpty();
    }

    @Transactional
    public List<Schedule> generate(String yearMonth) {
        // Delete all existing records for this month (both DRAFT and PUBLISHED)
        scheduleRepository.deleteByYearMonth(yearMonth);

        List<User> employees = userRepository.findByRole(User.Role.EMPLOYEE);
        List<ShiftRequirement> requirements = requirementService.getByMonth(yearMonth);
        List<String> holidays = getHolidayList(yearMonth);

        List<Map<String, Object>> reqData = new ArrayList<>();
        for (User emp : employees) {
            ShiftRequirement req = requirements.stream()
                    .filter(r -> r.getUser().getId().equals(emp.getId()))
                    .findFirst().orElse(null);
            Map<String, Object> entry = new HashMap<>();
            entry.put("name", emp.getName());
            entry.put("unavailable", req != null ? req.getParsedUnavailable() : "[]");
            entry.put("preference", req != null ? req.getParsedPreferences() : "{}");
            reqData.add(entry);
        }

        List<String> employeeNames = employees.stream().map(User::getName).collect(Collectors.toList());
        Map<String, User> nameToUser = new HashMap<>();
        for (User emp : employees) {
            nameToUser.put(emp.getName(), emp);
        }

        List<Map<String, Object>> generated = null;
        String lastError = null;

        // Try up to 2 times (1 initial + 1 retry) if validation fails
        for (int attempt = 0; attempt < 2; attempt++) {
            generated = llmService.generateSchedule(reqData, yearMonth, employeeNames, holidays);

            List<Schedule> parsed = parseGeneratedSchedule(generated, employees, nameToUser, yearMonth);
            List<String> violations = validateHardConstraints(parsed, employees, yearMonth);
            if (violations.isEmpty()) {
                List<Schedule> saved = scheduleRepository.saveAll(parsed);
                auditLogService.log(AuditLog.Action.CREATE, "Schedule", null,
                        Map.of("yearMonth", yearMonth, "recordCount", saved.size()));
                return saved;
            }
            lastError = "第" + (attempt + 1) + "次生成不满足以下约束: " + String.join("; ", violations);
        }

        throw new RuntimeException("LLM 排班生成失败: " + lastError);
    }

    /**
     * Parse LLM-generated schedule data into Schedule entities.
     */
    private List<Schedule> parseGeneratedSchedule(List<Map<String, Object>> generated,
                                                    List<User> employees,
                                                    Map<String, User> nameToUser,
                                                    String yearMonth) {
        List<Schedule> schedules = new ArrayList<>();
        for (Map<String, Object> day : generated) {
            String dateStr = (String) day.get("date");
            LocalDate date = LocalDate.parse(dateStr);

            if (HolidayUtil.isHoliday(date)) {
                for (User emp : employees) {
                    schedules.add(createSchedule(emp, date, Schedule.Shift.OFF, yearMonth));
                }
                continue;
            }

            String morningName = (String) day.get("morning");
            String eveningName = (String) day.get("evening");

            for (User emp : employees) {
                Schedule.Shift shift;
                if (emp.getName().equals(morningName)) {
                    shift = Schedule.Shift.MORNING;
                } else if (emp.getName().equals(eveningName)) {
                    shift = Schedule.Shift.EVENING;
                } else {
                    shift = Schedule.Shift.OFF;
                }
                schedules.add(createSchedule(emp, date, shift, yearMonth));
            }
        }
        return schedules;
    }

    /**
     * Validate hard constraints 2, 3, 5.
     * Rule 1 is enforced by DB unique constraint; Rule 4 is enforced in parse.
     */
    private List<String> validateHardConstraints(List<Schedule> schedules, List<User> employees, String yearMonth) {
        List<String> violations = new ArrayList<>();

        // Group schedules by user for per-user checks
        Map<Long, List<Schedule>> byUser = new HashMap<>();
        for (Schedule s : schedules) {
            byUser.computeIfAbsent(s.getUser().getId(), k -> new ArrayList<>()).add(s);
        }

        // Rule 2: No consecutive working days > 4
        for (User emp : employees) {
            List<Schedule> userScheds = byUser.getOrDefault(emp.getId(), List.of());
            userScheds.sort(Comparator.comparing(Schedule::getDate));
            int consecutive = 0;
            YearMonth ym = YearMonth.parse(yearMonth);
            for (int day = 1; day <= ym.lengthOfMonth(); day++) {
                LocalDate date = ym.atDay(day);
                Schedule s = userScheds.stream()
                        .filter(x -> x.getDate().equals(date))
                        .findFirst().orElse(null);
                if (s != null && s.getShift() != Schedule.Shift.OFF) {
                    consecutive++;
                    if (consecutive > 4) {
                        violations.add(emp.getName() + " 从 " + date.minusDays(consecutive - 1) + " 起连续工作超过4天");
                        break;
                    }
                } else {
                    consecutive = 0;
                }
            }
        }

        // Rule 3: Each shift has at least 1 person every non-holiday weekday
        YearMonth ym = YearMonth.parse(yearMonth);
        for (int day = 1; day <= ym.lengthOfMonth(); day++) {
            LocalDate date = ym.atDay(day);
            if (HolidayUtil.isHoliday(date) || date.getDayOfWeek().getValue() > 5) continue;
            final LocalDate d = date;
            long morningCount = schedules.stream()
                    .filter(s -> s.getDate().equals(d) && s.getShift() == Schedule.Shift.MORNING).count();
            long eveningCount = schedules.stream()
                    .filter(s -> s.getDate().equals(d) && s.getShift() == Schedule.Shift.EVENING).count();
            if (morningCount < 1) violations.add(date + " 早班无人");
            if (eveningCount < 1) violations.add(date + " 晚班无人");
        }

        // Rule 5: Max 20 working days per employee per month
        for (User emp : employees) {
            long workingDays = schedules.stream()
                    .filter(s -> s.getUser().getId().equals(emp.getId()))
                    .filter(s -> s.getShift() != Schedule.Shift.OFF)
                    .count();
            if (workingDays > 20) {
                violations.add(emp.getName() + " 本月工作 " + workingDays + " 天，超过20天上限");
            }
        }

        return violations;
    }

    @Transactional
    public Schedule updateShift(Long scheduleId, String shiftStr) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("排班记录不存在"));
        Schedule.Shift newShift = Schedule.Shift.valueOf(shiftStr);

        if (newShift != Schedule.Shift.OFF) {
            Optional<Schedule> existing = scheduleRepository.findByUserIdAndDate(
                    schedule.getUser().getId(), schedule.getDate());
            if (existing.isPresent() && !existing.get().getId().equals(scheduleId)) {
                throw new RuntimeException(schedule.getUser().getName()
                        + " 已在" + schedule.getDate() + "被安排"
                        + (existing.get().getShift() == Schedule.Shift.MORNING ? "早班" : "晚班")
                        + "，不能同时被选为" + (newShift == Schedule.Shift.MORNING ? "早班" : "晚班"));
            }
        }

        Schedule.Shift oldShift = schedule.getShift();
        schedule.setShift(newShift);
        Schedule saved = scheduleRepository.save(schedule);
        auditLogService.log(AuditLog.Action.UPDATE, "Schedule", saved.getId(),
                Map.of("date", saved.getDate().toString(),
                       "userId", saved.getUser().getId(),
                       "oldShift", oldShift.name(),
                       "newShift", newShift.name()));
        return saved;
    }

    /**
     * Assign a specific employee to a shift on a given date.
     * Handles clearing the previous assignee and the new assignee's old shift.
     */
    @Transactional
    public void assignShift(String yearMonth, LocalDate date, Schedule.Shift shiftType, String userName) {
        List<User> employees = userRepository.findByRole(User.Role.EMPLOYEE);
        User targetUser = employees.stream()
                .filter(e -> e.getName().equals(userName))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("员工不存在: " + userName));

        // Find the user currently assigned to this shift on this date
        List<Schedule> daySchedules = scheduleRepository.findByDate(date);
        Schedule oldMorning = null;
        Schedule oldEvening = null;
        Schedule targetUserRecord = null;

        for (Schedule s : daySchedules) {
            if (s.getShift() == Schedule.Shift.MORNING) oldMorning = s;
            else if (s.getShift() == Schedule.Shift.EVENING) oldEvening = s;
            if (s.getUser().getId().equals(targetUser.getId())) targetUserRecord = s;
        }

        // Create records if they don't exist
        if (daySchedules.isEmpty()) {
            for (User emp : employees) {
                Schedule s = createSchedule(emp, date, Schedule.Shift.OFF, yearMonth);
                s = scheduleRepository.save(s);
                if (emp.getId().equals(targetUser.getId())) targetUserRecord = s;
                daySchedules.add(s);
            }
            // Re-fetch after save to get IDs
            daySchedules = scheduleRepository.findByDate(date);
            for (Schedule s : daySchedules) {
                if (s.getShift() == Schedule.Shift.MORNING) oldMorning = s;
                else if (s.getShift() == Schedule.Shift.EVENING) oldEvening = s;
                if (targetUserRecord == null && s.getUser().getId().equals(targetUser.getId()))
                    targetUserRecord = s;
            }
        }

        // Clear the old assignee for this shift
        if (shiftType == Schedule.Shift.MORNING && oldMorning != null && !oldMorning.getUser().getId().equals(targetUser.getId())) {
            oldMorning.setShift(Schedule.Shift.OFF);
            scheduleRepository.save(oldMorning);
        } else if (shiftType == Schedule.Shift.EVENING && oldEvening != null && !oldEvening.getUser().getId().equals(targetUser.getId())) {
            oldEvening.setShift(Schedule.Shift.OFF);
            scheduleRepository.save(oldEvening);
        }

        // Assign the target user to the shift
        if (targetUserRecord != null) {
            targetUserRecord.setShift(shiftType);
            scheduleRepository.save(targetUserRecord);
        } else {
            Schedule s = createSchedule(targetUser, date, shiftType, yearMonth);
            scheduleRepository.save(s);
        }
        auditLogService.log(AuditLog.Action.UPDATE, "Schedule", null,
                Map.of("yearMonth", yearMonth,
                       "date", date.toString(),
                       "shift", shiftType.name(),
                       "userName", userName));
    }

    @Transactional
    public void publish(String yearMonth) {
        scheduleRepository.publishByYearMonth(yearMonth);
        auditLogService.log(AuditLog.Action.PUBLISH, "Schedule", null,
                Map.of("yearMonth", yearMonth));
    }

    @Transactional
    public void unpublish(String yearMonth) {
        scheduleRepository.unpublishByYearMonth(yearMonth);
        auditLogService.log(AuditLog.Action.UNPUBLISH, "Schedule", null,
                Map.of("yearMonth", yearMonth));
    }

    public StatsResponse getStats(String yearMonth) {
        List<Schedule> schedules = scheduleRepository.findByYearMonth(yearMonth);
        long morningCount = schedules.stream().filter(s -> s.getShift() == Schedule.Shift.MORNING).count();
        long eveningCount = schedules.stream().filter(s -> s.getShift() == Schedule.Shift.EVENING).count();
        long totalWorking = morningCount + eveningCount;
        long employeeCount = schedules.stream().map(s -> s.getUser().getId()).distinct().count();
        double avg = employeeCount > 0 ? (double) totalWorking / employeeCount : 0;

        StatsResponse stats = new StatsResponse();
        stats.setMorningCount(morningCount);
        stats.setEveningCount(eveningCount);
        stats.setAvgWorkingDays(Math.round(avg * 10) / 10.0);
        stats.setTotalEmployees((int) employeeCount);
        return stats;
    }

    private Schedule createSchedule(User user, LocalDate date, Schedule.Shift shift, String yearMonth) {
        Schedule s = new Schedule();
        s.setUser(user);
        s.setDate(date);
        s.setShift(shift);
        s.setYearMonth(yearMonth);
        s.setStatus(Schedule.Status.DRAFT);
        return s;
    }

    private List<String> getHolidayList(String yearMonth) {
        YearMonth ym = YearMonth.parse(yearMonth);
        List<String> holidays = new ArrayList<>();
        for (int day = 1; day <= ym.lengthOfMonth(); day++) {
            LocalDate date = ym.atDay(day);
            if (HolidayUtil.isHoliday(date)) {
                holidays.add(date.toString());
            }
        }
        return holidays;
    }
}
