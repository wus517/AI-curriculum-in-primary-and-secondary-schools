package com.example.oj.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.oj.model.entity.TestCase;
import org.apache.ibatis.annotations.Mapper;

/**
 * 测试用例数据库操作
 */
@Mapper
public interface TestCaseMapper extends BaseMapper<TestCase> {
}
