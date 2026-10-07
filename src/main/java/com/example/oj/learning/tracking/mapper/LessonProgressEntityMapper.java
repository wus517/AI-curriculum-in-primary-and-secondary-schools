package com.example.oj.learning.tracking.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.oj.learning.tracking.entity.LessonProgressEntity;

public interface LessonProgressEntityMapper extends BaseMapper<LessonProgressEntity> {

    @org.apache.ibatis.annotations.Insert("""
        INSERT INTO lesson_progress (user_id, lesson_id, course_id, resource_progress, self_reported_complete, updated_at)
        VALUES (#{userId}, #{lessonId}, #{courseId}, '[]', 0, #{now})
        ON DUPLICATE KEY UPDATE id = id
        """)
    int ensure(@org.apache.ibatis.annotations.Param("userId") long userId,
               @org.apache.ibatis.annotations.Param("lessonId") long lessonId,
               @org.apache.ibatis.annotations.Param("courseId") long courseId,
               @org.apache.ibatis.annotations.Param("now") java.time.Instant now);
}
