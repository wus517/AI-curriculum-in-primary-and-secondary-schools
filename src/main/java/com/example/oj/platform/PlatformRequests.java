package com.example.oj.platform;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.List;
import static com.example.oj.platform.PlatformDtos.*;

public final class PlatformRequests {
    private PlatformRequests() {}

    public record EnrollRequest(@NotNull @Pattern(regexp = ContractIds.PATTERN) String courseId) {}
    public record LessonWrite(@NotBlank @Size(max = 128) String title, @Size(max = 10000) String objectives,
                              int sortOrder, @NotNull LessonStatus status) {}
    public record LinkWrite(int sortOrder) {}
    public record KnowledgePointWrite(@NotBlank @Size(max = 128) String name, @Size(max = 10000) String description) {}
    public record ProgressPayload(@NotNull @Pattern(regexp = ContractIds.PATTERN) String resourceId,
                                  @NotBlank @Size(max = 64) String resourceVersion,
                                  @NotNull @Size(max = 100) List<@Valid Interval> intervals,
                                  @PositiveOrZero double lastPosition, @Positive @Max(86400) double durationSeconds) {}
    public record ClientEvent(@NotBlank @Pattern(regexp = "^[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}$") String eventId,
                               @NotNull @Pattern(regexp = ContractIds.PATTERN) String courseId,
                               @NotNull @Pattern(regexp = ContractIds.PATTERN) String lessonId,
                               @NotNull EventType type, @NotNull Instant occurredAt,
                               @NotNull com.fasterxml.jackson.databind.JsonNode payload) {}
    public record NoteWrite(@Pattern(regexp = ContractIds.PATTERN) String lessonId,
                             @NotBlank @Size(max = 128) String title, @NotBlank @Size(max = 200000) String text) {}
    public record JobRequest(@Pattern(regexp = ContractIds.PATTERN) String resourceId,
                              @Pattern(regexp = ContractIds.PATTERN) String documentId) {}
    public record ConversationCreate(@NotNull @Pattern(regexp = ContractIds.PATTERN) String courseId,
                                       @Pattern(regexp = ContractIds.PATTERN) String lessonId) {}
    public record MessageCreate(@NotBlank @Pattern(regexp = "^[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}$") String clientMessageId,
                                @NotBlank @Size(max = 10000) String content, @NotNull Mode mode,
                                @Pattern(regexp = ContractIds.PATTERN) String problemId,
                                @Pattern(regexp = ContractIds.PATTERN) String submissionId) {}
}
