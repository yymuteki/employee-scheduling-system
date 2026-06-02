package com.schedule.repository;

import com.schedule.entity.ShiftRequirement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ShiftRequirementRepository extends JpaRepository<ShiftRequirement, Long> {
    Optional<ShiftRequirement> findByUserIdAndYearMonth(Long userId, String yearMonth);
    List<ShiftRequirement> findByYearMonth(String yearMonth);
}
