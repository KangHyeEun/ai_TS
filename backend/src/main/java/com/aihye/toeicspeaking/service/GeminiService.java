package com.aihye.toeicspeaking.service;

import com.aihye.toeicspeaking.dto.FeedbackRequest;
import com.aihye.toeicspeaking.dto.SampleAnswerRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Service
public class GeminiService {

    private final WebClient geminiWebClient;
    private final List<String> apiKeys;
    private final java.util.concurrent.atomic.AtomicInteger currentKeyIndex = new java.util.concurrent.atomic.AtomicInteger(0);

    // 키별 속도 제한 (무료 티어 10 RPM → 안전 마진 8 RPM per key)
    private static final int MAX_REQUESTS_PER_MINUTE = 8;
    private static final long MIN_INTERVAL_MS = 300;
    private final java.util.concurrent.ConcurrentHashMap<Integer, Deque<Long>> keyTimestamps = new java.util.concurrent.ConcurrentHashMap<>();

    public GeminiService(WebClient geminiWebClient, List<String> geminiApiKeys) {
        this.geminiWebClient = geminiWebClient;
        this.apiKeys = geminiApiKeys;
        log.info("Gemini API 키 {}개 등록됨", apiKeys.size());
    }

    private String getCurrentKey() {
        int idx = currentKeyIndex.get() % apiKeys.size();
        return apiKeys.get(idx);
    }

    private String rotateToNextKey() {
        int newIdx = currentKeyIndex.incrementAndGet() % apiKeys.size();
        log.info("API 키 전환: Key #{}", newIdx + 1);
        return apiKeys.get(newIdx);
    }

