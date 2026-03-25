package com.aihye.toeicspeaking.service;

import com.aihye.toeicspeaking.entity.Question;
import com.aihye.toeicspeaking.repository.QuestionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final GeminiService geminiService;
    private final ObjectMapper objectMapper;

    @Value("${upload.path}")
    private String uploadPath;

    // 파트별 준비시간/응답시간
    private static final Map<Integer, int[]> PART_TIMES = Map.of(
        1, new int[]{45, 45},
        2, new int[]{45, 30},
        3, new int[]{3, 15},
        4, new int[]{45, 30},
        5, new int[]{30, 60}
    );

    /**
     * DB에 해당 파트 문제가 있으면 랜덤 1개 반환, 없으면 AI로 새로 생성
     */
    public Map<String, Object> getRandomOrGenerate(int partNumber) throws Exception {
        List<Question> existing = questionRepository.findByPartNumber(partNumber);

        if (!existing.isEmpty()) {
            // DB에서 랜덤 선택
            Question picked = existing.get(ThreadLocalRandom.current().nextInt(existing.size()));
            log.info("DB에서 Part {} 문제 랜덤 선택: question_id={}", partNumber, picked.getQuestionId());

            Map<String, Object> result = new HashMap<>(objectMapper.readValue(picked.getContentText(), Map.class));
            int[] times = PART_TIMES.getOrDefault(partNumber, new int[]{30, 30});
            result.put("questionId", picked.getQuestionId());
            result.put("prepTime", picked.getPreparationTime());
            result.put("responseTime", picked.getResponseTime());
            result.put("partNumber", partNumber);
            result.put("source", "db");
            if (picked.getImageUrl() != null) {
                result.put("imageUrl", picked.getImageUrl());
            }
            return result;
        }

        // DB에 없으면 AI 생성
        log.info("Part {} 문제가 DB에 없어 AI로 생성합니다.", partNumber);
        Map<String, Object> generated = generateAndSave(partNumber);
        generated.put("source", "ai");
        return generated;
    }

    @Transactional
    public Map<String, Object> generateAndSave(int partNumber) throws Exception {
        String jsonText = geminiService.generateQuestion(partNumber);

        // Gemini 응답에서 JSON 부분만 추출
        jsonText = jsonText.trim();
        if (jsonText.startsWith("```")) {
            jsonText = jsonText.replaceAll("```json\\s*", "").replaceAll("```\\s*$", "").trim();
        }

        Map<String, Object> generated = new HashMap<>(objectMapper.readValue(jsonText, Map.class));

        // DB에 저장
        Question question = new Question();
        question.setPartNumber(partNumber);
        question.setQuestionType((String) generated.getOrDefault("questionType", "기타"));
        question.setContentText(jsonText);
        int[] times = PART_TIMES.getOrDefault(partNumber, new int[]{30, 30});
        question.setPreparationTime(times[0]);
        question.setResponseTime(times[1]);

        // Part 2: 사진 묘사 - Imagen API로 실제 이미지 생성
        if (partNumber == 2 && generated.containsKey("hint")) {
            String sceneDescription = (String) generated.get("hint");
            String imageUrl = generateAndSaveImage(sceneDescription);
            if (imageUrl != null) {
                question.setImageUrl(imageUrl);
                generated.put("imageUrl", imageUrl);
            }
        }

        questionRepository.save(question);

        // 응답에 questionId와 시간 정보 추가
        generated.put("questionId", question.getQuestionId());
        generated.put("prepTime", times[0]);
        generated.put("responseTime", times[1]);
        generated.put("partNumber", partNumber);

        return generated;
    }

    /**
     * Imagen API로 이미지를 생성하고 uploads/images/에 저장
     */
    private String generateAndSaveImage(String sceneDescription) {
        try {
            log.info("Imagen 이미지 생성 시작: {}", sceneDescription.substring(0, Math.min(50, sceneDescription.length())));

            byte[] imageBytes = geminiService.generateImage(sceneDescription);

            Path imageDir = Paths.get(uploadPath, "images").toAbsolutePath();
            Files.createDirectories(imageDir);

            String timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = String.format("part2_%s.png", timestamp);
            Path filePath = imageDir.resolve(filename);

            Files.write(filePath, imageBytes);
            log.info("이미지 저장 완료: {}", filePath);

            return "/uploads/images/" + filename;
        } catch (Exception e) {
            log.warn("Imagen 이미지 생성 실패, 텍스트 설명으로 대체: {}", e.getMessage());
            // 이미지 생성 실패 시 텍스트 설명 파일로 대체
            return saveSceneDescriptionFallback(sceneDescription);
        }
    }

    /**
     * Imagen 실패 시 텍스트 설명 파일로 대체 저장
     */
    private String saveSceneDescriptionFallback(String sceneText) {
        try {
            Path imageDir = Paths.get(uploadPath, "images").toAbsolutePath();
            Files.createDirectories(imageDir);

            String timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = String.format("scene_part2_%s.txt", timestamp);
            Path filePath = imageDir.resolve(filename);

            Files.writeString(filePath, sceneText);
            return "/uploads/images/" + filename;
        } catch (IOException e) {
            return null;
        }
    }
}
