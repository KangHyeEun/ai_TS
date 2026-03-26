package com.aihye.toeicspeaking.service;

import com.aihye.toeicspeaking.entity.Question;
import com.aihye.toeicspeaking.entity.UserResponse;
import com.aihye.toeicspeaking.repository.QuestionRepository;
import com.aihye.toeicspeaking.repository.UserResponseRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class UserResponseService {

    private final UserResponseRepository userResponseRepository;
    private final QuestionRepository questionRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public UserResponse saveResponse(Integer userId, Integer questionId, String practiceMode, String audioFilePath, String sttText, String textAnswer) {
        UserResponse response = new UserResponse();
        response.setUserId(userId);
        response.setQuestionId(questionId);
        response.setPracticeMode(practiceMode != null ? practiceMode : "REAL");
        response.setAudioFilePath(audioFilePath);
        response.setSttText(sttText);
        response.setTextAnswer(textAnswer);

        // 동일 문제 풀이 횟수 자동 계산
        long previousAttempts = userResponseRepository.countByUserIdAndQuestionId(userId, questionId);
        response.setAttemptNumber((int) previousAttempts + 1);

        return userResponseRepository.save(response);
    }

    public List<UserResponse> getUserResponses(Integer userId) {
        return userResponseRepository.findByUserIdOrderBySubmittedAtDesc(userId);
    }

    /**
     * 사용자 응답 + 문제 상세를 함께 반환
     */
    public List<Map<String, Object>> getUserResponsesWithDetail(Integer userId) {
        List<UserResponse> responses = userResponseRepository.findByUserIdOrderBySubmittedAtDesc(userId);
        List<Map<String, Object>> result = new ArrayList<>();

        for (UserResponse r : responses) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("responseId", r.getResponseId());
            item.put("userId", r.getUserId());
            item.put("questionId", r.getQuestionId());
            item.put("practiceMode", r.getPracticeMode());
            item.put("attemptNumber", r.getAttemptNumber());
            item.put("sttText", r.getSttText());
            item.put("textAnswer", r.getTextAnswer());
            item.put("audioFilePath", r.getAudioFilePath());
            item.put("submittedAt", r.getSubmittedAt());

            // 문제 상세 정보 추가
            questionRepository.findById(r.getQuestionId()).ifPresent(q -> {
                item.put("partNumber", q.getPartNumber());
                item.put("questionType", q.getQuestionType());
                try {
                    Map questionContent = objectMapper.readValue(q.getContentText(), Map.class);
                    item.put("questionContent", questionContent);
                } catch (Exception e) {
                    item.put("questionContent", q.getContentText());
                }
            });

            result.add(item);
        }
        return result;
    }
}
