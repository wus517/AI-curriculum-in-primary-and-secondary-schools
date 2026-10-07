package com.example.oj.learning.curriculum;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.oj.common.BusinessException;
import com.example.oj.common.ErrorCode;
import com.example.oj.learning.curriculum.entity.*;
import com.example.oj.learning.curriculum.entity.Enrollment;
import com.example.oj.learning.curriculum.mapper.*;
import com.example.oj.mapper.CourseMapper;
import com.example.oj.mapper.CourseProblemMapper;
import com.example.oj.mapper.CourseResourceMapper;
import com.example.oj.mapper.ProblemMapper;
import com.example.oj.mapper.SubmissionMapper;
import com.example.oj.model.entity.CourseProblem;
import com.example.oj.model.entity.Submission;
import com.example.oj.platform.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import static com.example.oj.platform.PlatformDtos.*;
import static com.example.oj.platform.ContractIds.format;

@Service
@RequiredArgsConstructor
public class CurriculumService implements CurriculumQueryService, EnrollmentLookup {
    private final CourseAccessService access;
    private final CourseMapper courses;
    private final CourseProblemMapper courseProblems;
    private final CourseResourceMapper resources;
    private final ProblemMapper problems;
    private final SubmissionMapper submissions;
    private final EnrollmentMapper enrollments;
    private final CourseLessonMapper lessons;
    private final KnowledgePointEntityMapper points;
    private final LessonResourceMapper lessonResources;
    private final LessonProblemMapper lessonProblems;
    private final LessonKnowledgePointMapper lessonPoints;
    private final ProblemKnowledgePointMapper problemPoints;

    @Override
    public boolean isActive(long userId, long courseId) {
        return enrollments.selectCount(new LambdaQueryWrapper<Enrollment>()
                .eq(Enrollment::getUserId, userId).eq(Enrollment::getCourseId, courseId)
                .eq(Enrollment::getStatus, "ACTIVE")) > 0;
    }

    @Transactional
    public PlatformDtos.Enrollment join(StudentContext student, long courseId) {
        // Serialize against course publication/deletion changes before the enrollment upsert.
        courses.selectOne(new LambdaQueryWrapper<com.example.oj.model.entity.Course>()
                .eq(com.example.oj.model.entity.Course::getId, courseId).last("FOR UPDATE"));
        access.requireEnroll(student, courseId);
        enrollments.join(student.userId(), courseId, Instant.now());
        return enrollment(student, courseId);
    }

    @Transactional
    public PlatformDtos.Enrollment withdraw(StudentContext student, long courseId) {
        var row = enrollments.selectOne(new LambdaQueryWrapper<Enrollment>()
                .eq(Enrollment::getUserId, student.userId()).eq(Enrollment::getCourseId, courseId).last("FOR UPDATE"));
        if (row == null) throw missing();
        row.setStatus("INACTIVE"); row.setUpdatedAt(Instant.now()); enrollments.updateById(row);
        return enrollment(student, courseId);
    }

    private Enrollment enrollmentRow(StudentContext student, long courseId) {
        var row = enrollments.selectOne(new LambdaQueryWrapper<Enrollment>()
                .eq(Enrollment::getUserId, student.userId()).eq(Enrollment::getCourseId, courseId));
        if (row == null) throw missing();
        return row;
    }

    private PlatformDtos.Enrollment enrollment(StudentContext student, long courseId) {
        var row = enrollmentRow(student, courseId);
        return new PlatformDtos.Enrollment(format(row.getId()), format(row.getUserId()), format(row.getCourseId()),
                row.getStatus(), row.getJoinedAt());
    }

    private boolean managementView(StudentContext student, long courseId) {
        var course = courses.selectById(courseId);
        if (course == null) throw missing();
        return student.isAdmin() || Objects.equals(course.getTeacherId(), student.userId());
    }

    private LambdaQueryWrapper<CourseLesson> lessonQuery(StudentContext student, long courseId) {
        return new LambdaQueryWrapper<CourseLesson>().eq(CourseLesson::getCourseId, courseId)
                .eq(CourseLesson::getActive, 1)
                .eq(!managementView(student, courseId), CourseLesson::getStatus, LessonStatus.PUBLISHED.name())
                .orderByAsc(CourseLesson::getSortOrder, CourseLesson::getId);
    }

    @Override
    public CourseOutline getOutline(StudentContext student, long courseId) {
        access.requireDiscover(student, courseId);
        var course = courses.selectById(courseId);
        if (course == null) throw missing();
        var outline = new CourseOutline(format(courseId), course.getTitle(), course.getDescription(),
                isActive(student.userId(), courseId), lessons.selectList(lessonQuery(student, courseId)).stream()
                .map(this::lessonDto).toList(), activePoints(courseId));
        access.requireDiscover(student, courseId);
        return outline;
    }

