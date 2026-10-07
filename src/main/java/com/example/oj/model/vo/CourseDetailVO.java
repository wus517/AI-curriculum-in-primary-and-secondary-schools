package com.example.oj.model.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;

/**
 * 课程详情视图对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CourseDetailVO extends CourseVO implements Serializable {

    /**
     * 视频资源列表
     */
    private List<CourseResourceVO> videoList;

    /**
     * PPT 资源列表
     */
    private List<CourseResourceVO> pptList;

    /**
     * 关联题目列表
     */
    private List<ProblemVO> problemList;
}
