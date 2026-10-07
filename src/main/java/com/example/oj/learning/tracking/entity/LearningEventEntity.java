package com.example.oj.learning.tracking.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.Instant;

@Data
@TableName("learning_event")
public class LearningEventEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String eventId;
    private Long userId;
    private Long courseId;
    private Long lessonId;
    private String type;
    private String source;
    private Instant occurredAt;
    private String payload;
    private String payloadHash;
    private String processingStatus;
    private Integer attempts;
    private String leaseOwner;
    private Instant leaseExpiresAt;
    private Instant processedAt;
    private Instant createdAt;
    private String error;
}
