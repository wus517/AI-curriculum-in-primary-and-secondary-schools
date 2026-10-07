package com.example.oj.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 课程视图对象
 */
@Data
public class CourseVO implements Serializable {

    private Long id;

    private String title;

    private String description;

    private String coverUrl;

    private String difficulty;

    /**
     * 标签列表（由存储的 JSON 字符串解析得到）
     */
    private List<String> tags;

    private Long teacherId;

    /**
     * 教师用户名
     */
    private String teacherName;

    private String status;

    private Date createTime;

    private Date updateTime;
}
