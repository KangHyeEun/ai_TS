package com.aihye.toeicspeaking.service;

import com.aihye.toeicspeaking.entity.Evaluation;
import com.aihye.toeicspeaking.repository.EvaluationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EvaluationService {

    private final EvaluationRepository evaluationRepository;

    @Transactional
    public Evaluation saveEvaluation(Map<String, Object> data) {
        Evaluation evaluation = new Evaluation();
        evaluation.setResponseId(toInt(data.get("responseId")));
        evaluation.setScore(toInt(data.get("score")));
        evaluation.setScoreComment(toStr(data.get("scoreComment")));
        evaluation.setTargetAnalysis(toStr(data.get("targetAnalysis")));
        evaluation.setTargetTips(toStr(data.get("targetTips")));
        evaluation.setStrengthsText(toStr(data.get("strengthsText")));
        evaluation.setFeedbackText(toStr(data.get("feedbackText")));
        evaluation.setCorrectedAnswers(toStr(data.get("correctedAnswers")));
        evaluation.setKeyExpressions(toStr(data.get("keyExpressions")));
        return evaluationRepository.save(evaluation);
    }

    public List<Map<String, Object>> getKeyExpressionsByUserId(Integer userId) {
        List<Evaluation> evaluations = evaluationRepository.findAll();
        List<Map<String, Object>> result = new java.util.ArrayList<>();

        for (Evaluation eval : evaluations) {
            if (eval.getKeyExpressions() == null || eval.getKeyExpressions().isEmpty()) continue;
            try {
                List<?> expressions = new com.fasterxml.jackson.databind.ObjectMapper()
                        .readValue(eval.getKeyExpressions(), List.class);
                for (Object expr : expressions) {
                    if (expr instanceof Map) {
                        Map<String, Object> item = new java.util.LinkedHashMap<>((Map<String, Object>) expr);
                        item.put("evaluationId", eval.getEvaluationId());
                        item.put("score", eval.getScore());
                        item.put("evaluatedAt", eval.getEvaluatedAt());
                        result.add(item);
                    }
                }
            } catch (Exception e) { /* skip invalid JSON */ }
        }
        return result;
    }

    public List<Evaluation> getEvaluationsByResponseId(Integer responseId) {
        return evaluationRepository.findByResponseIdOrderByEvaluatedAtDesc(responseId);
    }

    public Optional<Evaluation> getLatestEvaluation(Integer responseId) {
        return evaluationRepository.findTopByResponseIdOrderByEvaluatedAtDesc(responseId);
    }

    private Integer toInt(Object val) {
        if (val == null) return null;
        if (val instanceof Integer) return (Integer) val;
        if (val instanceof Number) return ((Number) val).intValue();
        return null;
    }

    private String toStr(Object val) {
        if (val == null) return null;
        return val.toString();
    }
}