    /**
     * 현재 키의 슬라이딩 윈도우 속도 제한
     */
    private void waitForRateLimit() {
        int keyIdx = currentKeyIndex.get() % apiKeys.size();
        Deque<Long> timestamps = keyTimestamps.computeIfAbsent(keyIdx, k -> new ConcurrentLinkedDeque<>());

        long now = System.currentTimeMillis();
        while (!timestamps.isEmpty() && now - timestamps.peekFirst() > 60_000) {
            timestamps.pollFirst();
        }

        // 현재 키가 한도에 도달하면, 다른 키가 있으면 전환
        if (timestamps.size() >= MAX_REQUESTS_PER_MINUTE) {
            if (apiKeys.size() > 1) {
                // 여유 있는 다른 키 찾기
                for (int i = 1; i < apiKeys.size(); i++) {
                    int altIdx = (keyIdx + i) % apiKeys.size();
                    Deque<Long> altTs = keyTimestamps.computeIfAbsent(altIdx, k -> new ConcurrentLinkedDeque<>());
                    long altNow = System.currentTimeMillis();
                    while (!altTs.isEmpty() && altNow - altTs.peekFirst() > 60_000) altTs.pollFirst();
                    if (altTs.size() < MAX_REQUESTS_PER_MINUTE) {
                        currentKeyIndex.set(altIdx);
                        log.info("Key #{} RPM 한도 도달, Key #{}로 전환 (사용량 {}/{})",
                                keyIdx + 1, altIdx + 1, altTs.size(), MAX_REQUESTS_PER_MINUTE);
                        timestamps = altTs;
                        break;
                    }
                }
            }

            // 모든 키가 한도면 대기
            now = System.currentTimeMillis();
            while (!timestamps.isEmpty() && now - timestamps.peekFirst() > 60_000) timestamps.pollFirst();
            if (timestamps.size() >= MAX_REQUESTS_PER_MINUTE) {
                long oldest = timestamps.peekFirst();
                long waitMs = 60_000 - (now - oldest) + 500;
                if (waitMs > 0) {
                    log.info("모든 키 RPM 한도 도달, {}ms 대기 중...", waitMs);
                    try { Thread.sleep(waitMs); } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    long afterWait = System.currentTimeMillis();
                    while (!timestamps.isEmpty() && afterWait - timestamps.peekFirst() > 60_000) timestamps.pollFirst();
                }
            }
        }

        // 연속 요청 간 최소 간격
        if (!timestamps.isEmpty()) {
            long elapsed = System.currentTimeMillis() - timestamps.peekLast();
            if (elapsed < MIN_INTERVAL_MS) {
                try { Thread.sleep(MIN_INTERVAL_MS - elapsed); } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        timestamps.addLast(System.currentTimeMillis());
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
            Requirements: The question itself must be SHORT (2-3 sentences max, under 40 words). Just state the topic and ask for opinion. Do NOT add lengthy context or background explanations in the question.
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
            {"questionType": "질문 응답", "subQuestions": [{"text": "Q5 simple question", "responseTime": 15}, {"text": "Q6 simple question", "responseTime": 15}, {"text": "Q7 longer opinion question with reasons", "responseTime": 30}]}
            IMPORTANT for Part 3: subQuestions must have EXACTLY 3 items. Each item must be an object with "text" and "responseTime" fields. Q5-Q6 have responseTime 15, Q7 has responseTime 30.

            For Part 4:
            {"questionType": "정보 활용", "infoTitle": "event/seminar title", "infoDetails": "date and location info, one item per line (e.g. Monday, November 16\\nEdison Community Center\\nRegistration fee: 25$)", "infoSchedule": [{"time": "9:00-10:00 A.M.", "content": "Welcome speech", "speaker": "Tom Rodriguez"}, {"time": "10:00-11:00 A.M.", "content": "Lecture: nutritional supplements", "speaker": "Jane Smith"}], "subQuestions": [{"text": "Q8 factual question", "responseTime": 15}, {"text": "Q9 factual question", "responseTime": 15}, {"text": "Q10 caller states wrong info, correct it", "responseTime": 30}]}
            IMPORTANT for Part 4: infoSchedule must have at least 5 entries. Each entry must have time, content, speaker fields. Use "-" for speaker if none. Content should use bold-style prefixes like "Lecture:", "Presentation:", "Group discussion:", "Workshop:", "Lunch" etc. subQuestions must have EXACTLY 3 items as objects with "text" and "responseTime" fields.

            For Part 5:
            {"text": "short opinion question (2-3 sentences, under 40 words). Example: 'Do you agree or disagree that companies should allow employees to work from home? Give specific reasons to support your opinion.'", "questionType": "의견 제시"}

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

    public String translateKoreanToEnglish(String koreanText, String questionText, int partId, int targetScore, int responseTime) {
        String levelGuide = getLevelGuide(targetScore);
        String timeGuide = getTimeGuide(responseTime);

        String prompt = String.format("""
            You are a TOEIC Speaking expert translator and tutor.

            The student answered a TOEIC Speaking Part %d question in Korean.
            Target score: %d/200
            Response time limit: %d seconds

            %s
            %s

            Question: %s
            Student's Korean answer: %s

            Respond ONLY in valid JSON (no markdown, no code blocks).

            {
              "questionKo": "(문제의 한국어 번역)",
              "questionKeyWords": [
                {"word": "(영어 단어/표현)", "meaning": "(한국어 뜻)", "example": "(예문)"}
              ],
              "translation": "(학생의 한글 답변을 자연스러운 TOEIC Speaking 영어로 번역)",
              "improved": "(목표 %d점 수준에 맞는 개선된 영어 답변, %d초 내 발화 가능한 분량)",
              "improvedKo": "(개선된 영어 답변의 한국어 번역)",
              "improvements": [
                {"before": "(학생 번역의 원래 표현)", "after": "(개선된 표현)", "reason": "(한국어로 개선 이유 설명)"}
              ],
              "keyExpressions": [
                {"expression": "(영어 표현)", "meaning": "(한국어 뜻)", "example": "(예문)"}
              ]
            }

            Requirements:
            - questionKeyWords: 3-5 key English words/phrases from the question
            - translation: faithful translation of the student's Korean answer
            - improved: MUST match target score %d level AND be speakable within %d seconds (~150 words/min)
            - improvedKo: Korean translation of the improved version
            - improvements: 2-4 specific changes, each with before/after/reason
            - keyExpressions: 3-5 useful expressions from the improved answer
            """, partId, targetScore, responseTime, levelGuide, timeGuide,
                questionText, koreanText, targetScore, responseTime, targetScore, responseTime);

        return callGeminiApi(prompt);
    }

    public String generateFeedback(FeedbackRequest request) {
        int target = request.getTargetScore() != null ? request.getTargetScore() : 130;
        int respTime = request.getResponseTime() != null ? request.getResponseTime() : 30;
        String levelGuide = getLevelGuide(target);
        boolean hasSubQuestions = request.getSubQuestions() != null && !request.getSubQuestions().isEmpty();

        StringBuilder sb = new StringBuilder();
        sb.append("You are a strict TOEIC Speaking evaluator.\n\n");

        // 질문 및 답변
        sb.append("=== QUESTION ===\n");
        sb.append("TOEIC Speaking Part ").append(request.getPartId()).append("\n");

        if (hasSubQuestions) {
            sb.append("This is a SET-type question with ").append(request.getSubQuestions().size()).append(" sub-questions.\n\n");
            if (request.getInfo() != null) {
                sb.append("Provided information:\n").append(request.getInfo()).append("\n\n");
            }
            for (int i = 0; i < request.getSubQuestions().size(); i++) {
                var sq = request.getSubQuestions().get(i);
                sb.append("Q").append(i + 1).append(": ").append(sq.getQuestion()).append("\n");
                sb.append("  Response time: ").append(sq.getResponseTime()).append(" seconds\n");
                sb.append("  Student's answer: \"").append(sq.getAnswer() != null ? sq.getAnswer() : "").append("\"\n\n");
            }
        } else {
            sb.append(request.getQuestionText()).append("\n\n");
            sb.append("=== STUDENT'S ACTUAL ANSWER (VERBATIM) ===\n");
            sb.append("\"").append(request.getUserAnswer()).append("\"\n\n");
        }

        // 규칙
        sb.append("=== STRICT RULES ===\n");
        sb.append("1. ONLY analyze the EXACT text the student wrote. NEVER fabricate or invent answers.\n");
        sb.append("2. 'quote' and 'studentSaid' fields MUST be EXACT copy-paste from the student's answer.\n");
        sb.append("3. If an answer is empty, gibberish, non-English, or too short: score 0-10, strengths=[], explain insufficiency.\n");
        sb.append("4. '(답변 없음)' means no answer was given.\n\n");

        sb.append("Target score: ").append(target).append("/200\n");
        sb.append(levelGuide).append("\n\n");

        if (request.getPartId() == 2) {
            sb.append("For Part 2: [Scene Description] in the question is ground truth for the image.\n\n");
        }

        // JSON 구조
        sb.append("Respond ONLY in valid JSON (no markdown, no code blocks). All text in Korean except English quotes/answers.\n\n");
        sb.append("{\n");
        sb.append("  \"estimatedScore\": (number 0-200),\n");
        sb.append("  \"scoreComment\": \"(1-2 sentence score explanation in Korean)\",\n");
        sb.append("  \"targetAnalysis\": \"(gap analysis vs target ").append(target).append(" score, 2-3 sentences in Korean)\",\n");
        sb.append("  \"targetTips\": [\"(tip 1 in Korean)\", \"(tip 2)\", \"(tip 3)\"],\n");
        sb.append("  \"strengths\": [\n");
        sb.append("    {\"point\": \"(title)\", \"detail\": \"(Korean)\", \"quote\": \"(EXACT from student's answer or \\\"\\\")\"}\n");
        sb.append("  ],\n");
        sb.append("  \"improvements\": [\n");
        sb.append("    {\"point\": \"(title)\", \"detail\": \"(Korean)\", \"studentSaid\": \"(EXACT from student's answer or \\\"\\\")\", \"betterWay\": \"(improved English)\"}\n");
        sb.append("  ],\n");

        if (hasSubQuestions) {
            sb.append("  \"correctedAnswers\": [\n");
            for (int i = 0; i < request.getSubQuestions().size(); i++) {
                var sq = request.getSubQuestions().get(i);
                int sqTime = sq.getResponseTime() != null ? sq.getResponseTime() : respTime;
                sb.append("    {\"question\": \"(Q").append(i + 1).append(" question text)\", ");
                sb.append("\"answer\": \"(model English answer within ").append(sqTime).append("s)\", ");
                sb.append("\"answerKo\": \"(Korean translation)\", ");
                sb.append("\"responseTime\": ").append(sqTime).append("}");
                if (i < request.getSubQuestions().size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append("  ],\n");
        } else {
            sb.append("  \"correctedAnswers\": [\n");
            sb.append("    {\"question\": \"(the question)\", \"answer\": \"(model English answer within ").append(respTime).append("s)\", \"answerKo\": \"(Korean translation)\", \"responseTime\": ").append(respTime).append("}\n");
            sb.append("  ],\n");
        }

        sb.append("  \"keyExpressions\": [\n");
        sb.append("    {\"expression\": \"(English)\", \"meaning\": \"(Korean)\", \"example\": \"(example sentence)\"}\n");
        sb.append("  ]\n");
        sb.append("}\n\n");

        sb.append("REQUIREMENTS:\n");
        sb.append("- targetTips: 3-4 actionable tips for reaching target score ").append(target).append("\n");
        sb.append("- strengths: 2-3 items (or [] if answer is insufficient)\n");
        sb.append("- improvements: 2-3 items with what student said vs better way\n");
        if (hasSubQuestions) {
            sb.append("- correctedAnswers: EXACTLY ").append(request.getSubQuestions().size()).append(" items, one per sub-question\n");
            sb.append("- Each answer MUST fit within its own responseTime at normal speaking pace (~150 words/min)\n");
        } else {
            sb.append("- correctedAnswers: 1 item, must fit within ").append(respTime).append(" seconds\n");
        }
        sb.append("- keyExpressions: 3-5 useful expressions from the corrected answers\n");

        return callGeminiApi(sb.toString());
    }

    /**
     * 장면 설명에서 카테고리를 추출하여 매칭되는 Pexels 이미지 URL 반환
     */
    public String getImageUrl(String sceneDescription) {
        String desc = sceneDescription.toLowerCase();

        // TOEIC Part 2 빈출 장면별 Pexels 무료 이미지 (직접 링크)
        if (desc.contains("office") || desc.contains("meeting") || desc.contains("desk") || desc.contains("cowork")) {
            return "https://images.pexels.com/photos/3184292/pexels-photo-3184292.jpeg?auto=compress&cs=tinysrgb&w=800";
        } else if (desc.contains("cafe") || desc.contains("coffee") || desc.contains("restaurant") || desc.contains("dining")) {
            return "https://images.pexels.com/photos/1537635/pexels-photo-1537635.jpeg?auto=compress&cs=tinysrgb&w=800";
        } else if (desc.contains("park") || desc.contains("outdoor") || desc.contains("garden")) {
            return "https://images.pexels.com/photos/1164572/pexels-photo-1164572.jpeg?auto=compress&cs=tinysrgb&w=800";
        } else if (desc.contains("market") || desc.contains("shop") || desc.contains("store") || desc.contains("mall")) {
            return "https://images.pexels.com/photos/3965548/pexels-photo-3965548.jpeg?auto=compress&cs=tinysrgb&w=800";
        } else if (desc.contains("airport") || desc.contains("station") || desc.contains("bus") || desc.contains("train")) {
            return "https://images.pexels.com/photos/2381463/pexels-photo-2381463.jpeg?auto=compress&cs=tinysrgb&w=800";
        } else if (desc.contains("library") || desc.contains("study") || desc.contains("school") || desc.contains("campus")) {
            return "https://images.pexels.com/photos/1438072/pexels-photo-1438072.jpeg?auto=compress&cs=tinysrgb&w=800";
        } else if (desc.contains("construction") || desc.contains("build")) {
            return "https://images.pexels.com/photos/2219024/pexels-photo-2219024.jpeg?auto=compress&cs=tinysrgb&w=800";
        } else if (desc.contains("hospital") || desc.contains("doctor") || desc.contains("medical")) {
            return "https://images.pexels.com/photos/4021775/pexels-photo-4021775.jpeg?auto=compress&cs=tinysrgb&w=800";
        } else if (desc.contains("gym") || desc.contains("sport") || desc.contains("exercise") || desc.contains("fitness")) {
            return "https://images.pexels.com/photos/1552242/pexels-photo-1552242.jpeg?auto=compress&cs=tinysrgb&w=800";
        } else if (desc.contains("kitchen") || desc.contains("cook")) {
            return "https://images.pexels.com/photos/2544829/pexels-photo-2544829.jpeg?auto=compress&cs=tinysrgb&w=800";
        } else {
            // 기본: 사무실 사진
            return "https://images.pexels.com/photos/3184291/pexels-photo-3184291.jpeg?auto=compress&cs=tinysrgb&w=800";
        }
    }

    private String callGeminiApi(String prompt) {
        Map<String, Object> requestBody = Map.of(
            "contents", List.of(
                Map.of("parts", List.of(
                    Map.of("text", prompt)
                ))
            )
        );

        int maxRetries = apiKeys.size() * 3; // 키 수 × 3회
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            waitForRateLimit();
            String key = getCurrentKey();

            try {
                Map response = geminiWebClient.post()
                        .uri(uriBuilder -> uriBuilder
                                .path(":generateContent")
                                .queryParam("key", key)
                                .build())
                        .bodyValue(requestBody)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .block();

                List<Map> candidates = (List<Map>) response.get("candidates");
                Map content = (Map) candidates.get(0).get("content");
                List<Map> parts = (List<Map>) content.get("parts");
                return (String) parts.get(0).get("text");

            } catch (WebClientResponseException.TooManyRequests e) {
                log.warn("Gemini API 429 (Key #{}, 시도 {}/{})",
                        currentKeyIndex.get() % apiKeys.size() + 1, attempt, maxRetries);

                // 다른 키가 있으면 즉시 전환
                if (apiKeys.size() > 1) {
                    rotateToNextKey();
                } else {
                    // 키가 1개면 30초 대기
                    long waitSeconds = Math.min(attempt * 30L, 180L);
                    log.info("{}초 후 재시도...", waitSeconds);
                    try { Thread.sleep(waitSeconds * 1000L); }
                    catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("재시도 중 인터럽트 발생", ie);
                    }
                }

                if (attempt == maxRetries) {
                    throw new RuntimeException("API 요청 한도 초과. 잠시 후 다시 시도해주세요.", e);
                }
            }
        }
        throw new RuntimeException("API 호출 실패");
    }
}
