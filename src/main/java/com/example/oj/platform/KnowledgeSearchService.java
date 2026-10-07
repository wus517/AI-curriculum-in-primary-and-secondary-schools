package com.example.oj.platform;

import static com.example.oj.platform.PlatformDtos.*;

public interface KnowledgeSearchService {
    SearchResult search(StudentContext student, long courseId, Long lessonId, String query, int topK);
}
