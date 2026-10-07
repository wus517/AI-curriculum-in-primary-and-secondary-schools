package com.example.oj.platform;

import static com.example.oj.platform.PlatformDtos.*;

public interface SubmissionQueryService {
    CursorResult<SubmissionSummary> listMine(StudentContext student, Long courseId, Long problemId,
                                             int pageSize, String cursor);
    SubmissionDetail getMine(StudentContext student, long submissionId);
}
