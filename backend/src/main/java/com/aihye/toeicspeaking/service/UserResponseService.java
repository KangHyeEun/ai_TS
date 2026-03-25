package com.aihye.toeicspeaking.service;

import com.aihye.toeicspeaking.entity.UserResponse;
import com.aihye.toeicspeaking.repository.UserResponseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserResponseService {

    private final UserResponseRepository userResponseRepository;

    @Transactional
    public UserResponse saveResponse(Integer userId, Integer questionId, String practiceMode, String audioFilePath, String sttText, String textAnswer) {
        UserResponse response = new UserResponse();
        response.setUserId(userId);
        response.setQuestionId(questionId);
        response.setPracticeMode(practiceMode != null ? practiceMode : "REAL");
        response.setAudioFilePath(audioFilePath);
        response.setSttText(sttText);
        response.setTextAnswer(textAnswer);
        return userResponseRepository.save(response);
    }

    public List<UserResponse> getUserResponses(Integer userId) {
        return userResponseRepository.findByUserIdOrderBySubmittedAtDesc(userId);
    }
}
