package com.example.oj.model.dto.course;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 课程修改请求
 */
@Data
public class CourseUpdateRequest implements Serializable {

    @NotNull(message = "课程 id 不能为空")
    private Long id;

    private String title;

    private String description;

    private String difficulty;

    private List<String> tags;

    /**
     * 状态：draft / published / offline
     */
    private String status;
}
