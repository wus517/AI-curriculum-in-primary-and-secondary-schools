package com.example.oj.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 题目视图对象
 */
@Data
public class ProblemVO implements Serializable {

    private Long id;

    private String title;

    private String content;

    private String inputDesc;

    private String outputDesc;

    private String sampleInput;

    private String sampleOutput;

    private Long timeLimit;

    private Long memoryLimit;

    private Integer difficulty;

    /**
     * 标签列表（由存储的 JSON 字符串解析得到）
     */
    private List<String> tags;

    private Long createUserId;

    private Date createTime;
}
