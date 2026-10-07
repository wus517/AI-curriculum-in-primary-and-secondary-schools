package com.example.oj.learning.curriculum.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.oj.learning.curriculum.entity.ProblemKnowledgePoint;

public interface ProblemKnowledgePointMapper extends BaseMapper<ProblemKnowledgePoint> {

    @org.apache.ibatis.annotations.Insert("""
        INSERT INTO problem_knowledge_point (course_id, problem_id, knowledge_point_id, sort_order, active)
        VALUES (#{courseId}, #{problemId}, #{knowledgePointId}, #{sortOrder}, 1)
        ON DUPLICATE KEY UPDATE sort_order = VALUES(sort_order), active = 1
        """)
    int bind(@org.apache.ibatis.annotations.Param("courseId") long courseId,
             @org.apache.ibatis.annotations.Param("problemId") long problemId,
             @org.apache.ibatis.annotations.Param("knowledgePointId") long knowledgePointId,
             @org.apache.ibatis.annotations.Param("sortOrder") int sortOrder);
}
