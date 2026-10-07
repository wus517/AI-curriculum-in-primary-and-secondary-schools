-- ============================================================
-- 课程管理模块建表脚本
-- 在已存在的 oj 库中新增表，纯增量，不影响现有表
-- 执行： mysql -uroot -p --default-character-set=utf8mb4 oj < course.sql
--        或 docker exec -i mysql mysql -uroot -proot --default-character-set=utf8mb4 oj < course.sql
-- ============================================================
USE `oj`;

-- ------------------------------------------------------------
-- 课程主表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `course`;
CREATE TABLE `course` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `title`       VARCHAR(128) NOT NULL COMMENT '课程标题',
    `description` TEXT         NULL COMMENT '课程简介',
    `cover_url`   VARCHAR(512) NULL COMMENT '封面访问地址',
    `difficulty`  VARCHAR(32)  NULL COMMENT '难度：beginner/intermediate/advanced',
    `tags`        VARCHAR(512) NULL COMMENT '标签 JSON 字符串',
    `teacher_id`  BIGINT       NOT NULL COMMENT '创建人/教师用户 id',
    `status`      VARCHAR(16)  NOT NULL DEFAULT 'draft' COMMENT '状态：draft/published/offline',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`   TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_teacher_id` (`teacher_id`),
    KEY `idx_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='课程表';

-- ------------------------------------------------------------
-- 课程资源表（视频 / PPT）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `course_resource`;
CREATE TABLE `course_resource` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `course_id`     BIGINT       NOT NULL COMMENT '所属课程 id',
    `resource_type` VARCHAR(16)  NOT NULL COMMENT '资源类型：video / ppt',
    `title`         VARCHAR(128) NULL COMMENT '资源标题',
    `description`   TEXT         NULL COMMENT '资源描述',
    `file_name`     VARCHAR(255) NULL COMMENT '原始文件名',
    `file_url`      VARCHAR(512) NOT NULL COMMENT '文件访问地址',
    `file_path`     VARCHAR(512) NULL COMMENT '服务器本地存储路径',
    `file_size`     BIGINT       NOT NULL DEFAULT 0 COMMENT '文件大小（byte）',
    `mime_type`     VARCHAR(128) NULL COMMENT '文件类型',
    `sort_order`    INT          NOT NULL DEFAULT 0 COMMENT '排序值',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_course_id` (`course_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='课程资源表';

-- ------------------------------------------------------------
-- 课程题目关联表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `course_problem`;
CREATE TABLE `course_problem` (
    `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `course_id`   BIGINT   NOT NULL COMMENT '课程 id',
    `problem_id`  BIGINT   NOT NULL COMMENT '题目 id',
    `sort_order`  INT      NOT NULL DEFAULT 0 COMMENT '题目排序',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`   TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_course_id` (`course_id`),
    KEY `idx_problem_id` (`problem_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='课程题目关联表';
