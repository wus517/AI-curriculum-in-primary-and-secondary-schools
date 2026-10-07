package com.example.oj.platform;

import static com.example.oj.platform.PlatformDtos.*;

public interface LearningQueryService {
    LearningSummary getSummary(StudentContext student, long courseId, TimeRange range);
}
