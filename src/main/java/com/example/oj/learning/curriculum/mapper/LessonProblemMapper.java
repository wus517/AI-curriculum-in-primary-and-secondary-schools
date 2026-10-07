package com.example.oj.learning.curriculum.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.oj.learning.curriculum.entity.LessonProblem;

public interface LessonProblemMapper extends BaseMapper<LessonProblem> {

    @org.apache.ibatis.annotations.Insert("""
        INSERT INTO lesson_problem (lesson_id, problem_id, sort_order, active)
        VALUES (#{lessonId}, #{problemId}, #{sortOrder}, 1)
        ON DUPLICATE KEY UPDATE sort_order = VALUES(sort_order), active = 1
        """)
    int bind(@org.apache.ibatis.annotations.Param("lessonId") long lessonId,
             @org.apache.ibatis.annotations.Param("problemId") long problemId,
             @org.apache.ibatis.annotations.Param("sortOrder") int sortOrder);
}
