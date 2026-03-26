package com.aihye.toeicspeaking.service;

import com.aihye.toeicspeaking.dto.RecordRequest;
import com.aihye.toeicspeaking.entity.PracticeRecord;
import com.aihye.toeicspeaking.entity.Question;
import com.aihye.toeicspeaking.repository.QuestionRepository;
import com.aihye.toeicspeaking.repository.RecordRepository;
import com.aihye.toeicspeaking.repository.StudyStatsRepository;
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
