package com.example.oj.platform;

import com.example.oj.mapper.CourseMapper;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ExistingCourseCatalog implements CourseCatalog {
    private final CourseMapper courses;
    public ExistingCourseCatalog(CourseMapper courses) { this.courses = courses; }

    @Override
    public Optional<CourseScope> find(long id) {
        return Optional.ofNullable(courses.selectById(id)).map(c -> new CourseScope(
                c.getId(), c.getTeacherId() == null ? 0 : c.getTeacherId(), c.getStatus(),
                Integer.valueOf(1).equals(c.getIsDelete())));
    }
}
