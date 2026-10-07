package com.example.oj.platform;

public interface CourseAccessService {
    void requireDiscover(StudentContext student, long courseId);
    void requireStudy(StudentContext student, long courseId);
    void requireManage(StudentContext student, long courseId);
    void requireEnroll(StudentContext student, long courseId);
}
