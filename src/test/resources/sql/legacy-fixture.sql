-- Isolated H2 test fixture derived from the existing entity/SQL fields. No live database use.
CREATE TABLE `user` (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(64) NOT NULL UNIQUE, password VARCHAR(128) NOT NULL,
 role VARCHAR(16) DEFAULT 'user', create_time DATETIME, update_time DATETIME, is_delete TINYINT DEFAULT 0
);
CREATE TABLE course (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, title VARCHAR(128) NOT NULL, description TEXT, cover_url VARCHAR(512),
 difficulty VARCHAR(32), tags VARCHAR(512), teacher_id BIGINT NOT NULL, status VARCHAR(16) DEFAULT 'draft',
 create_time DATETIME, update_time DATETIME, is_delete TINYINT DEFAULT 0
);
CREATE TABLE course_resource (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, course_id BIGINT NOT NULL, resource_type VARCHAR(16), title VARCHAR(128),
 description TEXT, file_name VARCHAR(255), file_url VARCHAR(512), file_path VARCHAR(512), file_size BIGINT,
 mime_type VARCHAR(128), sort_order INT DEFAULT 0, create_time DATETIME, update_time DATETIME, is_delete TINYINT DEFAULT 0
);
CREATE TABLE course_problem (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, course_id BIGINT NOT NULL, problem_id BIGINT NOT NULL, sort_order INT DEFAULT 0,
 create_time DATETIME, update_time DATETIME, is_delete TINYINT DEFAULT 0
);
CREATE TABLE problem (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, title VARCHAR(128) NOT NULL, content TEXT NOT NULL, input_desc TEXT,
 output_desc TEXT, sample_input TEXT, sample_output TEXT, time_limit BIGINT DEFAULT 1000, memory_limit BIGINT DEFAULT 256,
 difficulty INT DEFAULT 1, tags VARCHAR(512), create_user_id BIGINT NOT NULL, create_time DATETIME,
 update_time DATETIME, is_delete TINYINT DEFAULT 0
);
CREATE TABLE test_case (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, problem_id BIGINT NOT NULL, input TEXT NOT NULL, output TEXT NOT NULL,
 create_time DATETIME, update_time DATETIME, is_delete TINYINT DEFAULT 0
);
CREATE TABLE submission (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT NOT NULL, problem_id BIGINT NOT NULL, language VARCHAR(16),
 code MEDIUMTEXT, status VARCHAR(32), time_used BIGINT DEFAULT 0, memory_used BIGINT DEFAULT 0,
 error_message TEXT, judge_info VARCHAR(512), create_time DATETIME, update_time DATETIME, is_delete TINYINT DEFAULT 0
);
