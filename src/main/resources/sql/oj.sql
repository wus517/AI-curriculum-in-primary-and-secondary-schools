-- ============================================================
-- OJ 平台建表脚本
-- 使用前请先创建数据库，或直接执行下方 CREATE DATABASE
-- 字符集：utf8mb4
-- ============================================================

DROP DATABASE IF EXISTS `oj`;
CREATE DATABASE `oj` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `oj`;

-- ------------------------------------------------------------
-- 用户表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`    VARCHAR(64)  NOT NULL COMMENT '用户名',
    `password`    VARCHAR(128) NOT NULL COMMENT '密码（加盐 MD5）',
    `role`        VARCHAR(16)  NOT NULL DEFAULT 'user' COMMENT '角色：user / admin',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`   TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除（0 未删除 1 已删除）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='用户表';

-- ------------------------------------------------------------
-- 题目表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `problem`;
CREATE TABLE `problem` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `title`          VARCHAR(128) NOT NULL COMMENT '标题',
    `content`        TEXT         NOT NULL COMMENT '题目描述',
    `input_desc`     TEXT         NULL COMMENT '输入描述',
    `output_desc`    TEXT         NULL COMMENT '输出描述',
    `sample_input`   TEXT         NULL COMMENT '样例输入',
    `sample_output`  TEXT         NULL COMMENT '样例输出',
    `time_limit`     BIGINT       NOT NULL DEFAULT 1000 COMMENT '时间限制（ms）',
    `memory_limit`   BIGINT       NOT NULL DEFAULT 256 COMMENT '内存限制（MB）',
    `difficulty`     INT          NOT NULL DEFAULT 1 COMMENT '难度（1 简单 2 中等 3 困难）',
    `tags`           VARCHAR(512) NULL COMMENT '标签 JSON 字符串，例如 ["数组","DP"]',
    `create_user_id` BIGINT       NOT NULL COMMENT '创建人用户 id',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`      TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_create_user_id` (`create_user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='题目表';

-- ------------------------------------------------------------
-- 测试用例表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `test_case`;
CREATE TABLE `test_case` (
    `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `problem_id`  BIGINT   NOT NULL COMMENT '所属题目 id',
    `input`       TEXT     NOT NULL COMMENT '测试输入',
    `output`      TEXT     NOT NULL COMMENT '期望输出',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`   TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_problem_id` (`problem_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='测试用例表';

-- ------------------------------------------------------------
-- 提交记录表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `submission`;
CREATE TABLE `submission` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`      BIGINT       NOT NULL COMMENT '提交用户 id',
    `problem_id`   BIGINT       NOT NULL COMMENT '题目 id',
    `language`     VARCHAR(16)  NOT NULL COMMENT '语言：java',
    `code`         MEDIUMTEXT   NOT NULL COMMENT '提交代码',
    `status`       VARCHAR(32)  NOT NULL DEFAULT 'PENDING' COMMENT '判题状态',
    `time_used`    BIGINT       NOT NULL DEFAULT 0 COMMENT '耗时（ms）',
    `memory_used`  BIGINT       NOT NULL DEFAULT 0 COMMENT '内存（KB，占位）',
    `error_message` TEXT        NULL COMMENT '错误信息（编译/运行错误）',
    `judge_info`   VARCHAR(512) NULL COMMENT '判题附加信息 JSON',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`    TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_problem_id` (`problem_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='提交记录表';

-- ------------------------------------------------------------
-- 初始化数据：一个管理员账号
-- 密码为 123456 加盐（SALT = oj，见 UserServiceImpl）后 MD5
-- DigestUtil.md5Hex("123456oj") = 3324f7e55db0f9b73ef063df13f4b881
-- ------------------------------------------------------------
INSERT INTO `user` (`username`, `password`, `role`)
VALUES ('admin', '3324f7e55db0f9b73ef063df13f4b881', 'admin');

-- 初始化一道示例题目 + 测试用例（A+B 问题）
INSERT INTO `problem` (`title`, `content`, `input_desc`, `output_desc`, `sample_input`, `sample_output`,
                       `time_limit`, `memory_limit`, `difficulty`, `tags`, `create_user_id`)
VALUES ('A+B 问题',
        '输入两个整数 a 和 b，计算它们的和。',
        '一行，包含两个整数 a 和 b，以空格分隔。',
        '一行，包含一个整数，表示 a+b 的值。',
        '1 2',
        '3',
        1000, 256, 1, '["入门","数学"]', 1);

INSERT INTO `test_case` (`problem_id`, `input`, `output`) VALUES
    (1, '1 2', '3'),
    (1, '10 20', '30'),
    (1, '-5 7', '2');
