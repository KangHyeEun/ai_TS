package com.aihye.toeicspeaking.service;

import com.aihye.toeicspeaking.dto.FeedbackRequest;
import com.aihye.toeicspeaking.dto.SampleAnswerRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class GeminiService {

    private final WebClient geminiWebClient;
    private final String apiKey;

    public GeminiService(WebClient geminiWebClient,
                         @Qualifier("geminiApiKey") String apiKey) {
        this.geminiWebClient = geminiWebClient;
        this.apiKey = apiKey;
    }

    public String generateSampleAnswer(SampleAnswerRequest request) {
        StringBuilder prompt = new StringBuilder();
        prompt.append(String.format("""
            You are a TOEIC Speaking expert tutor.
            The student is practicing TOEIC Speaking Part %d.

            Question: %s
            """, request.getPartId(), request.getQuestionText()));

        if (request.getInfo() != null) {
            prompt.append(String.format("\nProvided information:\n%s\n", request.getInfo()));
        }

        if (request.getSubQuestions() != null && !request.getSubQuestions().isEmpty()) {
            String subQs = IntStream.range(0, request.getSubQuestions().size())
                    .mapToObj(i -> (i + 1) + ". " + request.getSubQuestions().get(i))
                    .collect(Collectors.joining("\n"));
            prompt.append(String.format("\nSub-questions:\n%s\n", subQs));
        }

        prompt.append("""

            Please provide:
            1. A model answer in English (natural, fluent, appropriate for TOEIC Speaking)
            2. Korean translation of the model answer
            3. Key expressions and vocabulary used (with Korean explanation)
            4. Tips for getting a high score on this type of question (in Korean)

            Format your response in the following structure:
            ## 모범 답안
            (English model answer)

            ## 한국어 해석
            (Korean translation)

            ## 핵심 표현
            (Key expressions with Korean explanation)

            ## 고득점 팁
            (Tips in Korean)
            """);

        return callGeminiApi(prompt.toString());
    }

    public String generateFeedback(FeedbackRequest request) {
        String prompt = String.format("""
            You are a TOEIC Speaking expert tutor evaluating a student's response.

            TOEIC Speaking Part %d
            Question: %s
            Student's answer: %s

            Please evaluate and provide feedback in Korean:

            ## 점수 (예상)
            (Estimate a TOEIC Speaking level 1-5 for this response)

            ## 잘한 점
            (What the student did well)

            ## 개선할 점
            (Areas for improvement with specific suggestions)

            ## 수정된 답변
            (Corrected/improved version of the student's answer in English)

            Keep the feedback encouraging and constructive.
            """, request.getPartId(), request.getQuestionText(), request.getUserAnswer());

        return callGeminiApi(prompt);
    }

    private String callGeminiApi(String prompt) {
        Map<String, Object> requestBody = Map.of(
            "contents", List.of(
                Map.of("parts", List.of(
                    Map.of("text", prompt)
                ))
            )
        );

        Map response = geminiWebClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path(":generateContent")
                        .queryParam("key", apiKey)
                        .build())
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        // 응답에서 텍스트 추출
        List<Map> candidates = (List<Map>) response.get("candidates");
        Map content = (Map) candidates.get(0).get("content");
        List<Map> parts = (List<Map>) content.get("parts");
        return (String) parts.get(0).get("text");
    }
}
