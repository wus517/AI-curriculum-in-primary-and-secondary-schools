package com.example.oj.platform;

import java.util.List;
import static com.example.oj.platform.PlatformDtos.*;

public interface CurriculumQueryService {
    CourseOutline getOutline(StudentContext student, long courseId);
    PageResult<Lesson> listLessons(StudentContext student, long courseId, PageQuery page);
    Lesson getLesson(StudentContext student, long courseId, long lessonId);
    List<PracticeCandidate> listPracticeCandidates(StudentContext student, long courseId,
                                                  List<Long> knowledgePointIds, Integer difficulty, int limit);
    void requireSubmissionSource(StudentContext student, long courseId, Long lessonId, long problemId);
}
