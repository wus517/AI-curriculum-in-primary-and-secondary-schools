package com.example.oj.sandbox.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 代码执行请求
 */
@Data
public class ExecuteCodeRequest implements Serializable {

    /**
     * 用户代码
     */
    private String code;

    /**
     * 语言：java
     */
    private String language;

    /**
     * 测试用例输入列表
     */
    private List<String> inputList;

    /**
     * 时间限制（ms）
     */
    private Long timeLimit;

    /**
     * 内存限制（MB，第一版占位）
     */
    private Long memoryLimit;
}
