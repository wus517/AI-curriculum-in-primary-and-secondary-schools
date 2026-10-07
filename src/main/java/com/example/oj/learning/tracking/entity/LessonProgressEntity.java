package com.example.oj.learning.tracking.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.Instant;

@Data
@TableName("lesson_progress")
public class LessonProgressEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long lessonId;
    private Long courseId;
    private String resourceProgress;
    private Integer selfReportedComplete;
    private Instant updatedAt;
}
