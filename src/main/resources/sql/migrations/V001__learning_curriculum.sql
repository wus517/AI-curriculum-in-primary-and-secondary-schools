-- V001 one-time migration. Back up first. Requires existing course/user/problem/resource tables.
-- No DROP. Reapplying fails on existing tables; T07's migration ledger must skip verified versions.
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
