package com.example.oj.learning.curriculum.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.oj.learning.curriculum.entity.LessonResource;

public interface LessonResourceMapper extends BaseMapper<LessonResource> {

    @org.apache.ibatis.annotations.Insert("""
        INSERT INTO lesson_resource (lesson_id, resource_id, sort_order, active)
        VALUES (#{lessonId}, #{resourceId}, #{sortOrder}, 1)
        ON DUPLICATE KEY UPDATE sort_order = VALUES(sort_order), active = 1
        """)
    int bind(@org.apache.ibatis.annotations.Param("lessonId") long lessonId,
             @org.apache.ibatis.annotations.Param("resourceId") long resourceId,
             @org.apache.ibatis.annotations.Param("sortOrder") int sortOrder);
}
