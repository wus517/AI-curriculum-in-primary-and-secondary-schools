package com.example.oj.platform;

import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import static com.example.oj.platform.PlatformDtos.*;

public final class StreamEvents {
    private StreamEvents() {}
    public record AcceptedEvent(String messageId, String requestId, long seq) {}
    public record ToolStatusEvent(String toolName, String status, long seq) {}
    public record DeltaEvent(String messageId, long seq, String text) {}
    public record CitationsEvent(String messageId, long seq, List<Citation> items) {}
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CompletedEvent(String messageId, long seq, String status, Map<String, Long> usage) {}
    public record FailedEvent(String messageId, long seq, String code, String message, boolean retryable) {}
    public record CancelledEvent(String messageId, long seq) {}
}
