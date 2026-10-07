package com.example.oj.platform;

import java.util.List;
import static com.example.oj.platform.PlatformDtos.*;

public interface CitationValidationService {
    List<Citation> validate(StudentContext student, long courseId, List<String> citationIds);
}
