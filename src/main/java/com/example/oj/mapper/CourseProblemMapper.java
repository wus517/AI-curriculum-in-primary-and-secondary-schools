package com.example.oj.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.oj.model.entity.CourseProblem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 课程题目关联数据库操作
 */
@Mapper
public interface CourseProblemMapper extends BaseMapper<CourseProblem> {
}