    @Override
    public PageResult<Lesson> listLessons(StudentContext student, long courseId, PageQuery page) {
        access.requireDiscover(student, courseId);
        var result = lessons.selectPage(new Page<>(page.pageNum(), page.pageSize()), lessonQuery(student, courseId));
        access.requireDiscover(student, courseId);
        return new PageResult<>(result.getRecords().stream().map(this::lessonDto).toList(),
                result.getCurrent(), result.getSize(), result.getTotal());
    }

    @Override
    public Lesson getLesson(StudentContext student, long courseId, long lessonId) {
        access.requireStudy(student, courseId);
        var row = lessonRow(courseId, lessonId);
        if (!managementView(student, courseId) && !LessonStatus.PUBLISHED.name().equals(row.getStatus())) throw missing();
        var result = lessonDto(row);
        access.requireStudy(student, courseId);
        return result;
    }

    public CourseLesson lessonRow(long courseId, long lessonId) {
        var row = lessons.selectById(lessonId);
        if (row == null || !Objects.equals(row.getCourseId(), courseId) || !Integer.valueOf(1).equals(row.getActive())) {
            throw missing();
        }
        return row;
    }

    @Transactional
    public Lesson createLesson(StudentContext student, long courseId, PlatformRequests.LessonWrite request) {
        access.requireManage(student, courseId);
        var row = new CourseLesson(); row.setCourseId(courseId); row.setActive(1); row.setCreatedAt(Instant.now());
        apply(row, request); lessons.insert(row); return lessonDto(row);
    }

    @Transactional
    public Lesson updateLesson(StudentContext student, long courseId, long lessonId, PlatformRequests.LessonWrite request) {
        access.requireManage(student, courseId);
        var row = lessonRow(courseId, lessonId); apply(row, request); lessons.updateById(row); return lessonDto(row);
    }

    private void apply(CourseLesson row, PlatformRequests.LessonWrite request) {
        if (request.title() == null || request.title().isBlank() || request.status() == null) throw invalid();
        row.setTitle(request.title().trim()); row.setObjectives(request.objectives()); row.setSortOrder(request.sortOrder());
        row.setStatus(request.status().name()); row.setUpdatedAt(Instant.now());
    }

    @Transactional
    public boolean deleteLesson(StudentContext student, long courseId, long lessonId) {
        access.requireManage(student, courseId);
        var row = lessonRow(courseId, lessonId); row.setActive(0); row.setStatus(LessonStatus.DISABLED.name());
        row.setUpdatedAt(Instant.now()); return lessons.updateById(row) == 1;
    }

    public PageResult<KnowledgePoint> listPoints(StudentContext student, long courseId, PageQuery page) {
        access.requireDiscover(student, courseId);
        var result = points.selectPage(new Page<>(page.pageNum(), page.pageSize()),
                new LambdaQueryWrapper<KnowledgePointEntity>().eq(KnowledgePointEntity::getCourseId, courseId)
                        .eq(KnowledgePointEntity::getActive, 1).orderByAsc(KnowledgePointEntity::getId));
        return new PageResult<>(result.getRecords().stream().map(this::pointDto).toList(),
                result.getCurrent(), result.getSize(), result.getTotal());
    }

    @Transactional
    public KnowledgePoint createPoint(StudentContext student, long courseId, PlatformRequests.KnowledgePointWrite request) {
        access.requireManage(student, courseId);
        var point = new KnowledgePointEntity(); point.setCourseId(courseId); point.setActive(1); point.setCreatedAt(Instant.now());
        apply(point, request); points.insert(point); return pointDto(point);
    }

    @Transactional
    public KnowledgePoint updatePoint(StudentContext student, long courseId, long pointId, PlatformRequests.KnowledgePointWrite request) {
        access.requireManage(student, courseId);
        var point = pointRow(courseId, pointId); apply(point, request); points.updateById(point); return pointDto(point);
    }

    private void apply(KnowledgePointEntity point, PlatformRequests.KnowledgePointWrite request) {
        if (request.name() == null || request.name().isBlank()) throw invalid();
        point.setName(request.name().trim()); point.setDescription(request.description()); point.setUpdatedAt(Instant.now());
    }

    @Transactional
    public boolean deletePoint(StudentContext student, long courseId, long pointId) {
        access.requireManage(student, courseId);
        var point = pointRow(courseId, pointId); point.setActive(0); point.setUpdatedAt(Instant.now());
        return points.updateById(point) == 1;
    }

    private KnowledgePointEntity pointRow(long courseId, long pointId) {
        var point = points.selectById(pointId);
        if (point == null || !Objects.equals(point.getCourseId(), courseId) || !Integer.valueOf(1).equals(point.getActive())) {
            throw missing();
        }
        return point;
    }

    public enum LinkKind { resources, problems, knowledge_points }

