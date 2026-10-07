package com.example.oj.learning.curriculum.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("lesson_problem")
public class LessonProblem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long lessonId;
    private Long problemId;
    private Integer sortOrder;
    private Integer active;
}
