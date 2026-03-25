package com.aihye.toeicspeaking.controller;

import com.aihye.toeicspeaking.entity.UserResponse;
import com.aihye.toeicspeaking.service.UserResponseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/responses")
@RequiredArgsConstructor
public class UserResponseController {

    private final UserResponseService userResponseService;

    @PostMapping
    public ResponseEntity<?> saveResponse(@RequestBody Map<String, Object> request) {
        try {
            Integer userId = (Integer) request.get("userId");
            Integer questionId = (Integer) request.get("questionId");
            String practiceMode = (String) request.get("practiceMode");
            String audioFilePath = (String) request.get("audioFilePath");
            String sttText = (String) request.get("sttText");
            String textAnswer = (String) request.get("textAnswer");

            UserResponse saved = userResponseService.saveResponse(userId, questionId, practiceMode, audioFilePath, sttText, textAnswer);

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "responseId", saved.getResponseId(),
                "message", "응답 저장 완료"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "응답 저장 실패: " + e.getMessage()));
        }
    }

    @GetMapping("/{userId}")
    public List<UserResponse> getUserResponses(@PathVariable Integer userId) {
        return userResponseService.getUserResponses(userId);
    }
}
