package com.example.oj.model.dto.problem;

import com.example.oj.model.entity.TestCase;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 题目修改请求
 */
@Data
public class ProblemUpdateRequest implements Serializable {

    @NotNull(message = "题目 id 不能为空")
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

    private List<String> tags;

    /**
     * 若不为空，则用该列表覆盖原有测试用例
     */
    private List<TestCase> testCases;
}
