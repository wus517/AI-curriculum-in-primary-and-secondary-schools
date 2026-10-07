package com.example.oj.platform;

import java.time.Duration;
import static com.example.oj.platform.PlatformDtos.*;

public interface TutorToolGateway {
    ToolResult call(StudentContext student, ToolInvocation invocation, Duration timeout);
}
