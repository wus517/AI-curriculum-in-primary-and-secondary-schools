package com.example.oj.learning.tracking.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.oj.learning.tracking.entity.LearningEventEntity;

public interface LearningEventEntityMapper extends BaseMapper<LearningEventEntity> {

    @org.apache.ibatis.annotations.Select("""
        SELECT id FROM learning_event
        WHERE (processing_status IN ('PENDING', 'FAILED')
          OR (processing_status = 'PROCESSING' AND lease_expires_at < #{now}))
          AND attempts < 3 ORDER BY id LIMIT #{limit}
        """)
    java.util.List<Long> pending(@org.apache.ibatis.annotations.Param("now") java.time.Instant now,
                                @org.apache.ibatis.annotations.Param("limit") int limit);
}
