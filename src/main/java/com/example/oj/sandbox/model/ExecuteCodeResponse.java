package com.example.oj.sandbox.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 代码执行响应
 */
@Data
public class ExecuteCodeResponse implements Serializable {

    /**
     * 每个测试用例对应的输出（与 inputList 一一对应）
     */
    private List<String> outputList;

    /**
     * 提示信息（编译/错误信息等）
     */
    private String message;

    /**
     * 执行状态，取值见 JudgeStatusEnum：
     * ACCEPTED（运行成功）、COMPILE_ERROR、RUNTIME_ERROR、
     * TIME_LIMIT_EXCEEDED、SYSTEM_ERROR
     * 注：ACCEPTED 仅表示执行成功，不代表答案正确，需由判题服务比较输出
     */
    private String status;

    /**
     * 最大耗时（ms）
     */
    private Long timeUsed;

    /**
     * 最大内存（KB，第一版占位）
     */
    private Long memoryUsed;
}
