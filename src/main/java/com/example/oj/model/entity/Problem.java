package com.example.oj.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 题目实体
 */
@Data
@TableName("problem")
public class Problem implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String content;

    private String inputDesc;

    private String outputDesc;

    private String sampleInput;

    private String sampleOutput;

    /**
     * 时间限制（ms）
     */
    private Long timeLimit;

    /**
     * 内存限制（MB）
     */
    private Long memoryLimit;

    /**
     * 难度：1 简单 2 中等 3 困难
     */
    private Integer difficulty;

    /**
     * 标签 JSON 字符串，例如 ["数组","DP"]
     */
    private String tags;

    private Long createUserId;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    @TableLogic
    private Integer isDelete;
}
