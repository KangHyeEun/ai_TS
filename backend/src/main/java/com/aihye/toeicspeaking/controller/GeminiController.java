package com.aihye.toeicspeaking.controller;

import com.aihye.toeicspeaking.dto.FeedbackRequest;
import com.aihye.toeicspeaking.dto.SampleAnswerRequest;
import com.aihye.toeicspeaking.service.GeminiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/gemini")
@RequiredArgsConstructor
public class GeminiController {

    private final GeminiService geminiService;

    @PostMapping("/sample-answer")
    public Map<String, String> sampleAnswer(@RequestBody SampleAnswerRequest request) {
        String answer = geminiService.generateSampleAnswer(request);
        return Map.of("answer", answer);
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
