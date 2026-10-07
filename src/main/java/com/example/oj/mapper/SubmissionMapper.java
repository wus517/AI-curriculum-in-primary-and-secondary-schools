package com.example.oj.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.oj.model.entity.Submission;
import org.apache.ibatis.annotations.Mapper;

/**
 * 提交记录数据库操作
 */
@Mapper
public interface SubmissionMapper extends BaseMapper<Submission> {
}
