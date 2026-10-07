package com.example.oj.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.oj.model.entity.Course;
import com.example.oj.model.entity.User;
import com.example.oj.model.dto.course.CourseAddRequest;
import com.example.oj.model.dto.course.CourseQueryRequest;
import com.example.oj.model.dto.course.CourseProblemBindRequest;
import com.example.oj.model.dto.course.CourseUpdateRequest;
import com.example.oj.model.vo.CourseDetailVO;
import com.example.oj.model.vo.CourseVO;
import com.example.oj.model.vo.ProblemVO;

import java.util.List;

/**
 * 课程服务
 */
public interface CourseService extends IService<Course> {

    /**
     * 创建课程，返回课程 id
     */
    long addCourse(CourseAddRequest request, Long teacherId);

    /**
     * 修改课程
     */
    boolean updateCourse(CourseUpdateRequest request);

    /**
     * 逻辑删除课程（同步删除其资源与题目关联）
     */
    boolean deleteCourse(long id);

    /**
     * 查询课程基本信息
     */
    CourseVO getCourseById(long id);

    /**
     * 分页查询课程列表
     */
    IPage<CourseVO> listCourseByPage(CourseQueryRequest request);

    /**
     * 查询课程详情（含视频/PPT/题目）
     */
    CourseDetailVO getCourseDetail(long id);

    /**
     * 给课程绑定题目（去重）
     */
    void bindProblems(CourseProblemBindRequest request);

    /**
     * 解绑单个题目
     */
    boolean unbindProblem(long courseId, long problemId);

    /**
     * 查询课程下题目列表（按 sortOrder 升序）
     */
    List<ProblemVO> listCourseProblems(long courseId);

    /**
     * 校验当前用户对课程的操作权限（创建者或 admin），不通过则抛异常
     */
    void checkCourseAuth(long courseId, User loginUser);
}
