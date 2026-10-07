package com.example.oj.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.oj.model.entity.Course;
import org.apache.ibatis.annotations.Mapper;

/**
 * 课程数据库操作
 */
@Mapper
public interface CourseMapper extends BaseMapper<Course> {
}
