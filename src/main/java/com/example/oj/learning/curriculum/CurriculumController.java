package com.example.oj.learning.curriculum;

import com.example.oj.platform.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import static com.example.oj.platform.PlatformDtos.*;
import static com.example.oj.platform.ContractIds.parse;

@RestController
@RequestMapping("/api/learning")
@Validated
@RequiredArgsConstructor
public class CurriculumController {
    private final StudentContextFactory identity;
    private final CurriculumService curriculum;

    @PostMapping("/enrollments")
    public PlatformResponse<Enrollment> join(@Valid @RequestBody PlatformRequests.EnrollRequest body, HttpServletRequest request) {
        return PlatformResponse.success(curriculum.join(identity.from(request), parse(body.courseId())));
    }

    @DeleteMapping("/enrollments/{courseId}")
    public PlatformResponse<Enrollment> withdraw(@PathVariable String courseId, HttpServletRequest request) {
        return PlatformResponse.success(curriculum.withdraw(identity.from(request), parse(courseId)));
    }

    @GetMapping("/courses/{courseId}/outline")
    public PlatformResponse<CourseOutline> outline(@PathVariable String courseId, HttpServletRequest request) {
        return PlatformResponse.success(curriculum.getOutline(identity.from(request), parse(courseId)));
    }

    @GetMapping("/courses/{courseId}/lessons")
    public PlatformResponse<PageResult<Lesson>> lessons(@PathVariable String courseId,
            @RequestParam(defaultValue = "1") @Min(1) int pageNum,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int pageSize, HttpServletRequest request) {
        return PlatformResponse.success(curriculum.listLessons(identity.from(request), parse(courseId), new PageQuery(pageNum, pageSize)));
    }

    @GetMapping("/courses/{courseId}/lessons/{lessonId}")
    public PlatformResponse<Lesson> lesson(@PathVariable String courseId, @PathVariable String lessonId, HttpServletRequest request) {
        return PlatformResponse.success(curriculum.getLesson(identity.from(request), parse(courseId), parse(lessonId)));
    }

    @PostMapping("/courses/{courseId}/lessons")
    public PlatformResponse<Lesson> createLesson(@PathVariable String courseId,
            @Valid @RequestBody PlatformRequests.LessonWrite body, HttpServletRequest request) {
        return PlatformResponse.success(curriculum.createLesson(identity.from(request), parse(courseId), body));
    }

    @PutMapping("/courses/{courseId}/lessons/{lessonId}")
    public PlatformResponse<Lesson> updateLesson(@PathVariable String courseId, @PathVariable String lessonId,
            @Valid @RequestBody PlatformRequests.LessonWrite body, HttpServletRequest request) {
        return PlatformResponse.success(curriculum.updateLesson(identity.from(request), parse(courseId), parse(lessonId), body));
    }

    @DeleteMapping("/courses/{courseId}/lessons/{lessonId}")
    public PlatformResponse<Boolean> deleteLesson(@PathVariable String courseId, @PathVariable String lessonId, HttpServletRequest request) {
        return PlatformResponse.success(curriculum.deleteLesson(identity.from(request), parse(courseId), parse(lessonId)));
    }

    @PutMapping("/courses/{courseId}/lessons/{lessonId}/{kind}/{targetId}")
    public PlatformResponse<Boolean> bind(@PathVariable String courseId, @PathVariable String lessonId,
            @PathVariable String kind, @PathVariable String targetId,
            @Valid @RequestBody PlatformRequests.LinkWrite body, HttpServletRequest request) {
        return PlatformResponse.success(curriculum.bindLesson(identity.from(request), parse(courseId), parse(lessonId),
                linkKind(kind), parse(targetId), body.sortOrder()));
    }

    @DeleteMapping("/courses/{courseId}/lessons/{lessonId}/{kind}/{targetId}")
    public PlatformResponse<Boolean> unbind(@PathVariable String courseId, @PathVariable String lessonId,
            @PathVariable String kind, @PathVariable String targetId, HttpServletRequest request) {
        return PlatformResponse.success(curriculum.unbindLesson(identity.from(request), parse(courseId), parse(lessonId), linkKind(kind), parse(targetId)));
    }

