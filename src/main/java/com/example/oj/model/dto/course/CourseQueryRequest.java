package com.example.oj.model.dto.course;

import lombok.Data;

import java.io.Serializable;

/**
 * 课程分页查询请求
 */
@Data
public class CourseQueryRequest implements Serializable {

    private Long pageNum;

    private Long pageSize;

    /**
     * 关键字，匹配 title / description
     */
    private String keyword;

    private String difficulty;

    private String status;
}
