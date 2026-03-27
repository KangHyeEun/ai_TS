package com.aihye.toeicspeaking.service;

import com.aihye.toeicspeaking.entity.Question;
import com.aihye.toeicspeaking.repository.QuestionRepository;
import com.aihye.toeicspeaking.repository.RecordRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final RecordRepository recordRepository;
    private final GeminiService geminiService;
    private final ObjectMapper objectMapper;
    private final PlatformTransactionManager transactionManager;

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
     * 문제 출제 우선순위:
     * 1순위: AI로 새 문제 생성 → DB 자동 저장
     * 2순위: AI 실패 시 → DB에서 아직 안 푼 문제 출제
     * 3순위: 모든 문제를 다 풀었으면 → DB에서 랜덤 출제 (복습)
     */
    public Map<String, Object> getRandomOrGenerate(int partNumber) throws Exception {
        return getRandomOrGenerate(partNumber, 1); // 기본 userId=1
    }

    public Map<String, Object> getRandomOrGenerate(int partNumber, int userId) throws Exception {
        // 1순위: AI 새 문제 생성
        try {
            log.info("Part {} AI 새 문제 생성 시도...", partNumber);
            Map<String, Object> generated = generateAndSave(partNumber);
            generated.put("source", "ai");
            return generated;
        } catch (Exception e) {
            log.warn("Part {} AI 문제 생성 실패, DB 폴백: {}", partNumber, e.getMessage());
        }

        // 2순위: DB에서 안 푼 문제
        List<Question> allQuestions = questionRepository.findByPartNumber(partNumber);
        if (!allQuestions.isEmpty()) {
            Set<Integer> solvedIds = recordRepository.findSolvedQuestionIdsByUserId(userId);
            List<Question> unsolved = allQuestions.stream()
                    .filter(q -> !solvedIds.contains(q.getQuestionId()))
                    .collect(Collectors.toList());

            if (!unsolved.isEmpty()) {
                Question picked = unsolved.get(ThreadLocalRandom.current().nextInt(unsolved.size()));
                log.info("DB에서 Part {} 안 푼 문제 선택: question_id={} (전체 {}, 풀은 {}, 남은 {})",
                        partNumber, picked.getQuestionId(), allQuestions.size(), solvedIds.size(), unsolved.size());
                return toQuestionMap(picked, partNumber, "db");
            }

            // 3순위: 모두 풀었으면 랜덤 복습
            Question picked = allQuestions.get(ThreadLocalRandom.current().nextInt(allQuestions.size()));
            log.info("Part {} 모든 문제 완료, 복습 문제 선택: question_id={}", partNumber, picked.getQuestionId());
            return toQuestionMap(picked, partNumber, "review");
        }

        throw new RuntimeException("Part " + partNumber + " 문제가 없습니다.");
    }

    private Map<String, Object> toQuestionMap(Question picked, int partNumber, String source) throws Exception {
        Map<String, Object> result = new HashMap<>(objectMapper.readValue(picked.getContentText(), Map.class));
        result.put("questionId", picked.getQuestionId());
        result.put("prepTime", picked.getPreparationTime());
        result.put("responseTime", picked.getResponseTime());
        result.put("partNumber", partNumber);
        result.put("source", source);
        if (partNumber == 2) {
            String imgUrl = picked.getImageUrl();
            if (imgUrl == null || imgUrl.endsWith(".txt") || !imgUrl.startsWith("http")) {
                int seed = Math.abs(picked.getQuestionId().intValue() % 1000);
                imgUrl = String.format("https://picsum.photos/seed/%d/800/500", seed);
            }
            result.put("imageUrl", imgUrl);
        } else if (picked.getImageUrl() != null) {
            result.put("imageUrl", picked.getImageUrl());
        }
        return result;
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

        // Part 3/4: subQuestions가 있으면 text 필드 제거 (중복 방지)
        if ((partNumber == 3 || partNumber == 4) && generated.containsKey("subQuestions")) {
            generated.remove("text");
            // subQuestions 정규화: 문자열 배열 → 객체 배열로 변환
            List<?> subs = (List<?>) generated.get("subQuestions");
            List<Map<String, Object>> normalizedSubs = new ArrayList<>();
            for (int i = 0; i < subs.size(); i++) {
                Object sub = subs.get(i);
                if (sub instanceof String) {
                    Map<String, Object> subMap = new HashMap<>();
                    subMap.put("text", sub);
                    subMap.put("responseTime", i == subs.size() - 1 ? 30 : 15);
                    normalizedSubs.add(subMap);
                } else if (sub instanceof Map) {
                    normalizedSubs.add((Map<String, Object>) sub);
                }
            }
            generated.put("subQuestions", normalizedSubs);
            // 정규화된 JSON으로 다시 직렬화
            jsonText = objectMapper.writeValueAsString(generated);
        }

        // DB에 저장
        Question question = new Question();
        question.setPartNumber(partNumber);
        question.setQuestionType((String) generated.getOrDefault("questionType", "기타"));
        question.setContentText(jsonText);
        int[] times = PART_TIMES.getOrDefault(partNumber, new int[]{30, 30});
        question.setPreparationTime(times[0]);
        question.setResponseTime(times[1]);

        // 세트 문항 여부 설정
        boolean hasSubQuestions = generated.containsKey("subQuestions") && generated.get("subQuestions") instanceof List;
        question.setIsSet(hasSubQuestions);
        question.setSetOrder(hasSubQuestions ? 1 : null);

        // Part 2: 사진 묘사 - Unsplash에서 관련 이미지 URL 가져오기
        if (partNumber == 2 && generated.containsKey("hint")) {
            String sceneDescription = (String) generated.get("hint");
            try {
                String imageUrl = geminiService.getImageUrl(sceneDescription);
                question.setImageUrl(imageUrl);
                generated.put("imageUrl", imageUrl);
                log.info("Part 2 이미지 URL 설정: {}", imageUrl);
            } catch (Exception e) {
                log.warn("이미지 URL 생성 실패: {}", e.getMessage());
            }
        }

        // 독립 트랜잭션으로 DB 저장 (self-invocation 문제 우회)
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        txTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        txTemplate.execute(status -> {
            questionRepository.saveAndFlush(question);
            return null;
        });
        log.info("Part {} AI 생성 문제 DB 저장 완료: question_id={}", partNumber, question.getQuestionId());

        // 응답에 questionId와 시간 정보 추가
        generated.put("questionId", question.getQuestionId());
        generated.put("prepTime", times[0]);
        generated.put("responseTime", times[1]);
        generated.put("partNumber", partNumber);

        return generated;
    }

    /**
     * 프론트 기본 내장 문제를 DB에 저장 (중복 방지: content_text 기준)
     */
    @Transactional
    public Map<String, Object> saveIfNotExists(int partNumber, String contentJson) throws Exception {
        Map<String, Object> content = objectMapper.readValue(contentJson, Map.class);

        // 동일 content가 이미 있는지 확인
        List<Question> existing = questionRepository.findByPartNumber(partNumber);
        for (Question q : existing) {
            if (q.getContentText().equals(contentJson)) {
                log.info("Part {} 동일 문제가 이미 DB에 존재: question_id={}", partNumber, q.getQuestionId());
                Map<String, Object> result = new HashMap<>(content);
                result.put("questionId", q.getQuestionId());
                result.put("source", "db");
                return result;
            }
        }

        // 새로 저장
        Question question = new Question();
        question.setPartNumber(partNumber);
        question.setQuestionType((String) content.getOrDefault("questionType", "기타"));
        question.setContentText(contentJson);
        int[] times = PART_TIMES.getOrDefault(partNumber, new int[]{30, 30});
        question.setPreparationTime(times[0]);
        question.setResponseTime(times[1]);

        boolean hasSubQuestions = content.containsKey("subQuestions") && content.get("subQuestions") instanceof List;
        question.setIsSet(hasSubQuestions);
        question.setSetOrder(hasSubQuestions ? 1 : null);

        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        txTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        txTemplate.execute(status -> {
            questionRepository.saveAndFlush(question);
            return null;
        });
        log.info("Part {} 기본 문제 DB 저장 완료: question_id={}", partNumber, question.getQuestionId());

        Map<String, Object> result = new HashMap<>(content);
        result.put("questionId", question.getQuestionId());
        result.put("source", "saved");
        return result;
    }
}
