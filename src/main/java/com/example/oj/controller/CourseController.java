package com.example.oj.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.oj.common.BaseResponse;
import com.example.oj.common.ResultUtils;
import com.example.oj.model.entity.User;
import com.example.oj.model.dto.course.CourseAddRequest;
import com.example.oj.model.dto.course.CourseProblemBindRequest;
import com.example.oj.model.dto.course.CourseQueryRequest;
import com.example.oj.model.dto.course.CourseResourceUpdateRequest;
import com.example.oj.model.dto.course.CourseUpdateRequest;
import com.example.oj.model.vo.CourseDetailVO;
import com.example.oj.model.vo.CourseResourceVO;
import com.example.oj.model.vo.CourseVO;
import com.example.oj.model.vo.ProblemVO;
import com.example.oj.service.CourseResourceService;
import com.example.oj.service.CourseService;
import com.example.oj.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 课程接口
 */
@RestController
@RequestMapping("/api/course")
@Tag(name = "课程接口")
public class CourseController {

    private final CourseService courseService;
    private final CourseResourceService courseResourceService;
    private final UserService userService;

    public CourseController(CourseService courseService,
                            CourseResourceService courseResourceService,
                            UserService userService) {
        this.courseService = courseService;
        this.courseResourceService = courseResourceService;
        this.userService = userService;
    }

    // ==================== 课程基本信息 ====================

    /**
     * 创建课程（当前登录用户作为教师）
     */
    @PostMapping("/add")
    public BaseResponse<Long> addCourse(@Valid @RequestBody CourseAddRequest request,
                                        HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        long id = courseService.addCourse(request, loginUser.getId());
        return ResultUtils.success(id);
    }

    /**
     * 修改课程（仅创建者或 admin）
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateCourse(@Valid @RequestBody CourseUpdateRequest request,
                                              HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        courseService.checkCourseAuth(request.getId(), loginUser);
        boolean result = courseService.updateCourse(request);
        return ResultUtils.success(result);
    }

    /**
     * 删除课程（逻辑删除，含资源与题目关联）
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteCourse(@RequestParam("id") long id,
                                              HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        courseService.checkCourseAuth(id, loginUser);
        boolean result = courseService.deleteCourse(id);
        return ResultUtils.success(result);
    }

    /**
     * 分页查询课程列表（支持关键字、难度、状态过滤）
     */
    @GetMapping("/list")
    public BaseResponse<IPage<CourseVO>> listCourse(@ModelAttribute CourseQueryRequest request,
                                                    HttpServletRequest httpRequest) {
        userService.getLoginUser(httpRequest);
        IPage<CourseVO> page = courseService.listCourseByPage(request);
        return ResultUtils.success(page);
    }

    /**
     * 查询课程详情（含视频、PPT、题目）
     */
    @GetMapping("/get")
    public BaseResponse<CourseDetailVO> getCourse(@RequestParam("id") long id,
                                                  HttpServletRequest httpRequest) {
        userService.getLoginUser(httpRequest);
        CourseDetailVO detail = courseService.getCourseDetail(id);
        return ResultUtils.success(detail);
    }

    // ==================== 文件上传 ====================

    /**
     * 上传课程封面，返回访问 URL
     */
    @PostMapping("/upload/cover")
    public BaseResponse<String> uploadCover(@RequestParam("courseId") long courseId,
                                            @RequestParam("file") MultipartFile file,
                                            HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        String url = courseResourceService.uploadCover(courseId, file, loginUser);
        return ResultUtils.success(url);
    }

    /**
     * 上传课程视频，返回资源信息
     */
    @PostMapping("/upload/video")
    public BaseResponse<CourseResourceVO> uploadVideo(@RequestParam("courseId") long courseId,
                                                       @RequestParam(value = "title", required = false) String title,
                                                       @RequestParam(value = "description", required = false) String description,
                                                       @RequestParam(value = "sortOrder", required = false) Integer sortOrder,
                                                       @RequestParam("file") MultipartFile file,
                                                       HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        CourseResourceVO vo = courseResourceService.uploadResource(
                courseId, "video", title, description, sortOrder, file, loginUser);
        return ResultUtils.success(vo);
    }

    /**
     * 上传课程 PPT，返回资源信息
     */
    @PostMapping("/upload/ppt")
    public BaseResponse<CourseResourceVO> uploadPpt(@RequestParam("courseId") long courseId,
                                                     @RequestParam(value = "title", required = false) String title,
                                                     @RequestParam(value = "description", required = false) String description,
                                                     @RequestParam(value = "sortOrder", required = false) Integer sortOrder,
                                                     @RequestParam("file") MultipartFile file,
                                                     HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        CourseResourceVO vo = courseResourceService.uploadResource(
                courseId, "ppt", title, description, sortOrder, file, loginUser);
        return ResultUtils.success(vo);
    }

    // ==================== 资源管理 ====================

    /**
     * 删除课程资源（逻辑删除）
     */
    @PostMapping("/resource/delete")
    public BaseResponse<Boolean> deleteResource(@RequestParam("id") long id,
                                                HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        boolean result = courseResourceService.deleteResource(id, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 修改课程资源信息（标题/描述/排序）
     */
    @PostMapping("/resource/update")
    public BaseResponse<Boolean> updateResource(@Valid @RequestBody CourseResourceUpdateRequest request,
                                                HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        boolean result = courseResourceService.updateResource(request, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 查询课程资源列表
     */
    @GetMapping("/resource/list")
    public BaseResponse<List<CourseResourceVO>> listResources(@RequestParam("courseId") long courseId,
                                                              @RequestParam(value = "type", required = false) String type,
                                                              HttpServletRequest httpRequest) {
        userService.getLoginUser(httpRequest);
        List<CourseResourceVO> list = courseResourceService.listResources(courseId, type);
        return ResultUtils.success(list);
    }

    // ==================== 题目绑定 ====================

    /**
     * 给课程绑定题目
     */
    @PostMapping("/problem/bind")
    public BaseResponse<Boolean> bindProblems(@Valid @RequestBody CourseProblemBindRequest request,
                                              HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        courseService.checkCourseAuth(request.getCourseId(), loginUser);
        courseService.bindProblems(request);
        return ResultUtils.success(true);
    }

    /**
     * 解绑课程题目
     */
    @PostMapping("/problem/unbind")
    public BaseResponse<Boolean> unbindProblem(@RequestParam("courseId") long courseId,
                                               @RequestParam("problemId") long problemId,
                                               HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        courseService.checkCourseAuth(courseId, loginUser);
        boolean result = courseService.unbindProblem(courseId, problemId);
        return ResultUtils.success(result);
    }

    /**
     * 查询课程下的题目列表
     */
    @GetMapping("/problem/list")
    public BaseResponse<List<ProblemVO>> listCourseProblems(@RequestParam("courseId") long courseId,
                                                            HttpServletRequest httpRequest) {
        userService.getLoginUser(httpRequest);
        List<ProblemVO> list = courseService.listCourseProblems(courseId);
        return ResultUtils.success(list);
    }
}
