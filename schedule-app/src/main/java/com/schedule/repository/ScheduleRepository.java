package com.schedule.repository;

import com.schedule.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    List<Schedule> findByYearMonthAndStatus(String yearMonth, Schedule.Status status);
    List<Schedule> findByYearMonth(String yearMonth);
    List<Schedule> findByUserIdAndYearMonth(Long userId, String yearMonth);
    void deleteByYearMonthAndStatus(String yearMonth, Schedule.Status status);
    void deleteByYearMonth(String yearMonth);

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

    @Query("SELECT s FROM Schedule s WHERE s.date = :date")
    List<Schedule> findByDate(LocalDate date);
}
