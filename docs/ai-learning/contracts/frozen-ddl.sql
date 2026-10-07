-- Contract blueprint only. T01-T05 copy their section into V001-V004.
-- Do not run this file against a live database. No DROP, USE or database recreation.
-- All new DATETIME(3) values are UTC. Historical rows have no cascading deletes.

-- V001: curriculum
CREATE TABLE course_enrollment (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL, course_id BIGINT NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE', joined_at DATETIME(3) NOT NULL,
  updated_at DATETIME(3) NOT NULL,
  UNIQUE KEY uk_enrollment_user_course (user_id, course_id),
  KEY idx_enrollment_course (course_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE course_lesson (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, course_id BIGINT NOT NULL,
  title VARCHAR(128) NOT NULL, objectives TEXT NULL, sort_order INT NOT NULL DEFAULT 0,
  status VARCHAR(16) NOT NULL DEFAULT 'DRAFT', active TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME(3) NOT NULL, updated_at DATETIME(3) NOT NULL,
  KEY idx_lesson_course_order (course_id, active, sort_order, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE lesson_resource (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, lesson_id BIGINT NOT NULL, resource_id BIGINT NOT NULL,
  sort_order INT NOT NULL DEFAULT 0, active TINYINT NOT NULL DEFAULT 1,
  UNIQUE KEY uk_lesson_resource (lesson_id, resource_id), KEY idx_resource_lesson (resource_id, active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE lesson_problem (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, lesson_id BIGINT NOT NULL, problem_id BIGINT NOT NULL,
  sort_order INT NOT NULL DEFAULT 0, active TINYINT NOT NULL DEFAULT 1,
  UNIQUE KEY uk_lesson_problem (lesson_id, problem_id), KEY idx_problem_lesson (problem_id, active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE knowledge_point (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, course_id BIGINT NOT NULL,
  name VARCHAR(128) NOT NULL, description TEXT NULL, active TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME(3) NOT NULL, updated_at DATETIME(3) NOT NULL,
  KEY idx_knowledge_point_course (course_id, active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE lesson_knowledge_point (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, lesson_id BIGINT NOT NULL,
  knowledge_point_id BIGINT NOT NULL, sort_order INT NOT NULL DEFAULT 0, active TINYINT NOT NULL DEFAULT 1,
  UNIQUE KEY uk_lesson_knowledge (lesson_id, knowledge_point_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE problem_knowledge_point (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, course_id BIGINT NOT NULL, problem_id BIGINT NOT NULL,
  knowledge_point_id BIGINT NOT NULL, sort_order INT NOT NULL DEFAULT 0, active TINYINT NOT NULL DEFAULT 1,
  UNIQUE KEY uk_problem_knowledge (course_id, problem_id, knowledge_point_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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

-- V003: knowledge. One stable document, immutable chunks per version.
CREATE TABLE knowledge_document (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, course_id BIGINT NOT NULL, lesson_id BIGINT NULL,
  resource_id BIGINT NULL, source_type VARCHAR(16) NOT NULL,
  title VARCHAR(128) NOT NULL, note_text MEDIUMTEXT NULL,
  version BIGINT NOT NULL DEFAULT 1, active_version BIGINT NULL, content_hash CHAR(64) NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'QUEUED', active TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME(3) NOT NULL, updated_at DATETIME(3) NOT NULL,
  UNIQUE KEY uk_document_resource (resource_id), KEY idx_document_course_active (course_id, active, active_version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE knowledge_chunk (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, document_id BIGINT NOT NULL, version BIGINT NOT NULL,
  ordinal INT NOT NULL, text TEXT NOT NULL, page INT NULL,
  start_seconds DOUBLE NULL, end_seconds DOUBLE NULL,
  citation_id VARCHAR(128) NOT NULL,
  UNIQUE KEY uk_chunk_version_ordinal (document_id, version, ordinal),
  UNIQUE KEY uk_chunk_citation (citation_id), KEY idx_chunk_document_version (document_id, version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE knowledge_ingestion_job (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, document_id BIGINT NOT NULL, version BIGINT NOT NULL,
  content_hash CHAR(64) NOT NULL, source_snapshot MEDIUMTEXT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'QUEUED', attempts INT NOT NULL DEFAULT 0,
  lease_owner VARCHAR(64) NULL, lease_expires_at DATETIME(3) NULL, error VARCHAR(200) NULL,
  created_at DATETIME(3) NOT NULL, updated_at DATETIME(3) NOT NULL,
  UNIQUE KEY uk_ingestion_document_version (document_id, version),
  KEY idx_ingestion_recovery (status, lease_expires_at, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- V004: assistant. Conversation row is the transactional generation lock.
CREATE TABLE ai_conversation (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, user_id BIGINT NOT NULL,
  course_id BIGINT NOT NULL, lesson_id BIGINT NULL, active TINYINT NOT NULL DEFAULT 1,
  active_message_id BIGINT NULL, created_at DATETIME(3) NOT NULL, updated_at DATETIME(3) NOT NULL,
  KEY idx_conversation_user_course (user_id, course_id, active, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE ai_message (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, conversation_id BIGINT NOT NULL,
  client_message_id CHAR(36) NOT NULL, role VARCHAR(16) NOT NULL, mode VARCHAR(16) NOT NULL,
  request_hash CHAR(64) NOT NULL, problem_id BIGINT NULL, submission_id BIGINT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'QUEUED', content MEDIUMTEXT NOT NULL,
  citations JSON NOT NULL, usage_json JSON NULL,
  lease_owner VARCHAR(64) NULL, lease_expires_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL, updated_at DATETIME(3) NOT NULL,
  UNIQUE KEY uk_message_request_role (conversation_id, client_message_id, role),
  KEY idx_message_history (conversation_id, id), KEY idx_message_recovery (status, lease_expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE ai_tool_call (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, conversation_id BIGINT NOT NULL,
  message_id BIGINT NOT NULL, request_id VARCHAR(128) NOT NULL, tool_name VARCHAR(64) NOT NULL,
  status VARCHAR(16) NOT NULL, duration_ms BIGINT NOT NULL,
  arguments_summary VARCHAR(2000) NULL, result_summary VARCHAR(2000) NULL,
  created_at DATETIME(3) NOT NULL, KEY idx_tool_call_message (message_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
