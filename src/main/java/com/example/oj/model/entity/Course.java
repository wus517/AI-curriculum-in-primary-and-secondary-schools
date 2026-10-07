package com.example.oj.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 课程实体
 */
@Data
@TableName("course")
public class Course implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String description;

    /**
     * 封面访问地址
     */
    private String coverUrl;

    /**
     * 难度：beginner / intermediate / advanced
     */
    private String difficulty;

    /**
     * 标签 JSON 字符串，例如 ["Java","基础"]
     */
    private String tags;

    /**
     * 创建人/教师用户 id
     */
    private Long teacherId;

    /**
     * 状态：draft / published / offline
     */
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    @TableLogic
    private Integer isDelete;
}
