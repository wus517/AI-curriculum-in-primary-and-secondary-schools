package com.example.oj.platform;

public interface EnrollmentLookup {
    boolean isActive(long userId, long courseId);
}
