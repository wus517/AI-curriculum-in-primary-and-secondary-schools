package com.example.oj.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 课程资源实体（视频 / PPT）
 */
@Data
@TableName("course_resource")
public class CourseResource implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long courseId;

    /**
     * 资源类型：video / ppt
     */
    private String resourceType;

    private String title;

    private String description;

    /**
     * 原始文件名
     */
    private String fileName;

    /**
     * 文件访问地址
     */
    private String fileUrl;

    /**
     * 服务器本地存储路径
     */
    private String filePath;

    /**
     * 文件大小（byte）
     */
    private Long fileSize;

    private String mimeType;

    /**
     * 排序值
     */
    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    @TableLogic
    private Integer isDelete;
}
