package com.aihye.toeicspeaking.service;

import com.aihye.toeicspeaking.entity.Evaluation;
import com.aihye.toeicspeaking.repository.EvaluationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EvaluationService {

    private final EvaluationRepository evaluationRepository;

    @Transactional
    public Evaluation saveEvaluation(Integer responseId, Integer score, String strengthsText, String feedbackText, String grammarCorrections) {
        Evaluation evaluation = new Evaluation();
        evaluation.setResponseId(responseId);
        evaluation.setScore(score);
        evaluation.setStrengthsText(strengthsText);
        evaluation.setFeedbackText(feedbackText);
        evaluation.setGrammarCorrections(grammarCorrections);
        return evaluationRepository.save(evaluation);
    }

    public List<Evaluation> getEvaluationsByResponseId(Integer responseId) {
        return evaluationRepository.findByResponseIdOrderByEvaluatedAtDesc(responseId);
    }

    public Optional<Evaluation> getLatestEvaluation(Integer responseId) {
        return evaluationRepository.findTopByResponseIdOrderByEvaluatedAtDesc(responseId);
    }
}
