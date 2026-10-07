package com.example.oj.learning.curriculum.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.Instant;

@Data
@TableName("course_lesson")
public class CourseLesson {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long courseId;
    private String title;
    private String objectives;
    private Integer sortOrder;
    private String status;
    private Integer active;
    private Instant createdAt;
    private Instant updatedAt;
}
