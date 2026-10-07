package com.example.oj.platform;

import java.util.Optional;

public interface CourseCatalog {
    record CourseScope(long courseId, long teacherId, String status, boolean deleted) {}
    Optional<CourseScope> find(long courseId);
}
