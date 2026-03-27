package com.aihye.toeicspeaking.controller;

import com.aihye.toeicspeaking.entity.Evaluation;
import com.aihye.toeicspeaking.service.EvaluationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/evaluations")
@RequiredArgsConstructor
public class EvaluationController {

    private final EvaluationService evaluationService;

    @PostMapping
    public ResponseEntity<?> saveEvaluation(@RequestBody Map<String, Object> request) {
        try {
            Evaluation saved = evaluationService.saveEvaluation(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "evaluationId", saved.getEvaluationId(),
                "message", "평가 저장 완료"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "평가 저장 실패: " + e.getMessage()));
        }
    }

    @GetMapping("/key-expressions/{userId}")
    public List<Map<String, Object>> getKeyExpressions(@PathVariable Integer userId) {
        return evaluationService.getKeyExpressionsByUserId(userId);
    }

    @GetMapping("/response/{responseId}")
    public List<Evaluation> getByResponseId(@PathVariable Integer responseId) {
        return evaluationService.getEvaluationsByResponseId(responseId);
    }

    @GetMapping("/response/{responseId}/latest")
    public ResponseEntity<?> getLatest(@PathVariable Integer responseId) {
        return evaluationService.getLatestEvaluation(responseId)
                .map(e -> ResponseEntity.ok((Object) e))
                .orElse(ResponseEntity.noContent().build());
    }
}
