package com.example.oj.learning.tracking;

import com.example.oj.platform.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import static com.example.oj.platform.PlatformDtos.*;
import static com.example.oj.platform.ContractIds.parse;

@RestController
@RequestMapping("/api/learning")
@Validated
@RequiredArgsConstructor
public class TrackingController {
    private final StudentContextFactory identity;
    private final TrackingService tracking;
    private final SubmissionQueryService submissions;

    @PostMapping("/events")
    public PlatformResponse<EventReceipt> event(@Valid @RequestBody PlatformRequests.ClientEvent body, HttpServletRequest request) {
        var student = identity.from(request); var receipt = tracking.accept(student, body);
        return PlatformResponse.success(tracking.processAccepted(student, receipt));
    }

    @GetMapping("/courses/{courseId}/summary")
    public PlatformResponse<LearningSummary> summary(@PathVariable String courseId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            HttpServletRequest request) {
        return PlatformResponse.success(tracking.getSummary(identity.from(request), parse(courseId), new TimeRange(from, to)));
    }

    @GetMapping("/submissions")
    public PlatformResponse<CursorResult<SubmissionSummary>> submissions(@RequestParam(required = false) String courseId,
            @RequestParam(required = false) String problemId, @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(20) int pageSize, HttpServletRequest request) {
        return PlatformResponse.success(submissions.listMine(identity.from(request), courseId == null ? null : parse(courseId),
                problemId == null ? null : parse(problemId), pageSize, cursor));
    }

    @GetMapping("/submissions/{submissionId}")
    public PlatformResponse<SubmissionDetail> submission(@PathVariable String submissionId, HttpServletRequest request) {
        return PlatformResponse.success(submissions.getMine(identity.from(request), parse(submissionId)));
    }
}
