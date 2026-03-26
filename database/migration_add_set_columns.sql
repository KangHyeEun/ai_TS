USE toeic_speaking;

-- questions 테이블에 세트 문항 관련 컬럼 추가
ALTER TABLE questions
  ADD COLUMN is_set TINYINT(1) NOT NULL DEFAULT 0 COMMENT '세트 문항 여부 (0=단독, 1=세트)' AFTER response_time,
  ADD COLUMN set_order TINYINT DEFAULT NULL COMMENT '세트 내 출제 순서 (1,2,3...)' AFTER is_set;

-- 기존 Part 3, 4 문항을 세트로 업데이트 (content_text에 subQuestions가 포함된 경우)
UPDATE questions SET is_set = 1, set_order = 1 WHERE part_number IN (3, 4);

-- user_responses 테이블에 풀이 횟수 컬럼 추가
ALTER TABLE user_responses
  ADD COLUMN attempt_number INT NOT NULL DEFAULT 1 COMMENT '해당 문제 풀이 횟수 (1=첫 시도, 2=두 번째...)' AFTER practice_mode;

-- practice_records 테이블 구조 변경 (user_id, question_id 추가, question_idx 제거)
ALTER TABLE practice_records
  ADD COLUMN user_id INT NOT NULL DEFAULT 1 COMMENT '사용자 ID' AFTER id,
  ADD COLUMN question_id INT DEFAULT NULL COMMENT '문제 ID' AFTER user_id,
  ADD COLUMN practice_mode VARCHAR(20) NOT NULL DEFAULT 'REAL' COMMENT '연습 모드' AFTER part_title,
  DROP COLUMN question_idx;

-- evaluations 테이블에 잘한점 컬럼 추가
ALTER TABLE evaluations
  ADD COLUMN strengths_text TEXT DEFAULT NULL COMMENT '잘한 점 (JSON 배열)' AFTER score;
