package com.example.oj.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 提交记录实体
 */
@Data
@TableName("submission")
public class Submission implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long problemId;

    private Long courseId;

    private Long lessonId;

    private String assistanceLevel;

    private Long judgedRevision;

    /**
     * 编程语言：java
     */
    private String language;

    private String code;

    /**
     * 判题状态，见 JudgeStatusEnum
     */
    private String status;

    /**
     * 耗时（ms）
     */
    private Long timeUsed;

    /**
     * 内存（KB，占位字段）
     */
    private Long memoryUsed;

    private String errorMessage;

    /**
     * 判题附加信息 JSON
     */
    private String judgeInfo;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    @TableLogic
    private Integer isDelete;
}
