package com.aihye.toeicspeaking.controller;

import com.aihye.toeicspeaking.dto.FeedbackRequest;
import com.aihye.toeicspeaking.dto.SampleAnswerRequest;
import com.aihye.toeicspeaking.service.GeminiService;
import com.aihye.toeicspeaking.service.QuestionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/gemini")
@RequiredArgsConstructor
public class GeminiController {

    private final GeminiService geminiService;
    private final QuestionService questionService;
    private final ObjectMapper objectMapper;

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

    @PostMapping("/save-question")
    public ResponseEntity<?> saveQuestion(@RequestBody Map<String, Object> request) {
        try {
            int partNumber = (Integer) request.get("partNumber");
            String contentJson = objectMapper.writeValueAsString(request.get("content"));
            Map<String, Object> result = questionService.saveIfNotExists(partNumber, contentJson);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "문제 저장 실패: " + e.getMessage()));
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
            String cleaned = cleanJsonResponse(result);
            return ResponseEntity.ok(objectMapper.readValue(cleaned, Map.class));
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
            int targetScore = Integer.parseInt(request.getOrDefault("targetScore", "130"));
            int responseTime = Integer.parseInt(request.getOrDefault("responseTime", "30"));
            String raw = geminiService.translateKoreanToEnglish(koreanText, questionText, partId, targetScore, responseTime);

            String cleaned = cleanJsonResponse(raw);
            try {
                Map parsed = objectMapper.readValue(cleaned, Map.class);
                parsed.put("raw", raw);
                return ResponseEntity.ok(parsed);
            } catch (Exception e) {
                try {
                    String extracted = extractJsonObject(cleaned);
                    Map parsed = objectMapper.readValue(extracted, Map.class);
                    parsed.put("raw", raw);
                    return ResponseEntity.ok(parsed);
                } catch (Exception e2) {
                    return ResponseEntity.ok(Map.of("translation", raw, "parseError", true));
                }
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "번역 실패: " + e.getMessage()));
        }
    }

    @PostMapping("/feedback")
    public ResponseEntity<?> feedback(@RequestBody FeedbackRequest request) {
        String feedback;
        try {
            feedback = geminiService.generateFeedback(request);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "AI 피드백 생성 실패: " + e.getMessage()));
        }

        // JSON 정리 및 파싱
        String cleaned = cleanJsonResponse(feedback);

        try {
            Map parsed = objectMapper.readValue(cleaned, Map.class);
            parsed.put("raw", feedback);
            return ResponseEntity.ok(parsed);
        } catch (Exception e) {
            log.warn("JSON 1차 파싱 실패: {}", e.getMessage());
            // 2차 시도: { } 블록만 추출
            try {
                String extracted = extractJsonObject(cleaned);
                Map parsed = objectMapper.readValue(extracted, Map.class);
                parsed.put("raw", feedback);
                return ResponseEntity.ok(parsed);
            } catch (Exception e2) {
                log.error("JSON 최종 파싱 실패: {}", e2.getMessage());
                return ResponseEntity.ok(Map.of("raw", feedback, "parseError", true));
            }
        }
    }

    /**
     * Gemini 응답에서 JSON 부분 정리
     */
    private String cleanJsonResponse(String response) {
        String cleaned = response.trim();
        // 마크다운 코드 블록 제거
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceAll("```(?:json)?\\s*", "").trim();
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.substring(0, cleaned.lastIndexOf("```")).trim();
            }
        }
        return cleaned;
    }

    /**
     * 문자열에서 첫 번째 { } JSON 객체 추출
     */
    private String extractJsonObject(String text) {
        int start = text.indexOf('{');
        if (start < 0) throw new RuntimeException("JSON 객체를 찾을 수 없음");

        int depth = 0;
        boolean inString = false;
        boolean escape = false;

        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (escape) { escape = false; continue; }
            if (c == '\\') { escape = true; continue; }
            if (c == '"') { inString = !inString; continue; }
            if (inString) continue;
            if (c == '{') depth++;
            if (c == '}') {
                depth--;
                if (depth == 0) {
                    return text.substring(start, i + 1);
                }
            }
        }
        throw new RuntimeException("JSON 객체 종료를 찾을 수 없음");
    }
}
