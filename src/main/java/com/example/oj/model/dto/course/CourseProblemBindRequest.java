package com.example.oj.model.dto.course;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 课程绑定题目请求
 */
@Data
public class CourseProblemBindRequest implements Serializable {

    @NotNull(message = "课程 id 不能为空")
    private Long courseId;

    @NotEmpty(message = "题目 id 列表不能为空")
    private List<Long> problemIdList;
}
