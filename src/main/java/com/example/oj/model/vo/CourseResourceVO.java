package com.example.oj.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 课程资源视图对象
 */
@Data
public class CourseResourceVO implements Serializable {

    private Long id;

    private Long courseId;

    private String resourceType;

    private String title;

    private String description;

    private String fileName;

    private String fileUrl;

    private Long fileSize;

    private String mimeType;

    private Integer sortOrder;

    private Date createTime;
}
