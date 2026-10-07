package com.example.oj.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.oj.common.BusinessException;
import com.example.oj.common.ErrorCode;
import com.example.oj.mapper.CourseMapper;
import com.example.oj.mapper.CourseProblemMapper;
import com.example.oj.mapper.CourseResourceMapper;
import com.example.oj.model.entity.Course;
import com.example.oj.model.entity.CourseProblem;
import com.example.oj.model.entity.CourseResource;
import com.example.oj.model.entity.Problem;
import com.example.oj.model.entity.User;
import com.example.oj.model.dto.course.CourseAddRequest;
import com.example.oj.model.dto.course.CourseProblemBindRequest;
import com.example.oj.model.dto.course.CourseQueryRequest;
import com.example.oj.model.dto.course.CourseUpdateRequest;
import com.example.oj.model.enums.CourseResourceTypeEnum;
import com.example.oj.model.enums.CourseStatusEnum;
import com.example.oj.model.vo.CourseDetailVO;
import com.example.oj.model.vo.CourseResourceVO;
import com.example.oj.model.vo.CourseVO;
import com.example.oj.model.vo.ProblemVO;
import com.example.oj.service.CourseService;
import com.example.oj.service.ProblemService;
import com.example.oj.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 课程服务实现
 * <p>
 * 注意：为避免与 CourseResourceService 循环依赖，本类直接使用 CourseResourceMapper / CourseProblemMapper。
 */
@Service
public class CourseServiceImpl extends ServiceImpl<CourseMapper, Course> implements CourseService {

    private final CourseResourceMapper courseResourceMapper;
    private final CourseProblemMapper courseProblemMapper;
    private final ProblemService problemService;
    private final UserService userService;

    public CourseServiceImpl(CourseResourceMapper courseResourceMapper,
                             CourseProblemMapper courseProblemMapper,
                             ProblemService problemService,
                             UserService userService) {
        this.courseResourceMapper = courseResourceMapper;
        this.courseProblemMapper = courseProblemMapper;
        this.problemService = problemService;
        this.userService = userService;
    }

    // ==================== 课程 CRUD ====================

