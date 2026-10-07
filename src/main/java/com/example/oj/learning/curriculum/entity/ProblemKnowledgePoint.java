package com.example.oj.learning.curriculum.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("problem_knowledge_point")
public class ProblemKnowledgePoint {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long courseId;
    private Long problemId;
    private Long knowledgePointId;
    private Integer sortOrder;
    private Integer active;
}
