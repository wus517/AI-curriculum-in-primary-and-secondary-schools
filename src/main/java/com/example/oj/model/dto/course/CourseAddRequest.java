package com.example.oj.model.dto.course;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 课程新增请求
 */
@Data
public class CourseAddRequest implements Serializable {

    @NotBlank(message = "课程标题不能为空")
    private String title;

    private String description;

    /**
     * 难度：beginner / intermediate / advanced
     */
    private String difficulty;

    /**
     * 标签列表，服务层转 JSON 存储
     */
    private List<String> tags;
}