    @Override
    public long addCourse(CourseAddRequest request, Long teacherId) {
        Course course = new Course();
        course.setTitle(request.getTitle());
        course.setDescription(request.getDescription());
        course.setDifficulty(StrUtil.isBlank(request.getDifficulty()) ? "beginner" : request.getDifficulty());
        course.setTags(tagsToJson(request.getTags()));
        course.setTeacherId(teacherId);
        // 新建课程默认草稿
        course.setStatus(CourseStatusEnum.DRAFT.getValue());
        boolean saved = this.save(course);
        if (!saved) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "创建课程失败");
        }
        return course.getId();
    }

    @Override
    public boolean updateCourse(CourseUpdateRequest request) {
        Course oldCourse = this.getById(request.getId());
        if (oldCourse == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "课程不存在");
        }
        Course course = new Course();
        course.setId(request.getId());
        course.setTitle(request.getTitle());
        course.setDescription(request.getDescription());
        course.setDifficulty(request.getDifficulty());
        if (request.getTags() != null) {
            course.setTags(tagsToJson(request.getTags()));
        }
        if (StrUtil.isNotBlank(request.getStatus())) {
            if (!CourseStatusEnum.isSupport(request.getStatus())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "非法的课程状态");
            }
            course.setStatus(request.getStatus());
        }
        return this.updateById(course);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteCourse(long id) {
        Course course = this.getById(id);
        if (course == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "课程不存在");
        }
        // 逻辑删除课程
        boolean removed = this.removeById(id);
        // 逻辑删除其资源
        courseResourceMapper.delete(new LambdaQueryWrapper<CourseResource>()
                .eq(CourseResource::getCourseId, id));
        // 逻辑删除其题目关联
        courseProblemMapper.delete(new LambdaQueryWrapper<CourseProblem>()
                .eq(CourseProblem::getCourseId, id));
        return removed;
    }

    @Override
    public CourseVO getCourseById(long id) {
        Course course = this.getById(id);
        if (course == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "课程不存在");
        }
        CourseVO vo = new CourseVO();
        fillCourseVO(course, vo);
        vo.setTeacherName(teacherName(course.getTeacherId()));
        return vo;
    }

    @Override
    public IPage<CourseVO> listCourseByPage(CourseQueryRequest request) {
        long pageNum = request.getPageNum() == null ? 1 : request.getPageNum();
        long pageSize = request.getPageSize() == null ? 10 : request.getPageSize();

        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<>();
        String keyword = request.getKeyword();
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(Course::getTitle, keyword).or().like(Course::getDescription, keyword));
        }
        wrapper.eq(StrUtil.isNotBlank(request.getDifficulty()), Course::getDifficulty, request.getDifficulty());
        wrapper.eq(StrUtil.isNotBlank(request.getStatus()), Course::getStatus, request.getStatus());
        wrapper.orderByDesc(Course::getCreateTime);

        Page<Course> page = new Page<>(pageNum, pageSize);
        IPage<Course> coursePage = this.page(page, wrapper);

        // 批量查询教师名
        Set<Long> teacherIds = coursePage.getRecords().stream()
                .map(Course::getTeacherId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> nameMap = teacherIds.isEmpty()
                ? Collections.emptyMap()
                : userService.listByIds(teacherIds).stream()
                .collect(Collectors.toMap(User::getId, User::getUsername));

        List<CourseVO> voList = coursePage.getRecords().stream().map(course -> {
            CourseVO vo = new CourseVO();
            fillCourseVO(course, vo);
            vo.setTeacherName(nameMap.get(course.getTeacherId()));
            return vo;
        }).collect(Collectors.toList());

        Page<CourseVO> voPage = new Page<>(pageNum, pageSize, coursePage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public CourseDetailVO getCourseDetail(long id) {
        Course course = this.getById(id);
        if (course == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "课程不存在");
        }
        CourseDetailVO vo = new CourseDetailVO();
        fillCourseVO(course, vo);
        vo.setTeacherName(teacherName(course.getTeacherId()));

        // 资源拆分为视频/PPT
        List<CourseResource> resources = courseResourceMapper.selectList(new LambdaQueryWrapper<CourseResource>()
                .eq(CourseResource::getCourseId, id)
                .orderByAsc(CourseResource::getSortOrder));
        List<CourseResourceVO> videoList = new ArrayList<>();
        List<CourseResourceVO> pptList = new ArrayList<>();
        for (CourseResource resource : resources) {
            if (CourseResourceTypeEnum.VIDEO.getValue().equals(resource.getResourceType())) {
                videoList.add(toResourceVO(resource));
            } else if (CourseResourceTypeEnum.PPT.getValue().equals(resource.getResourceType())) {
                pptList.add(toResourceVO(resource));
            }
        }
        vo.setVideoList(videoList);
        vo.setPptList(pptList);
        vo.setProblemList(listCourseProblems(id));
        return vo;
    }

    // ==================== 题目绑定 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindProblems(CourseProblemBindRequest request) {
        Long courseId = request.getCourseId();
        List<Long> problemIdList = request.getProblemIdList();

        // 校验题目是否存在
        List<Problem> problems = problemService.listByIds(problemIdList);
        Set<Long> existProblemIds = problems.stream().map(Problem::getId).collect(Collectors.toSet());
        for (Long pid : new HashSet<>(problemIdList)) {
            if (!existProblemIds.contains(pid)) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "题目不存在：" + pid);
            }
        }

        // 已绑定的题目去重
        List<CourseProblem> existBindings = courseProblemMapper.selectList(new LambdaQueryWrapper<CourseProblem>()
                .eq(CourseProblem::getCourseId, courseId));
        Set<Long> boundIds = existBindings.stream().map(CourseProblem::getProblemId).collect(Collectors.toSet());

        // 按 problemIdList 的顺序设置 sortOrder
        int order = 0;
        for (Long pid : problemIdList) {
            if (boundIds.contains(pid)) {
                continue;
            }
            CourseProblem cp = new CourseProblem();
            cp.setCourseId(courseId);
            cp.setProblemId(pid);
            cp.setSortOrder(order);
            order++;
            courseProblemMapper.insert(cp);
        }
    }

    @Override
    public boolean unbindProblem(long courseId, long problemId) {
        CourseProblem binding = courseProblemMapper.selectOne(new LambdaQueryWrapper<CourseProblem>()
                .eq(CourseProblem::getCourseId, courseId)
                .eq(CourseProblem::getProblemId, problemId));
        if (binding == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "该题目未绑定到课程");
        }
        return courseProblemMapper.deleteById(binding.getId()) > 0;
    }

    @Override
    public List<ProblemVO> listCourseProblems(long courseId) {
        List<CourseProblem> bindings = courseProblemMapper.selectList(new LambdaQueryWrapper<CourseProblem>()
                .eq(CourseProblem::getCourseId, courseId)
                .orderByAsc(CourseProblem::getSortOrder));
        if (CollUtil.isEmpty(bindings)) {
            return Collections.emptyList();
        }
        List<Long> problemIds = bindings.stream()
                .map(CourseProblem::getProblemId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, Problem> problemMap = problemService.listByIds(problemIds).stream()
                .collect(Collectors.toMap(Problem::getId, p -> p));

        List<ProblemVO> result = new ArrayList<>();
        for (CourseProblem binding : bindings) {
            Problem problem = problemMap.get(binding.getProblemId());
            if (problem != null) {
                result.add(toProblemVO(problem));
            }
        }
        return result;
    }

    // ==================== 权限校验 ====================

    @Override
    public void checkCourseAuth(long courseId, User loginUser) {
        Course course = this.getById(courseId);
        if (course == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "课程不存在");
        }
        if (!"admin".equals(loginUser.getRole()) && !course.getTeacherId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权操作该课程");
        }
    }

    // ==================== 私有工具方法 ====================

    private String teacherName(Long teacherId) {
        if (teacherId == null) {
            return null;
        }
        User teacher = userService.getById(teacherId);
        return teacher == null ? null : teacher.getUsername();
    }

    private void fillCourseVO(Course course, CourseVO vo) {
        vo.setId(course.getId());
        vo.setTitle(course.getTitle());
        vo.setDescription(course.getDescription());
        vo.setCoverUrl(course.getCoverUrl());
        vo.setDifficulty(course.getDifficulty());
        vo.setTeacherId(course.getTeacherId());
        vo.setStatus(course.getStatus());
        vo.setCreateTime(course.getCreateTime());
        vo.setUpdateTime(course.getUpdateTime());
        if (StrUtil.isNotBlank(course.getTags())) {
            vo.setTags(JSONUtil.toList(course.getTags(), String.class));
        } else {
            vo.setTags(Collections.emptyList());
        }
    }

    private CourseResourceVO toResourceVO(CourseResource resource) {
        CourseResourceVO vo = new CourseResourceVO();
        vo.setId(resource.getId());
        vo.setCourseId(resource.getCourseId());
        vo.setResourceType(resource.getResourceType());
        vo.setTitle(resource.getTitle());
        vo.setDescription(resource.getDescription());
        vo.setFileName(resource.getFileName());
        vo.setFileUrl(resource.getFileUrl());
        vo.setFileSize(resource.getFileSize());
        vo.setMimeType(resource.getMimeType());
        vo.setSortOrder(resource.getSortOrder());
        vo.setCreateTime(resource.getCreateTime());
        return vo;
    }

    private ProblemVO toProblemVO(Problem problem) {
        ProblemVO vo = new ProblemVO();
        vo.setId(problem.getId());
        vo.setTitle(problem.getTitle());
        vo.setContent(problem.getContent());
        vo.setInputDesc(problem.getInputDesc());
        vo.setOutputDesc(problem.getOutputDesc());
        vo.setSampleInput(problem.getSampleInput());
        vo.setSampleOutput(problem.getSampleOutput());
        vo.setTimeLimit(problem.getTimeLimit());
        vo.setMemoryLimit(problem.getMemoryLimit());
        vo.setDifficulty(problem.getDifficulty());
        vo.setCreateUserId(problem.getCreateUserId());
        vo.setCreateTime(problem.getCreateTime());
        if (StrUtil.isNotBlank(problem.getTags())) {
            vo.setTags(JSONUtil.toList(problem.getTags(), String.class));
        }
        return vo;
    }

    private String tagsToJson(List<String> tags) {
        if (CollUtil.isEmpty(tags)) {
            return "[]";
        }
        return JSONUtil.toJsonStr(tags);
    }
}
