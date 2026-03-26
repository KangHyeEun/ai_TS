package com.aihye.toeicspeaking.service;

import com.aihye.toeicspeaking.entity.StudyStats;
import com.aihye.toeicspeaking.repository.StudyStatsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StudyStatsService {

    private final StudyStatsRepository studyStatsRepository;

    /**
     * 오늘의 학습 통계에 완료 문제 수 1 증가 (없으면 새로 생성)
     */
    @Transactional
    public StudyStats incrementQuestionCount(Integer userId) {
        LocalDate today = LocalDate.now();
        StudyStats stats = studyStatsRepository.findByUserIdAndStudyDate(userId, today)
                .orElseGet(() -> {
                    StudyStats newStats = new StudyStats();
                    newStats.setUserId(userId);
                    newStats.setStudyDate(today);
                    newStats.setCompletedQuestionsCount(0);
                    newStats.setAverageScore(BigDecimal.ZERO);
                    return newStats;
                });

        stats.setCompletedQuestionsCount(stats.getCompletedQuestionsCount() + 1);
        return studyStatsRepository.save(stats);
    }

    /**
     * 오늘의 평균 점수 업데이트 (새 점수를 반영하여 이동평균 계산)
     */
    @Transactional
    public StudyStats updateAverageScore(Integer userId, int newScore) {
        LocalDate today = LocalDate.now();
        StudyStats stats = studyStatsRepository.findByUserIdAndStudyDate(userId, today)
                .orElseGet(() -> {
                    StudyStats newStats = new StudyStats();
                    newStats.setUserId(userId);
                    newStats.setStudyDate(today);
                    newStats.setCompletedQuestionsCount(0);
                    newStats.setAverageScore(BigDecimal.ZERO);
                    return newStats;
                });

        int count = stats.getCompletedQuestionsCount();
        if (count <= 0) {
            stats.setAverageScore(BigDecimal.valueOf(newScore));
        } else {
            // 이동평균: (기존평균 * 기존개수 + 새점수) / (기존개수 + 1)
            BigDecimal total = stats.getAverageScore().multiply(BigDecimal.valueOf(count))
                    .add(BigDecimal.valueOf(newScore));
            stats.setAverageScore(total.divide(BigDecimal.valueOf(count + 1), 2, RoundingMode.HALF_UP));
        }

        return studyStatsRepository.save(stats);
    }

    /**
     * 특정 사용자의 전체 학습 통계 조회
     */
    public List<StudyStats> getAllStats(Integer userId) {
        return studyStatsRepository.findByUserIdOrderByStudyDateDesc(userId);
    }

    /**
     * 기간별 학습 통계 조회
     */
    public List<StudyStats> getStatsByDateRange(Integer userId, LocalDate startDate, LocalDate endDate) {
        return studyStatsRepository.findByUserIdAndStudyDateBetweenOrderByStudyDateAsc(userId, startDate, endDate);
    }

    /**
     * 오늘의 학습 통계 조회
     */
    public StudyStats getTodayStats(Integer userId) {
        return studyStatsRepository.findByUserIdAndStudyDate(userId, LocalDate.now())
                .orElse(null);
    }
}
