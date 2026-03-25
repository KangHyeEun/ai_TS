package com.aihye.toeicspeaking.controller;

import com.aihye.toeicspeaking.dto.FeedbackRequest;
import com.aihye.toeicspeaking.dto.SampleAnswerRequest;
import com.aihye.toeicspeaking.service.GeminiService;
import com.aihye.toeicspeaking.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/gemini")
@RequiredArgsConstructor
public class GeminiController {

    private final GeminiService geminiService;
    private final QuestionService questionService;

    @PostMapping("/generate-question")
    public ResponseEntity<?> generateQuestion(@RequestBody Map<String, Integer> request) {
        try {
            int partNumber = request.get("partNumber");
            Map<String, Object> question = questionService.generateAndSave(partNumber);
            return ResponseEntity.ok(question);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "문제 생성 실패: " + e.getMessage()));
        }
    }

    @PostMapping("/get-question")
    public ResponseEntity<?> getQuestion(@RequestBody Map<String, Integer> request) {
        try {
            int partNumber = request.get("partNumber");
            Map<String, Object> question = questionService.getRandomOrGenerate(partNumber);
            return ResponseEntity.ok(question);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "문제 조회 실패: " + e.getMessage()));
        }
    }

    @PostMapping("/sample-answer")
    public Map<String, String> sampleAnswer(@RequestBody SampleAnswerRequest request) {
        String answer = geminiService.generateSampleAnswer(request);
        return Map.of("answer", answer);
    }

    @PostMapping("/analyze-pronunciation")
    public ResponseEntity<?> analyzePronunciation(@RequestBody Map<String, String> request) {
        try {
            String originalText = request.get("originalText");
            String sttText = request.get("sttText");
            String result = geminiService.analyzePronunciation(originalText, sttText);
            // JSON 파싱해서 반환
            result = result.trim();
            if (result.startsWith("```")) {
                result = result.replaceAll("```json\\s*", "").replaceAll("```\\s*$", "").trim();
            }
            return ResponseEntity.ok(new com.fasterxml.jackson.databind.ObjectMapper().readValue(result, Map.class));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "발음 분석 실패"));
        }
    }

    @PostMapping("/translate-to-english")
    public ResponseEntity<?> translateToEnglish(@RequestBody Map<String, String> request) {
        try {
            String koreanText = request.get("koreanText");
            String questionText = request.get("questionText");
            int partId = Integer.parseInt(request.getOrDefault("partId", "1"));
            String translation = geminiService.translateKoreanToEnglish(koreanText, questionText, partId);
            return ResponseEntity.ok(Map.of("translation", translation));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "번역 실패"));
        }
    }

    @PostMapping("/feedback")
    public ResponseEntity<?> feedback(@RequestBody FeedbackRequest request) {
        try {
            String feedback = geminiService.generateFeedback(request);
            return ResponseEntity.ok(Map.of("feedback", feedback));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "AI 피드백 생성 실패"));
        }
    }
}
