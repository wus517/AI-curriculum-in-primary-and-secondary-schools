package com.example.oj.platform;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class PlatformDtos {
    private PlatformDtos() {}

    public enum LessonStatus { DRAFT, PUBLISHED, DISABLED }
    public enum EventType { LESSON_OPENED, RESOURCE_PROGRESS, LESSON_MARKED_COMPLETE, SUBMISSION_JUDGED, AI_HINT_USED }
    public enum AssistanceLevel { NONE, HINT, SOLUTION, UNKNOWN }
    public enum Mode { CONCEPT, HINT, REVIEW }
    public enum GenerationStatus { QUEUED, RUNNING, COMPLETED, FAILED, CANCELLED, INTERRUPTED }
    public enum IngestionStatus { QUEUED, PROCESSING, READY, FAILED, UNSUPPORTED }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record PageResult<T>(List<T> records, long current, long size, long total) {
        public PageResult { records = List.copyOf(records); }
    }
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record CursorResult<T>(List<T> items, String nextCursor) {
        public CursorResult { items = List.copyOf(items); }
    }
    public record PageQuery(int pageNum, int pageSize) {
        public PageQuery {
            if (pageNum < 1 || pageSize < 1 || pageSize > 50) throw new IllegalArgumentException("Invalid page");
        }
    }
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record TimeRange(Instant from, Instant to) {
        public TimeRange {
            if (from != null && to != null && !from.isBefore(to)) throw new IllegalArgumentException("Invalid range");
        }
    }
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record KnowledgePoint(String id, String courseId, String name, String description, boolean active) {}
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record ResourceRef(String id, String title, String resourceType, String version, int sortOrder, String contentUrl) {}
    public record ProblemRef(String id, String title, int difficulty, int sortOrder, List<String> knowledgePointIds) {}
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record Lesson(String id, String courseId, String title, String objectives, int sortOrder,
                         LessonStatus status, List<ResourceRef> resources, List<ProblemRef> problems,
                         List<KnowledgePoint> knowledgePoints) {}
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record CourseOutline(String courseId, String title, String description, boolean enrolled,
                                List<Lesson> lessons, List<KnowledgePoint> knowledgePoints) {}
    public record PracticeCandidate(String problemId, String title, int difficulty,
                                    List<String> knowledgePointIds, boolean accepted) {}
    public record Interval(double start, double end) {
        public Interval {
            if (!Double.isFinite(start) || !Double.isFinite(end) || start < 0 || end <= start) {
                throw new IllegalArgumentException("Invalid playback interval");
            }
        }
    }
    public record Evidence(String id, String type, Instant occurredAt) {}
    public record ResourceProgress(String resourceId, String resourceVersion, List<Interval> intervals,
                                   double recordedSeconds, double lastPosition) {}
    public record LessonProgress(String lessonId, boolean selfReportedComplete, Instant updatedAt,
                                 List<ResourceProgress> resources) {}
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record LearningSummary(String courseId, TimeRange range, Instant asOf, long accepted,
                                  long judged, Double passRate, Map<String, Long> statusCounts,
                                  List<LessonProgress> lessons, List<Evidence> evidence) {}
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record SubmissionSummary(String id, String problemId, String courseId, String lessonId,
                                    String status, AssistanceLevel assistanceLevel, Instant createdAt) {}
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record SubmissionDetail(SubmissionSummary submission, String problemTitle, String publicContent,
                                   String language, String code, boolean codeTruncated, Long timeUsed, Long memoryUsed, String errorSummary) {}
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record Citation(String citationId, String courseId, String lessonId, String resourceId,
                           String documentId, String documentVersion, String title, Integer page,
                           Double startSeconds, Double endSeconds, String quote, String contentUrl) {}
    public record SearchHit(String text, double score, Citation citation) {}
    public record SearchResult(String retrievalMode, List<SearchHit> items) {}
    public record PracticeCandidates(List<PracticeCandidate> items) {}
    public record Enrollment(String id, String userId, String courseId, String status, Instant joinedAt) {}
    public record EventReceipt(String eventId, boolean duplicate, String processingStatus) {}
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record Document(String id, String courseId, String lessonId, String resourceId, String title,
                           String version, IngestionStatus status, boolean active) {}
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record IngestionJob(String jobId, String documentId, String version, IngestionStatus status,
                               int attempts, String error) {}
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record Conversation(String id, String courseId, String lessonId, Instant createdAt) {}
    public record Message(String id, String clientMessageId, String role, GenerationStatus status,
                          Mode mode, String content, List<Citation> citations, Instant createdAt, Instant updatedAt) {}
    public record Cancellation(String messageId, GenerationStatus status) {}
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record ToolError(String code, String message, boolean retryable) {}
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record ToolResult(boolean ok, JsonNode data, ToolError error) {}
    public record ToolInvocation(String name, JsonNode arguments) {}
    public record ModelMessage(String role, String text, List<ToolInvocation> toolCalls, JsonNode toolResult) {}
    public record ModelRequest(Mode mode, List<ModelMessage> messages, List<JsonNode> toolDefinitions) {}
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record ModelTurn(String text, List<ToolInvocation> toolCalls, List<String> citationIds,
                            List<String> problemIds, Map<String, Long> usage) {}
}
