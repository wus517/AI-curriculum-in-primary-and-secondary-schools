package com.example.oj.learning.tracking;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.oj.common.BusinessException;
import com.example.oj.common.ErrorCode;
import com.example.oj.mapper.ProblemMapper;
import com.example.oj.mapper.SubmissionMapper;
import com.example.oj.model.entity.Submission;
import com.example.oj.platform.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import static com.example.oj.platform.PlatformDtos.*;
import static com.example.oj.platform.ContractIds.*;

@Service
@RequiredArgsConstructor
public class SubmissionQueries implements SubmissionQueryService {
    private final SubmissionMapper submissions;
    private final ProblemMapper problems;
    private final CourseAccessService access;

    @Override
    public CursorResult<SubmissionSummary> listMine(StudentContext student, Long courseId, Long problemId, int pageSize, String cursor) {
        if (pageSize < 1 || pageSize > 20) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        if (courseId != null) access.requireStudy(student, courseId);
        Long before = cursor == null ? null : parse(cursor);
        var rows = submissions.selectList(new LambdaQueryWrapper<Submission>()
                .eq(Submission::getUserId, student.userId()).eq(courseId != null, Submission::getCourseId, courseId)
                .eq(problemId != null, Submission::getProblemId, problemId).lt(before != null, Submission::getId, before)
                .orderByDesc(Submission::getId).last("LIMIT " + (pageSize + 1)));
        boolean more = rows.size() > pageSize;
        var visible = more ? rows.subList(0, pageSize) : rows;
        if (courseId != null) access.requireStudy(student, courseId);
        return new CursorResult<>(visible.stream().map(this::summary).toList(), more ? format(visible.get(visible.size() - 1).getId()) : null);
    }

    @Override
    public SubmissionDetail getMine(StudentContext student, long submissionId) {
        var row = submissions.selectOne(new LambdaQueryWrapper<Submission>()
                .eq(Submission::getId, submissionId).eq(Submission::getUserId, student.userId()));
        if (row == null) throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        if (row.getCourseId() != null) access.requireStudy(student, row.getCourseId());
        var problem = problems.selectById(row.getProblemId());
        String code = row.getCode() == null ? "" : row.getCode();
        boolean truncated = code.length() > 100000;
        if (truncated) code = code.substring(0, 100000);
        return new SubmissionDetail(summary(row), problem == null ? "Unavailable problem" : problem.getTitle(),
                problem == null ? "" : problem.getContent(), row.getLanguage(), code, truncated,
                row.getTimeUsed(), row.getMemoryUsed(), sanitizeError(row.getErrorMessage()));
    }

    private SubmissionSummary summary(Submission row) {
        AssistanceLevel level;
        try { level = AssistanceLevel.valueOf(row.getAssistanceLevel()); }
        catch (Exception e) { level = AssistanceLevel.UNKNOWN; }
        return new SubmissionSummary(format(row.getId()), format(row.getProblemId()),
                row.getCourseId() == null ? null : format(row.getCourseId()), row.getLessonId() == null ? null : format(row.getLessonId()),
                row.getStatus(), level, row.getCreateTime().toInstant());
    }

    public static String sanitizeError(String text) {
        if (text == null) return null;
        String result = text.replaceAll("(?i)(authorization|password|api[_-]?key|token|secret)\\s*[:=]\\s*[^\\s]+", "$1=[redacted]")
                .replaceAll("(?i)Bearer\\s+[A-Za-z0-9._~-]+", "Bearer [redacted]")
                .replaceAll("(?<![A-Za-z0-9])(?:[A-Za-z]:[\\\\/]|/)[^\\s\\\"'<>]*", "[path]");
        return result.length() <= 2000 ? result : result.substring(0, 1980) + " [truncated]";
    }
}
