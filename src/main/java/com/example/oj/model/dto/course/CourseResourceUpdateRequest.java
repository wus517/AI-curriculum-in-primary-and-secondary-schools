package com.example.oj.model.dto.course;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 课程资源信息修改请求（只改标题/描述/排序，不涉及文件）
 */
@Data
public class CourseResourceUpdateRequest implements Serializable {

    @NotNull(message = "资源 id 不能为空")
    private Long id;

    private String title;

    private String description;

    private Integer sortOrder;
}
