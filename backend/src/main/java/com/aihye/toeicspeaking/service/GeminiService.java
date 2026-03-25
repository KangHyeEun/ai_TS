package com.aihye.toeicspeaking.service;

import com.aihye.toeicspeaking.dto.FeedbackRequest;
import com.aihye.toeicspeaking.dto.SampleAnswerRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Base64;
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

    public String generateQuestion(int partNumber) {
        String prompt = String.format("""
            You are a TOEIC Speaking test question creator.
            You must create a NEW, ORIGINAL question for TOEIC Speaking Part %d.
            Follow BOTH the recent exam trends (2023-2025) AND the specific question type patterns below.

            ============================================================
            PART-BY-PART QUESTION TYPES AND EXAM TRENDS
            ============================================================

            [Part 1: Read a Text Aloud — 문장 읽기 (문제 1-2)]
            Question Types (randomly select one):
              - Announcement: company event, office relocation, system maintenance, policy change
              - Advertisement: product launch, store sale, service promotion, membership offer
              - Notice: building maintenance, parking update, schedule change, safety guideline
              - News/Broadcast: weather report, traffic update, local event coverage
              - Voicemail/Recorded Message: appointment confirmation, delivery notification, customer service
            Requirements: 4-5 sentences, natural English, include proper nouns, numbers, and varied punctuation.
            Recent trends: Workplace announcements, eco-friendly initiatives, technology product launches, health & wellness events.

            [Part 2: Describe a Picture — 사진 묘사 (문제 3-4)]
            Question Types (randomly select one):
              - Indoor workplace: meeting room, office desk, presentation, cafeteria
              - Outdoor activity: park, street market, construction site, sports event
              - Commercial area: shopping mall, restaurant, cafe, supermarket
              - Transportation: airport, train station, bus stop, parking lot
              - Public space: library, museum, hospital waiting area, school campus
            Requirements: Provide a vivid, detailed scene description with 3+ people doing different activities.
            Recent trends: Remote/hybrid work settings, outdoor dining, delivery services, co-working spaces.

            [Part 3: Respond to Questions — 듣고, 질문에 답하기 (문제 5-7)]
            Question Types (randomly select one topic, then create 3 progressive questions):
              - Daily life: cooking habits, exercise routines, morning routines, weekend plans
              - Shopping/Consumer: online vs offline shopping, product preferences, return experiences
              - Travel/Leisure: vacation planning, favorite destinations, travel tips
              - Work/Career: job satisfaction, workplace culture, career goals
              - Technology: smartphone usage, social media habits, online learning
              - Health/Wellness: stress management, healthy eating, sleep habits
            Requirements: Q5-Q6 are simpler (yes/no + explanation), Q7 requires longer opinion with reasons.
            Recent trends: Subscription services, food delivery apps, work-from-home experiences, AI tools in daily life.

            [Part 4: Respond Using Information — 제공된 정보를 사용하여 답하기 (문제 8-10)]
            Question Types (randomly select one):
              - Workshop/Seminar schedule: time slots, topics, speakers, locations
              - Conference/Event program: keynote, breakout sessions, networking, meals
              - Travel itinerary: flight times, hotel check-in, tour schedules, transfers
              - Class/Training timetable: subjects, instructors, rooms, office hours
              - Company event plan: team building, annual party, orientation schedule
            Requirements: Include a detailed schedule/table with at least 5 time entries.
            Q8-Q9: factual answers from the schedule. Q10: correct wrong information the caller states.
            Recent trends: Virtual/hybrid event schedules, wellness workshop programs, tech conference agendas.

            [Part 5: Express an Opinion — 의견 제시하기 (문제 11)]
            Question Types (randomly select one):
              - Agree/Disagree: "Do you agree or disagree with the statement that..."
              - Preference: "Some people prefer A while others prefer B. Which do you prefer?"
              - Problem/Solution: "What is the best way to solve [issue]?"
            Topic categories:
              - Education: online vs classroom, mandatory courses, study abroad
              - Workplace: remote work policy, dress code, open office vs private
              - Technology: AI in education, social media regulation, screen time limits
              - Environment: plastic ban, public transport incentives, renewable energy
              - Society: volunteer requirements, retirement age, public space usage
            Requirements: Clear position statement + 2 supporting reasons with examples.
            Recent trends: AI impact on jobs, 4-day work week, digital detox, sustainable living.

            ============================================================
            OUTPUT FORMAT
            ============================================================
            IMPORTANT: Respond ONLY in valid JSON format (no markdown, no code blocks).

            For Part 1:
            {"text": "the passage to read aloud (4-5 sentences)", "questionType": "지문 읽기"}

            For Part 2:
            {"text": "Describe the picture below.", "hint": "detailed scene description with 3+ people, their actions, locations, and background details", "questionType": "사진 묘사"}

            For Part 3:
            {"text": "first question (simpler)", "questionType": "질문 응답", "subQuestions": ["second question (simpler)", "third question (requires longer answer with reasons)"]}

            For Part 4:
            {"text": "main instruction", "infoTitle": "event/seminar title", "infoDetails": "date and location info, one item per line (e.g. Monday, November 16\\nEdison Community Center\\nRegistration fee: 25$)", "infoSchedule": [{"time": "9:00-10:00 A.M.", "content": "Welcome speech", "speaker": "Tom Rodriguez"}, {"time": "10:00-11:00 A.M.", "content": "Lecture: nutritional supplements", "speaker": "Jane Smith"}], "questionType": "정보 활용", "subQuestions": ["Q8: factual question", "Q9: factual question", "Q10: caller states wrong info, correct it"]}
            IMPORTANT for Part 4: infoSchedule must have at least 5 entries. Each entry must have time, content, speaker fields. Use "-" for speaker if none. Content should use bold-style prefixes like "Lecture:", "Presentation:", "Group discussion:", "Workshop:", "Lunch" etc.

            For Part 5:
            {"text": "the opinion question with full context and instructions", "questionType": "의견 제시"}

            Generate for Part %d now.
            """, partNumber, partNumber);

        return callGeminiApi(prompt);
    }

    public String generateSampleAnswer(SampleAnswerRequest request) {
        int target = request.getTargetScore() != null ? request.getTargetScore() : 130;
        int respTime = request.getResponseTime() != null ? request.getResponseTime() : 30;
        String levelGuide = getLevelGuide(target);
        String timeGuide = getTimeGuide(respTime);

        StringBuilder prompt = new StringBuilder();
        prompt.append(String.format("""
            You are a TOEIC Speaking expert tutor.
            The student is practicing TOEIC Speaking Part %d.
            The student's target score is %d out of 200.
            The response time limit is %d seconds.

            %s

            %s

            Question: %s
            """, request.getPartId(), target, respTime, levelGuide, timeGuide, request.getQuestionText()));

        if (request.getInfo() != null) {
            prompt.append(String.format("\nProvided information:\n%s\n", request.getInfo()));
        }

        if (request.getSubQuestions() != null && !request.getSubQuestions().isEmpty()) {
            String subQs = IntStream.range(0, request.getSubQuestions().size())
                    .mapToObj(i -> (i + 1) + ". " + request.getSubQuestions().get(i))
                    .collect(Collectors.joining("\n"));
            prompt.append(String.format("\nSub-questions:\n%s\n", subQs));
        }

        prompt.append(String.format("""

            Please provide a model answer that:
            - Can be spoken naturally within %d seconds
            - Matches the target level complexity
            - Uses appropriate pacing (not too fast, not too slow)

            Format your response in the following structure:
            ## 모범 답안 (목표 %d점 / 응답시간 %d초)
            (English model answer that fits within the time limit)

            ## 예상 소요 시간
            (Estimated speaking time for this answer)

            ## 한국어 해석
            (Korean translation)

            ## 핵심 표현
            (Key expressions with Korean explanation)

            ## 목표 점수 달성 팁
            (Specific tips for reaching target score within the time limit, in Korean)
            """, respTime, target, respTime));

        return callGeminiApi(prompt.toString());
    }

    private String getTimeGuide(int responseTime) {
        if (responseTime <= 15) {
            return """
                TIME CONSTRAINT (15 seconds): The answer must be very concise.
                - 1-2 short sentences maximum
                - Average speaking speed: ~150 words/min → about 35-40 words
                - Go straight to the point, no filler phrases""";
        } else if (responseTime <= 30) {
            return """
                TIME CONSTRAINT (30 seconds): The answer should be moderate length.
                - 2-4 sentences
                - Average speaking speed: ~150 words/min → about 65-75 words
                - Brief introduction + main point + 1 supporting detail""";
        } else if (responseTime <= 45) {
            return """
                TIME CONSTRAINT (45 seconds): The answer can be moderately detailed.
                - 4-6 sentences
                - Average speaking speed: ~150 words/min → about 100-110 words
                - Clear structure with introduction, main points, and brief conclusion""";
        } else {
            return """
                TIME CONSTRAINT (60 seconds): The answer should be well-developed.
                - 6-8 sentences
                - Average speaking speed: ~150 words/min → about 130-150 words
                - Full structure: position statement + 2 reasons with examples + conclusion""";
        }
    }

    private String getLevelGuide(int targetScore) {
        if (targetScore >= 180) {
            return """
                Level 8 (Advanced): The model answer should use sophisticated vocabulary,
                complex sentence structures, varied connectors, and demonstrate native-like fluency.
                Include idiomatic expressions and nuanced opinions with multiple supporting examples.""";
        } else if (targetScore >= 160) {
            return """
                Level 7 (High-Intermediate): Use clear, well-organized responses with good vocabulary range.
                Include transition words, compound/complex sentences, and 2 supporting reasons with examples.
                Minor errors are acceptable.""";
        } else if (targetScore >= 130) {
            return """
                Level 5-6 (Intermediate): Use clear, simple-to-moderate sentences.
                Focus on directly answering the question with 1-2 reasons.
                Use common vocabulary and basic connectors (First, Also, Because).
                Keep responses organized but not overly complex.""";
        } else {
            return """
                Level 3-4 (Basic): Use simple, short sentences with basic vocabulary.
                Focus on answering the core question directly.
                Use very common words and simple structures (I think... because...).
                Keep the answer brief and easy to follow.""";
        }
    }

    public String analyzePronunciation(String originalText, String sttText) {
        String prompt = String.format("""
            You are a TOEIC Speaking pronunciation analysis expert.

            The student was asked to read the following passage aloud:
            Original text: "%s"

            The speech recognition system transcribed their speech as:
            Student's speech (STT result): "%s"

            Compare the original text with the STT result word by word.
            Identify words that were mispronounced, skipped, or incorrectly spoken.

            IMPORTANT: Respond ONLY in valid JSON format (no markdown, no code blocks).
            {
              "score": (pronunciation accuracy percentage 0-100),
              "totalWords": (total word count in original),
              "correctWords": (number of correctly pronounced words),
              "incorrectWords": [
                {"original": "word1", "spoken": "what student said or SKIPPED", "tip": "brief Korean tip for correct pronunciation"},
                {"original": "word2", "spoken": "what student said", "tip": "Korean tip"}
              ],
              "overallFeedback": "Overall pronunciation feedback in Korean (2-3 sentences)"
            }
            """, originalText, sttText);

        return callGeminiApi(prompt);
    }

    public String translateKoreanToEnglish(String koreanText, String questionText, int partId) {
        String prompt = String.format("""
            You are a TOEIC Speaking expert translator and tutor.

            The student answered a TOEIC Speaking Part %d question in Korean.
            Translate their Korean answer into natural, fluent English suitable for TOEIC Speaking.

            Question: %s
            Student's Korean answer: %s

            Please provide:

            ## 영어 번역
            (Natural English translation of the student's answer, suitable for TOEIC Speaking)

            ## 표현 개선
            (If the student's idea can be expressed better, provide an improved English version with explanation in Korean)

            ## 핵심 영어 표현
            (List 3-5 key English expressions used in the translation, with Korean explanation)
            """, partId, questionText, koreanText);

        return callGeminiApi(prompt);
    }

    public String generateFeedback(FeedbackRequest request) {
        int target = request.getTargetScore() != null ? request.getTargetScore() : 130;
        int respTime = request.getResponseTime() != null ? request.getResponseTime() : 30;
        String levelGuide = getLevelGuide(target);
        String timeGuide = getTimeGuide(respTime);

        String prompt = String.format("""
            You are a TOEIC Speaking expert tutor evaluating a student's response.

            TOEIC Speaking Part %d
            Question: %s
            Student's answer: %s

            The student's target score is %d out of 200.
            The response time limit is %d seconds.
            %s
            %s

            Please evaluate based on the student's target score and provide feedback in Korean:

            ## 현재 예상 점수
            (Estimate the current TOEIC Speaking score for this response, out of 200)

            ## 목표 점수(%d점) 대비 분석
            (How close is this response to the target score? What's missing?)

            ## 잘한 점
            (What the student did well - be specific)

            ## 개선할 점
            (Specific areas to improve to reach the target score, with actionable advice)

            ## 수정된 답변 (목표 %d점 / 응답시간 %d초)
            (Corrected/improved version that can be spoken within %d seconds, matching the target score level, in English)

            ## 핵심 표현 학습
            (3-5 key expressions from the improved answer, with Korean explanation)

            Keep the feedback encouraging, constructive, and focused on reaching the target score.
            The corrected answer MUST be speakable within %d seconds at normal speaking pace.
            """, request.getPartId(), request.getQuestionText(), request.getUserAnswer(),
                target, respTime, levelGuide, timeGuide, target, target, respTime, respTime, respTime);

        return callGeminiApi(prompt);
    }

    /**
     * Imagen 3 API로 이미지 생성 후 Base64 바이트 배열 반환
     */
    public byte[] generateImage(String sceneDescription) {
        String prompt = String.format(
            "A realistic photograph for TOEIC Speaking Part 2 picture description practice. " +
            "The scene shows: %s. " +
            "The image should be clear, well-lit, and show people doing everyday activities. " +
            "Photorealistic style, no text or watermarks.", sceneDescription);

        WebClient imagenClient = WebClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta/models/imagen-3.0-generate-002")
                .build();

        Map<String, Object> requestBody = Map.of(
            "instances", List.of(Map.of("prompt", prompt)),
            "parameters", Map.of(
                "sampleCount", 1,
                "aspectRatio", "16:9"
            )
        );

        Map response = imagenClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path(":predict")
                        .queryParam("key", apiKey)
                        .build())
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        List<Map> predictions = (List<Map>) response.get("predictions");
        String base64Image = (String) predictions.get(0).get("bytesBase64Encoded");
        return Base64.getDecoder().decode(base64Image);
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
