package com.example.oj.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.oj.model.entity.Problem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 题目数据库操作
 */
@Mapper
public interface ProblemMapper extends BaseMapper<Problem> {
}
