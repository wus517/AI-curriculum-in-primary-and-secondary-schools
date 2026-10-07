-- One-time migration after V001. Back up existing data and verify the migration ledger first.
-- Nullable submission sources preserve independent submissions; UNKNOWN is not an assertion of no help.
-- V002: tracking. Existing submissions preserve nullable course/lesson source.
ALTER TABLE submission ADD COLUMN course_id BIGINT NULL;
ALTER TABLE submission ADD COLUMN lesson_id BIGINT NULL;
ALTER TABLE submission ADD COLUMN assistance_level VARCHAR(16) NOT NULL DEFAULT 'UNKNOWN';
ALTER TABLE submission ADD COLUMN judged_revision BIGINT NOT NULL DEFAULT 0;
CREATE INDEX idx_submission_user_course ON submission (user_id, course_id, id);

CREATE TABLE learning_event (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, event_id VARCHAR(128) NOT NULL,
  user_id BIGINT NOT NULL, course_id BIGINT NULL, lesson_id BIGINT NULL,
  type VARCHAR(32) NOT NULL, source VARCHAR(16) NOT NULL, occurred_at DATETIME(3) NOT NULL,
  payload JSON NOT NULL, payload_hash CHAR(64) NOT NULL,
  processing_status VARCHAR(16) NOT NULL DEFAULT 'PENDING', attempts INT NOT NULL DEFAULT 0,
  lease_owner VARCHAR(64) NULL, lease_expires_at DATETIME(3) NULL, processed_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL, error VARCHAR(200) NULL,
  UNIQUE KEY uk_learning_event_id (event_id), KEY idx_event_user_course_time (user_id, course_id, occurred_at),
  KEY idx_event_recovery (processing_status, lease_expires_at, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE lesson_progress (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, user_id BIGINT NOT NULL, lesson_id BIGINT NOT NULL,
  course_id BIGINT NOT NULL, resource_progress JSON NOT NULL,
  self_reported_complete TINYINT NOT NULL DEFAULT 0, updated_at DATETIME(3) NOT NULL,
  UNIQUE KEY uk_progress_user_lesson (user_id, lesson_id), KEY idx_progress_user_course (user_id, course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
