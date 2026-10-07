package com.example.oj.platform;

import com.example.oj.common.BusinessException;
import com.example.oj.common.ErrorCode;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class DefaultCourseAccessService implements CourseAccessService {
    private final CourseCatalog catalog;
    private final ObjectProvider<EnrollmentLookup> enrollment;

    public DefaultCourseAccessService(CourseCatalog catalog, ObjectProvider<EnrollmentLookup> enrollment) {
        this.catalog = catalog;
        this.enrollment = enrollment;
    }

    private CourseCatalog.CourseScope visibleCourse(StudentContext student, long id) {
        Objects.requireNonNull(student, "student");
        if (id <= 0) throw new BusinessException(ErrorCode.PARAMS_ERROR, "Invalid course ID");
        CourseCatalog.CourseScope course = catalog.find(id)
                .filter(c -> !c.deleted()).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ERROR));
        return course;
    }

    private boolean manages(StudentContext student, CourseCatalog.CourseScope course) {
        return student.isAdmin() || student.userId() == course.teacherId();
    }

    @Override
    public void requireDiscover(StudentContext student, long id) {
        var course = visibleCourse(student, id);
        if (!"published".equals(course.status()) && !manages(student, course)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
    }

    @Override
    public void requireStudy(StudentContext student, long id) {
        var course = visibleCourse(student, id);
        if (manages(student, course)) return;
        if (!"published".equals(course.status())) throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        EnrollmentLookup lookup = enrollment.getIfAvailable();
        if (lookup == null || !lookup.isActive(student.userId(), id)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "Course enrollment required");
        }
    }

    @Override
    public void requireManage(StudentContext student, long id) {
        if (!manages(student, visibleCourse(student, id))) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
    }

    @Override
    public void requireEnroll(StudentContext student, long id) {
        if (!"published".equals(visibleCourse(student, id).status())) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
    }
}