    private CurriculumService.LinkKind linkKind(String kind) {
        return switch (kind) {
            case "resources" -> CurriculumService.LinkKind.resources;
            case "problems" -> CurriculumService.LinkKind.problems;
            case "knowledge-points" -> CurriculumService.LinkKind.knowledge_points;
            default -> throw new IllegalArgumentException("Unknown association");
        };
    }

    @GetMapping("/courses/{courseId}/knowledge-points")
    public PlatformResponse<PageResult<KnowledgePoint>> points(@PathVariable String courseId,
            @RequestParam(defaultValue = "1") @Min(1) int pageNum,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int pageSize, HttpServletRequest request) {
        return PlatformResponse.success(curriculum.listPoints(identity.from(request), parse(courseId), new PageQuery(pageNum, pageSize)));
    }

    @PostMapping("/courses/{courseId}/knowledge-points")
    public PlatformResponse<KnowledgePoint> createPoint(@PathVariable String courseId,
            @Valid @RequestBody PlatformRequests.KnowledgePointWrite body, HttpServletRequest request) {
        return PlatformResponse.success(curriculum.createPoint(identity.from(request), parse(courseId), body));
    }

    @PutMapping("/courses/{courseId}/knowledge-points/{knowledgePointId}")
    public PlatformResponse<KnowledgePoint> updatePoint(@PathVariable String courseId, @PathVariable String knowledgePointId,
            @Valid @RequestBody PlatformRequests.KnowledgePointWrite body, HttpServletRequest request) {
        return PlatformResponse.success(curriculum.updatePoint(identity.from(request), parse(courseId), parse(knowledgePointId), body));
    }

    @DeleteMapping("/courses/{courseId}/knowledge-points/{knowledgePointId}")
    public PlatformResponse<Boolean> deletePoint(@PathVariable String courseId, @PathVariable String knowledgePointId, HttpServletRequest request) {
        return PlatformResponse.success(curriculum.deletePoint(identity.from(request), parse(courseId), parse(knowledgePointId)));
    }

    @PutMapping("/courses/{courseId}/problems/{problemId}/knowledge-points/{knowledgePointId}")
    public PlatformResponse<Boolean> bindProblemPoint(@PathVariable String courseId, @PathVariable String problemId,
            @PathVariable String knowledgePointId, @Valid @RequestBody PlatformRequests.LinkWrite body, HttpServletRequest request) {
        return PlatformResponse.success(curriculum.bindProblemPoint(identity.from(request), parse(courseId), parse(problemId), parse(knowledgePointId), body.sortOrder()));
    }

    @DeleteMapping("/courses/{courseId}/problems/{problemId}/knowledge-points/{knowledgePointId}")
    public PlatformResponse<Boolean> unbindProblemPoint(@PathVariable String courseId, @PathVariable String problemId,
            @PathVariable String knowledgePointId, HttpServletRequest request) {
        return PlatformResponse.success(curriculum.unbindProblemPoint(identity.from(request), parse(courseId), parse(problemId), parse(knowledgePointId)));
    }

    @GetMapping("/courses/{courseId}/practice-candidates")
    public PlatformResponse<PracticeCandidates> candidates(@PathVariable String courseId,
            @RequestParam(required = false) List<String> knowledgePointIds,
            @RequestParam(required = false) @Min(1) @Max(3) Integer difficulty,
            @RequestParam(defaultValue = "10") @Min(1) @Max(10) int limit, HttpServletRequest request) {
        var points = knowledgePointIds == null ? List.<Long>of() : knowledgePointIds.stream().map(ContractIds::parse).toList();
        return PlatformResponse.success(new PracticeCandidates(curriculum.listPracticeCandidates(
                identity.from(request), parse(courseId), points, difficulty, limit)));
    }
}
