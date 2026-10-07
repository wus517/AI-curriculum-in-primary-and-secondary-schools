package com.example.oj.model.dto.problem;

import com.example.oj.model.entity.TestCase;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 题目新增请求
 */
@Data
public class ProblemAddRequest implements Serializable {

    @NotBlank(message = "标题不能为空")
    private String title;

    @NotBlank(message = "题目描述不能为空")
    private String content;

    private String inputDesc;

    private String outputDesc;

    private String sampleInput;

    private String sampleOutput;

    @NotNull(message = "时间限制不能为空")
    private Long timeLimit;

    @NotNull(message = "内存限制不能为空")
    private Long memoryLimit;

    private Integer difficulty;

    /**
     * 标签列表，后端会转成 JSON 字符串存储
     */
    private List<String> tags;

    /**
     * 测试用例（与题目一起创建）
     */
    private List<TestCase> testCases;
}