    @Transactional
    public boolean bindLesson(StudentContext student, long courseId, long lessonId, LinkKind kind, long targetId, int sortOrder) {
        access.requireManage(student, courseId); lessonRow(courseId, lessonId);
        switch (kind) {
            case resources -> {
                var resource = resources.selectById(targetId);
                if (resource == null || !Objects.equals(resource.getCourseId(), courseId)) throw missing();
                lessonResources.bind(lessonId, targetId, sortOrder);
            }
            case problems -> {
                if (problems.selectById(targetId) == null) throw missing();
                lessonProblems.bind(lessonId, targetId, sortOrder);
            }
            case knowledge_points -> { pointRow(courseId, targetId); lessonPoints.bind(lessonId, targetId, sortOrder); }
        }
        return true;
    }

    @Transactional
    public boolean unbindLesson(StudentContext student, long courseId, long lessonId, LinkKind kind, long targetId) {
        access.requireManage(student, courseId); lessonRow(courseId, lessonId);
        switch (kind) {
            case resources -> lessonResources.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<LessonResource>()
                    .eq(LessonResource::getLessonId, lessonId).eq(LessonResource::getResourceId, targetId).set(LessonResource::getActive, 0));
            case problems -> lessonProblems.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<LessonProblem>()
                    .eq(LessonProblem::getLessonId, lessonId).eq(LessonProblem::getProblemId, targetId).set(LessonProblem::getActive, 0));
            case knowledge_points -> lessonPoints.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<LessonKnowledgePoint>()
                    .eq(LessonKnowledgePoint::getLessonId, lessonId).eq(LessonKnowledgePoint::getKnowledgePointId, targetId).set(LessonKnowledgePoint::getActive, 0));
        }
        return true;
    }

    @Transactional
    public boolean bindProblemPoint(StudentContext student, long courseId, long problemId, long pointId, int sortOrder) {
        access.requireManage(student, courseId); pointRow(courseId, pointId);
        if (!courseProblemIds(student, courseId).contains(problemId) || problems.selectById(problemId) == null) throw missing();
        problemPoints.bind(courseId, problemId, pointId, sortOrder); return true;
    }

    @Transactional
    public boolean unbindProblemPoint(StudentContext student, long courseId, long problemId, long pointId) {
        access.requireManage(student, courseId);
        problemPoints.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ProblemKnowledgePoint>()
                .eq(ProblemKnowledgePoint::getCourseId, courseId).eq(ProblemKnowledgePoint::getProblemId, problemId)
                .eq(ProblemKnowledgePoint::getKnowledgePointId, pointId).set(ProblemKnowledgePoint::getActive, 0));
        return true;
    }

    @Override
    public void requireSubmissionSource(StudentContext student, long courseId, Long lessonId, long problemId) {
        access.requireStudy(student, courseId);
        if (problems.selectById(problemId) == null) throw missing();
        if (lessonId != null) {
            getLesson(student, courseId, lessonId);
            if (lessonProblems.selectCount(new LambdaQueryWrapper<LessonProblem>().eq(LessonProblem::getLessonId, lessonId)
                    .eq(LessonProblem::getProblemId, problemId).eq(LessonProblem::getActive, 1)) == 0) throw missing();
        } else if (!courseProblemIds(student, courseId).contains(problemId)) throw missing();
    }

    private LinkedHashSet<Long> courseProblemIds(StudentContext student, long courseId) {
        var ids = new LinkedHashSet<Long>();
        courseProblems.selectList(new LambdaQueryWrapper<CourseProblem>().eq(CourseProblem::getCourseId, courseId)
                .orderByAsc(CourseProblem::getSortOrder, CourseProblem::getId)).forEach(row -> ids.add(row.getProblemId()));
        for (var lesson : lessons.selectList(lessonQuery(student, courseId))) {
            lessonProblems.selectList(new LambdaQueryWrapper<LessonProblem>().eq(LessonProblem::getLessonId, lesson.getId())
                    .eq(LessonProblem::getActive, 1).orderByAsc(LessonProblem::getSortOrder, LessonProblem::getId))
                    .forEach(row -> ids.add(row.getProblemId()));
        }
        return ids;
    }

    @Override
    public List<PracticeCandidate> listPracticeCandidates(StudentContext student, long courseId,
                                                          List<Long> pointIds, Integer difficulty, int limit) {
        access.requireStudy(student, courseId);
        if (limit < 1 || limit > 10 || pointIds == null || pointIds.size() > 20
                || (difficulty != null && (difficulty < 1 || difficulty > 3))) throw invalid();
        pointIds.forEach(id -> pointRow(courseId, id));
        var result = new ArrayList<PracticeCandidate>();
        for (long problemId : courseProblemIds(student, courseId)) {
            var problem = problems.selectById(problemId);
            if (problem == null || (difficulty != null && !Objects.equals(difficulty, problem.getDifficulty()))) continue;
            var attachedPoints = problemPointIds(courseId, problemId);
            if (!pointIds.isEmpty() && attachedPoints.stream().noneMatch(id -> pointIds.contains(ContractIds.parse(id)))) continue;
            boolean accepted = submissions.selectCount(new LambdaQueryWrapper<Submission>()
                    .eq(Submission::getUserId, student.userId()).eq(Submission::getProblemId, problemId)
                    .eq(Submission::getStatus, "ACCEPTED")) > 0;
            result.add(new PracticeCandidate(format(problemId), problem.getTitle(), problem.getDifficulty(), attachedPoints, accepted));
            if (result.size() == limit) break;
        }
        access.requireStudy(student, courseId); return List.copyOf(result);
    }

    private List<String> problemPointIds(long courseId, long problemId) {
        return problemPoints.selectList(new LambdaQueryWrapper<ProblemKnowledgePoint>()
                .eq(ProblemKnowledgePoint::getCourseId, courseId).eq(ProblemKnowledgePoint::getProblemId, problemId)
                .eq(ProblemKnowledgePoint::getActive, 1).orderByAsc(ProblemKnowledgePoint::getSortOrder, ProblemKnowledgePoint::getId))
                .stream().filter(row -> {
                    var point = points.selectById(row.getKnowledgePointId());
                    return point != null && Integer.valueOf(1).equals(point.getActive()) && Objects.equals(point.getCourseId(), courseId);
                }).map(row -> format(row.getKnowledgePointId())).toList();
    }

    private List<KnowledgePoint> activePoints(long courseId) {
        return points.selectList(new LambdaQueryWrapper<KnowledgePointEntity>().eq(KnowledgePointEntity::getCourseId, courseId)
                .eq(KnowledgePointEntity::getActive, 1).orderByAsc(KnowledgePointEntity::getId)).stream().map(this::pointDto).toList();
    }

    private KnowledgePoint pointDto(KnowledgePointEntity point) {
        return new KnowledgePoint(format(point.getId()), format(point.getCourseId()), point.getName(), point.getDescription(),
                Integer.valueOf(1).equals(point.getActive()));
    }

    private Lesson lessonDto(CourseLesson lesson) {
        var resourceRefs = new ArrayList<ResourceRef>();
        lessonResources.selectList(new LambdaQueryWrapper<LessonResource>().eq(LessonResource::getLessonId, lesson.getId())
                .eq(LessonResource::getActive, 1).orderByAsc(LessonResource::getSortOrder, LessonResource::getId)).forEach(link -> {
            var resource = resources.selectById(link.getResourceId());
            if (resource != null && Objects.equals(resource.getCourseId(), lesson.getCourseId())) {
                resourceRefs.add(new ResourceRef(format(resource.getId()), resource.getTitle(), resource.getResourceType(),
                        format(resource.getId()), link.getSortOrder(), "/api/learning/resources/" + resource.getId() + "/content"));
            }
        });
        var problemRefs = new ArrayList<ProblemRef>();
        lessonProblems.selectList(new LambdaQueryWrapper<LessonProblem>().eq(LessonProblem::getLessonId, lesson.getId())
                .eq(LessonProblem::getActive, 1).orderByAsc(LessonProblem::getSortOrder, LessonProblem::getId)).forEach(link -> {
            var problem = problems.selectById(link.getProblemId());
            if (problem != null) problemRefs.add(new ProblemRef(format(problem.getId()), problem.getTitle(), problem.getDifficulty(),
                    link.getSortOrder(), problemPointIds(lesson.getCourseId(), problem.getId())));
        });
        var attachedPoints = new ArrayList<KnowledgePoint>();
        lessonPoints.selectList(new LambdaQueryWrapper<LessonKnowledgePoint>().eq(LessonKnowledgePoint::getLessonId, lesson.getId())
                .eq(LessonKnowledgePoint::getActive, 1).orderByAsc(LessonKnowledgePoint::getSortOrder, LessonKnowledgePoint::getId))
                .forEach(link -> {
                    var point = points.selectById(link.getKnowledgePointId());
                    if (point != null && Integer.valueOf(1).equals(point.getActive()) && Objects.equals(point.getCourseId(), lesson.getCourseId())) {
                        attachedPoints.add(pointDto(point));
                    }
                });
        return new Lesson(format(lesson.getId()), format(lesson.getCourseId()), lesson.getTitle(), lesson.getObjectives(),
                lesson.getSortOrder(), LessonStatus.valueOf(lesson.getStatus()), resourceRefs, problemRefs, attachedPoints);
    }

    private BusinessException missing() { return new BusinessException(ErrorCode.NOT_FOUND_ERROR); }
    private BusinessException invalid() { return new BusinessException(ErrorCode.PARAMS_ERROR); }
}
