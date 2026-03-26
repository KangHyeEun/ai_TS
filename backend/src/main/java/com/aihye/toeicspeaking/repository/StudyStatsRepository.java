package com.aihye.toeicspeaking.repository;

import com.aihye.toeicspeaking.entity.StudyStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudyStatsRepository extends JpaRepository<StudyStats, Integer> {

    Optional<StudyStats> findByUserIdAndStudyDate(Integer userId, LocalDate studyDate);

    List<StudyStats> findByUserIdOrderByStudyDateDesc(Integer userId);

    List<StudyStats> findByUserIdAndStudyDateBetweenOrderByStudyDateAsc(
            Integer userId, LocalDate startDate, LocalDate endDate);
}
