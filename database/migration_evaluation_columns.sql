USE toeic_speaking;

-- evaluations 테이블에 AI 피드백 탭별 컬럼 추가
ALTER TABLE evaluations
  ADD COLUMN score_comment TEXT DEFAULT NULL COMMENT '점수 설명' AFTER score,
  ADD COLUMN target_analysis TEXT DEFAULT NULL COMMENT '목표 분석' AFTER score_comment,
  ADD COLUMN target_tips TEXT DEFAULT NULL COMMENT '목표 달성 팁 (JSON 배열)' AFTER target_analysis,
  ADD COLUMN corrected_answers TEXT DEFAULT NULL COMMENT '수정 답변 (JSON 배열)' AFTER feedback_text,
  ADD COLUMN key_expressions TEXT DEFAULT NULL COMMENT '핵심 표현 (JSON 배열)' AFTER corrected_answers;

-- grammar_corrections 컬럼의 기존 데이터를 corrected_answers로 이동
UPDATE evaluations SET corrected_answers = grammar_corrections WHERE grammar_corrections IS NOT NULL;

-- grammar_corrections 컬럼 삭제
ALTER TABLE evaluations DROP COLUMN grammar_corrections;
