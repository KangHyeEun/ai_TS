package com.aihye.toeicspeaking.service;

import com.aihye.toeicspeaking.dto.RecordRequest;
import com.aihye.toeicspeaking.entity.PracticeRecord;
import com.aihye.toeicspeaking.entity.Question;
import com.aihye.toeicspeaking.entity.UserResponse;
import com.aihye.toeicspeaking.repository.EvaluationRepository;
import com.aihye.toeicspeaking.repository.QuestionRepository;
import com.aihye.toeicspeaking.repository.RecordRepository;
import com.aihye.toeicspeaking.repository.StudyStatsRepository;
import com.aihye.toeicspeaking.repository.UserResponseRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class RecordService {

    private final RecordRepository recordRepository;
    private final QuestionRepository questionRepository;
    private final UserResponseRepository userResponseRepository;
    private final EvaluationRepository evaluationRepository;
    private final StudyStatsRepository studyStatsRepository;
    private final ObjectMapper objectMapper;

    public List<PracticeRecord> getAllRecords() {
        return recordRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<PracticeRecord> getRecordsByUserId(Integer userId) {
        return recordRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * 기록 + 문제 상세를 함께 반환
     */
    public List<Map<String, Object>> getRecordsWithDetail(Integer userId) {
        List<PracticeRecord> records = recordRepository.findByUserIdOrderByCreatedAtDesc(userId);
        List<UserResponse> responses = userResponseRepository.findByUserIdOrderBySubmittedAtDesc(userId);
        List<Map<String, Object>> result = new ArrayList<>();

        for (PracticeRecord r : records) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", r.getId());
            item.put("userId", r.getUserId());
            item.put("questionId", r.getQuestionId());
            item.put("partId", r.getPartId());
            item.put("partTitle", r.getPartTitle());
            item.put("practiceMode", r.getPracticeMode());
            item.put("createdAt", r.getCreatedAt());

            // 문제 상세 정보
            if (r.getQuestionId() != null) {
                questionRepository.findById(r.getQuestionId()).ifPresent(q -> {
                    item.put("questionType", q.getQuestionType());
                    try {
                        Map questionContent = objectMapper.readValue(q.getContentText(), Map.class);
                        item.put("questionContent", questionContent);
                    } catch (Exception e) {
                        item.put("questionContent", Map.of("text", q.getContentText()));
                    }
                });

                // 해당 문제의 가장 최근 답변 + 평가 조회
                responses.stream()
                        .filter(resp -> r.getQuestionId().equals(resp.getQuestionId()))
                        .findFirst()
                        .ifPresent(resp -> {
                            item.put("sttText", resp.getSttText());
                            item.put("textAnswer", resp.getTextAnswer());
                            item.put("audioFilePath", resp.getAudioFilePath());

                            // 해당 응답의 최신 평가 (전체 피드백 데이터)
                            evaluationRepository.findTopByResponseIdOrderByEvaluatedAtDesc(resp.getResponseId())
                                    .ifPresent(eval -> {
                                        item.put("score", eval.getScore());
                                        item.put("scoreComment", eval.getScoreComment());
                                        item.put("targetAnalysis", eval.getTargetAnalysis());
                                        item.put("targetTips", eval.getTargetTips());
                                        item.put("strengthsText", eval.getStrengthsText());
                                        item.put("evaluationFeedbackText", eval.getFeedbackText());
                                        item.put("correctedAnswers", eval.getCorrectedAnswers());
                                        item.put("keyExpressions", eval.getKeyExpressions());
                                    });
                        });
            }

            result.add(item);
        }
        return result;
    }

    @Transactional
    public PracticeRecord createRecord(RecordRequest request) {
        PracticeRecord record = new PracticeRecord();
        record.setUserId(request.getUserId() != null ? request.getUserId() : 1);
        record.setQuestionId(request.getQuestionId());
        record.setPartId(request.getPartId());
        record.setPartTitle(request.getPartTitle());
        record.setPracticeMode(request.getPracticeMode() != null ? request.getPracticeMode() : "REAL");
        return recordRepository.save(record);
    }

    @Transactional
    public void deleteAllRecords() {
        recordRepository.deleteAll();
        studyStatsRepository.deleteAll();
    }
}
