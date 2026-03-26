CREATE DATABASE IF NOT EXISTS toeic_speaking
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE toeic_speaking;

-- ========================================
-- 1. 회원 정보 테이블
-- ========================================
CREATE TABLE IF NOT EXISTS users (
  user_id INT AUTO_INCREMENT PRIMARY KEY,
  email VARCHAR(100) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  nickname VARCHAR(50) NOT NULL,
  target_score INT DEFAULT 0 COMMENT '목표 점수 (0~200)',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- ========================================
-- 2. 문항 콘텐츠 테이블
-- ========================================
CREATE TABLE IF NOT EXISTS questions (
  question_id INT AUTO_INCREMENT PRIMARY KEY,
  part_number TINYINT NOT NULL COMMENT 'Part 1~5',
  question_type VARCHAR(50) NOT NULL COMMENT '지문 읽기, 사진 묘사, 질문 응답, 정보 활용, 의견 제시',
  content_text TEXT NOT NULL COMMENT '문제 지문',
  image_url VARCHAR(500) DEFAULT NULL COMMENT '사진 묘사용 이미지 경로',
  audio_url VARCHAR(500) DEFAULT NULL COMMENT '듣기 지문용 오디오 경로',
  preparation_time INT NOT NULL COMMENT '준비 시간(초)',
  response_time INT NOT NULL COMMENT '응답 시간(초)',
  is_set TINYINT(1) NOT NULL DEFAULT 0 COMMENT '세트 문항 여부 (0=단독, 1=세트)',
  set_order TINYINT DEFAULT NULL COMMENT '세트 내 출제 순서 (1,2,3...)'
);

-- ========================================
-- 3. 사용자 응답 테이블
-- ========================================
-- practice_mode 코드:
-- MOCK    = 모의고사
-- REAL    = 파트별 연습 - 실전 연습
-- FREE    = 파트별 연습 - 자유 연습
-- KOREAN  = 파트별 연습 - 한글 연습
CREATE TABLE IF NOT EXISTS user_responses (
  response_id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  question_id INT NOT NULL,
  practice_mode VARCHAR(20) NOT NULL DEFAULT 'REAL' COMMENT '연습 모드 (MOCK/REAL/FREE/KOREAN)',
  attempt_number INT NOT NULL DEFAULT 1 COMMENT '해당 문제 풀이 횟수 (1=첫 시도, 2=두 번째...)',
  audio_file_path VARCHAR(500) DEFAULT NULL COMMENT '녹음 파일 경로',
  stt_text TEXT DEFAULT NULL COMMENT '음성→텍스트 변환 결과',
  text_answer TEXT DEFAULT NULL COMMENT '텍스트 직접 입력 답변',
  submitted_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '답변 제출 일시',
  FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
  FOREIGN KEY (question_id) REFERENCES questions(question_id) ON DELETE CASCADE
);

-- ========================================
-- 4. AI/전문가 평가 테이블
-- ========================================
CREATE TABLE IF NOT EXISTS evaluations (
  evaluation_id INT AUTO_INCREMENT PRIMARY KEY,
  response_id INT NOT NULL,
  score INT DEFAULT NULL COMMENT '점수 (0~200)',
  strengths_text TEXT DEFAULT NULL COMMENT '잘한 점 (JSON 배열)',
  feedback_text TEXT DEFAULT NULL COMMENT '개선할 점 (JSON 배열)',
  grammar_corrections TEXT DEFAULT NULL COMMENT '문법 교정 내용',
  evaluated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (response_id) REFERENCES user_responses(response_id) ON DELETE CASCADE
);

-- ========================================
-- 5. 연습 기록 테이블
-- ========================================
CREATE TABLE IF NOT EXISTS practice_records (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL COMMENT '사용자 ID',
  question_id INT DEFAULT NULL COMMENT '문제 ID (questions 테이블 FK)',
  part_id INT NOT NULL COMMENT 'Part 번호 (1~5)',
  part_title VARCHAR(100) NOT NULL COMMENT 'Part 제목',
  practice_mode VARCHAR(20) NOT NULL DEFAULT 'REAL' COMMENT '연습 모드',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
  FOREIGN KEY (question_id) REFERENCES questions(question_id) ON DELETE SET NULL
);

-- ========================================
-- 6. 학습 통계 테이블
-- ========================================
CREATE TABLE IF NOT EXISTS study_stats (
  stat_id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  study_date DATE NOT NULL,
  completed_questions_count INT DEFAULT 0,
  average_score DECIMAL(5,2) DEFAULT 0.00,
  UNIQUE KEY uq_user_date (user_id, study_date),
  FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);
