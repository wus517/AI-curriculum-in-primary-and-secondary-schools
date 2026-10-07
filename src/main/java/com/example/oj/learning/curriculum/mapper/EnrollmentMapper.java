package com.example.oj.learning.curriculum.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.oj.learning.curriculum.entity.Enrollment;

public interface EnrollmentMapper extends BaseMapper<Enrollment> {

    @org.apache.ibatis.annotations.Insert("""
        INSERT INTO course_enrollment (user_id, course_id, status, joined_at, updated_at)
        VALUES (#{userId}, #{courseId}, 'ACTIVE', #{now}, #{now})
        ON DUPLICATE KEY UPDATE
        joined_at = CASE WHEN status = 'INACTIVE' THEN VALUES(joined_at) ELSE joined_at END,
        status = 'ACTIVE', updated_at = VALUES(updated_at)
        """)
    int join(@org.apache.ibatis.annotations.Param("userId") long userId,
             @org.apache.ibatis.annotations.Param("courseId") long courseId,
             @org.apache.ibatis.annotations.Param("now") java.time.Instant now);
}
