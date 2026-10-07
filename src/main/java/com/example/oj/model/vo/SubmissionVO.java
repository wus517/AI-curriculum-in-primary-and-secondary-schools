package com.example.oj.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 提交记录视图对象
 */
@Data
public class SubmissionVO implements Serializable {

    private Long id;

    private Long userId;

    private Long problemId;

    private String language;

    private String code;

    private String status;

    private Long timeUsed;

    private Long memoryUsed;

    private String errorMessage;

    private String judgeInfo;

    private Date createTime;
}
