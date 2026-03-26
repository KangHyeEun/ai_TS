package com.aihye.toeicspeaking.controller;

import com.aihye.toeicspeaking.entity.StudyStats;
import com.aihye.toeicspeaking.service.StudyStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StudyStatsController {

    private final StudyStatsService studyStatsService;

    @GetMapping("/{userId}")
    public List<StudyStats> getAllStats(@PathVariable Integer userId) {
        return studyStatsService.getAllStats(userId);
    }

    @GetMapping("/{userId}/today")
    public ResponseEntity<?> getTodayStats(@PathVariable Integer userId) {
        StudyStats stats = studyStatsService.getTodayStats(userId);
        if (stats == null) {
            return ResponseEntity.ok(Map.of(
                "completedQuestionsCount", 0,
                "averageScore", 0
            ));
        }
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/{userId}/range")
    public List<StudyStats> getStatsByRange(
            @PathVariable Integer userId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        return studyStatsService.getStatsByDateRange(userId, start, end);
    }

    @PostMapping("/{userId}/increment")
    public ResponseEntity<?> incrementCount(@PathVariable Integer userId) {
        StudyStats stats = studyStatsService.incrementQuestionCount(userId);
        return ResponseEntity.ok(Map.of(
            "statId", stats.getStatId(),
            "completedQuestionsCount", stats.getCompletedQuestionsCount()
        ));
    }

    @PostMapping("/{userId}/score")
    public ResponseEntity<?> updateScore(@PathVariable Integer userId, @RequestBody Map<String, Integer> request) {
        Integer score = request.get("score");
        if (score == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "score는 필수입니다"));
        }
        StudyStats stats = studyStatsService.updateAverageScore(userId, score);
        return ResponseEntity.ok(Map.of(
            "statId", stats.getStatId(),
            "averageScore", stats.getAverageScore()
        ));
    }
}
