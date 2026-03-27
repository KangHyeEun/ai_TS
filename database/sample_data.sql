USE toeic_speaking;

-- ========================================
-- 샘플 데이터: 회원 정보
-- ========================================
INSERT INTO users (email, password, nickname, target_score)
VALUES ('kimstudent@example.com', 'password123', '김학생', 140);

-- ========================================
-- 기존 데이터 초기화 (FK 순서 주의)
-- ========================================
DELETE FROM evaluations;
DELETE FROM user_responses;
DELETE FROM practice_records;
DELETE FROM study_stats;
DELETE FROM questions;

-- AUTO_INCREMENT 리셋
ALTER TABLE questions AUTO_INCREMENT = 1;

-- ========================================
-- 파트별 표준 샘플 문제 (각 1개씩, 총 5개)
-- ========================================

-- Part 1: 지문 읽기 (단독 문항)
INSERT INTO questions (part_number, question_type, content_text, preparation_time, response_time, is_set, set_order)
VALUES (1, '지문 읽기', JSON_OBJECT(
  'text', 'Welcome to the annual technology conference. We are pleased to announce that this year''s event will feature over fifty speakers from around the world. Sessions will cover topics ranging from artificial intelligence to sustainable energy solutions. Please check the schedule posted near the registration desk for room assignments and times.',
  'questionType', '지문 읽기'
), 45, 45, 0, NULL);

-- Part 2: 사진 묘사 (단독 문항)
INSERT INTO questions (part_number, question_type, content_text, image_url, preparation_time, response_time, is_set, set_order)
VALUES (2, '사진 묘사', JSON_OBJECT(
  'text', 'Describe the picture below.',
  'hint', 'A bright, modern co-working space with large windows letting in natural light. In the foreground, a woman wearing a blue blazer is typing on a laptop while talking on the phone. Behind her, two men in casual shirts are standing near a whiteboard, pointing at a chart and discussing. On the right side, a young man with headphones is sitting on a bean bag, reading something on a tablet. Several potted plants are placed around the room, and coffee cups are visible on the desks.',
  'questionType', '사진 묘사'
), 'https://images.pexels.com/photos/3184292/pexels-photo-3184292.jpeg?auto=compress&cs=tinysrgb&w=800', 45, 30, 0, NULL);

-- Part 3: 질문 응답 (세트 문항 — 3문제: 15초, 15초, 30초)
INSERT INTO questions (part_number, question_type, content_text, preparation_time, response_time, is_set, set_order)
VALUES (3, '질문 응답', JSON_OBJECT(
  'questionType', '질문 응답',
  'subQuestions', JSON_ARRAY(
    JSON_OBJECT('text', 'How often do you use public transportation?', 'responseTime', 15),
    JSON_OBJECT('text', 'What do you think are the main advantages of using public transportation compared to driving?', 'responseTime', 15),
    JSON_OBJECT('text', 'Some cities are investing heavily in improving public transportation systems. Do you think this is a good use of public funds? Why or why not?', 'responseTime', 30)
  )
), 3, 15, 1, 1);

-- Part 4: 정보 활용 (세트 문항 — 3문제: 15초, 15초, 30초)
INSERT INTO questions (part_number, question_type, content_text, preparation_time, response_time, is_set, set_order)
VALUES (4, '정보 활용', JSON_OBJECT(
  'questionType', '정보 활용',
  'infoTitle', 'Annual Marketing Workshop',
  'infoDetails', 'Date: March 15\nTime: 9:00 AM - 4:00 PM\nLocation: Conference Room A, 3rd Floor',
  'infoSchedule', JSON_ARRAY(
    JSON_OBJECT('time', '9:00 - 10:30', 'content', 'Lecture: Brand Strategy Overview', 'speaker', 'Sarah Kim'),
    JSON_OBJECT('time', '10:30 - 10:45', 'content', 'Break (Coffee & Refreshments)', 'speaker', '-'),
    JSON_OBJECT('time', '10:45 - 12:00', 'content', 'Presentation: Social Media Trends', 'speaker', 'David Park'),
    JSON_OBJECT('time', '12:00 - 1:00', 'content', 'Lunch (included in registration)', 'speaker', '-'),
    JSON_OBJECT('time', '1:00 - 2:30', 'content', 'Workshop: Customer Engagement', 'speaker', 'Emily Chen'),
    JSON_OBJECT('time', '2:30 - 4:00', 'content', 'Group Discussion: Final Project', 'speaker', '-')
  ),
  'subQuestions', JSON_ARRAY(
    JSON_OBJECT('text', 'What time does the workshop begin and where is it being held?', 'responseTime', 15),
    JSON_OBJECT('text', 'What topic is covered right after the coffee break?', 'responseTime', 15),
    JSON_OBJECT('text', 'I heard the group project is in the morning. Is that correct?', 'responseTime', 30)
  )
), 45, 30, 1, 1);

-- Part 5: 의견 제시 (단독 문항)
INSERT INTO questions (part_number, question_type, content_text, preparation_time, response_time, is_set, set_order)
VALUES (5, '의견 제시', JSON_OBJECT(
  'text', 'Some people believe that employees should be required to work in the office every day, while others think that remote work should be an option. Which do you prefer and why? Give specific reasons and examples to support your opinion.',
  'questionType', '의견 제시'
), 30, 60, 0, NULL);
