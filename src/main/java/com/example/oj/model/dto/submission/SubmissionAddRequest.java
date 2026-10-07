package com.example.oj.model.dto.submission;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 代码提交请求
 */
@Data
public class SubmissionAddRequest implements Serializable {

    @NotNull(message = "题目 id 不能为空")
    private Long problemId;

    @NotBlank(message = "语言不能为空")
    private String language;

    @NotBlank(message = "代码不能为空")
    private String code;
}
