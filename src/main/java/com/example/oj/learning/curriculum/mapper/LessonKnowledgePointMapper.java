package com.example.oj.learning.curriculum.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.oj.learning.curriculum.entity.LessonKnowledgePoint;

public interface LessonKnowledgePointMapper extends BaseMapper<LessonKnowledgePoint> {

    @org.apache.ibatis.annotations.Insert("""
        INSERT INTO lesson_knowledge_point (lesson_id, knowledge_point_id, sort_order, active)
        VALUES (#{lessonId}, #{knowledgePointId}, #{sortOrder}, 1)
        ON DUPLICATE KEY UPDATE sort_order = VALUES(sort_order), active = 1
        """)
    int bind(@org.apache.ibatis.annotations.Param("lessonId") long lessonId,
             @org.apache.ibatis.annotations.Param("knowledgePointId") long knowledgePointId,
             @org.apache.ibatis.annotations.Param("sortOrder") int sortOrder);
}
