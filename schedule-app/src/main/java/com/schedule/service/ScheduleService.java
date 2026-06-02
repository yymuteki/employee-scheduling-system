package com.schedule.service;

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

    public ScheduleService(ScheduleRepository scheduleRepository, UserRepository userRepository,
                           RequirementService requirementService, LLMService llmService) {
        this.scheduleRepository = scheduleRepository;
        this.userRepository = userRepository;
        this.requirementService = requirementService;
        this.llmService = llmService;
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
        scheduleRepository.deleteByYearMonthAndStatus(yearMonth, Schedule.Status.DRAFT);

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

        List<Map<String, Object>> generated = llmService.generateSchedule(reqData, yearMonth, employeeNames, holidays);

        Map<String, User> nameToUser = new HashMap<>();
        for (User emp : employees) {
            nameToUser.put(emp.getName(), emp);
        }

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

        return scheduleRepository.saveAll(schedules);
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

        schedule.setShift(newShift);
        return scheduleRepository.save(schedule);
    }

    @Transactional
    public void publish(String yearMonth) {
        scheduleRepository.publishByYearMonth(yearMonth);
    }

    @Transactional
    public void unpublish(String yearMonth) {
        scheduleRepository.unpublishByYearMonth(yearMonth);
    }

    public Map<String, Object> getStats(String yearMonth) {
        List<Schedule> schedules = scheduleRepository.findByYearMonth(yearMonth);
        long morningCount = schedules.stream().filter(s -> s.getShift() == Schedule.Shift.MORNING).count();
        long eveningCount = schedules.stream().filter(s -> s.getShift() == Schedule.Shift.EVENING).count();
        long totalWorking = morningCount + eveningCount;
        long employeeCount = schedules.stream().map(s -> s.getUser().getId()).distinct().count();
        double avg = employeeCount > 0 ? (double) totalWorking / employeeCount : 0;

        Map<String, Object> stats = new HashMap<>();
        stats.put("morningCount", morningCount);
        stats.put("eveningCount", eveningCount);
        stats.put("avgWorkingDays", Math.round(avg * 10) / 10.0);
        stats.put("totalEmployees", (int) employeeCount);
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
