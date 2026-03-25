CREATE DATABASE IF NOT EXISTS toeic_speaking
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE toeic_speaking;

CREATE TABLE IF NOT EXISTS practice_records (
  id INT AUTO_INCREMENT PRIMARY KEY,
  part_id INT NOT NULL,
  part_title VARCHAR(100) NOT NULL,
  question_idx INT NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
