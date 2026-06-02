package com.schedule.util;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Set;

public class HolidayUtil {
    private static final Set<String> HOLIDAYS_2026 = Set.of(
        "2026-01-01",
        "2026-02-16", "2026-02-17", "2026-02-18", "2026-02-19", "2026-02-20", "2026-02-21", "2026-02-22",
        "2026-04-05",
        "2026-05-01", "2026-05-02", "2026-05-03", "2026-05-04", "2026-05-05",
        "2026-06-19", "2026-06-20", "2026-06-21",
        "2026-09-25",
        "2026-10-01", "2026-10-02", "2026-10-03", "2026-10-04", "2026-10-05", "2026-10-06", "2026-10-07"
    );

    public static boolean isHoliday(LocalDate date) {
        return HOLIDAYS_2026.contains(date.toString());
    }

    public static int getWorkingDaysInMonth(String yearMonth) {
        YearMonth ym = YearMonth.parse(yearMonth);
        int workingDays = 0;
        for (int day = 1; day <= ym.lengthOfMonth(); day++) {
            LocalDate date = ym.atDay(day);
            if (!isHoliday(date) && date.getDayOfWeek().getValue() <= 5) {
                workingDays++;
            }
        }
        return workingDays;
    }
}
