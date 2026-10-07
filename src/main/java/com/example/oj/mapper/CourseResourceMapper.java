package com.example.oj.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.oj.model.entity.CourseResource;
import org.apache.ibatis.annotations.Mapper;

/**
 * 课程资源数据库操作
 */
@Mapper
public interface CourseResourceMapper extends BaseMapper<CourseResource> {
}
