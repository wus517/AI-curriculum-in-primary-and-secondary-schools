package com.example.oj.learning.tracking;

import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.oj.common.BusinessException;
import com.example.oj.common.ErrorCode;
import com.example.oj.learning.tracking.entity.LearningEventEntity;
import com.example.oj.learning.tracking.entity.LessonProgressEntity;
import com.example.oj.learning.tracking.mapper.LearningEventEntityMapper;
import com.example.oj.learning.tracking.mapper.LessonProgressEntityMapper;
import com.example.oj.mapper.SubmissionMapper;
import com.example.oj.model.entity.Submission;
import com.example.oj.platform.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import static com.example.oj.platform.PlatformDtos.*;
import static com.example.oj.platform.ContractIds.*;

@Service
@RequiredArgsConstructor
public class TrackingService implements LearningQueryService {
    private final CourseAccessService access;
    private final CurriculumQueryService curriculum;
    private final LearningEventEntityMapper events;
    private final LessonProgressEntityMapper progress;
    private final SubmissionMapper submissions;
    private final ObjectMapper json;
    private final PlatformTransactionManager transactions;

    public record StoredResource(String resourceId, String resourceVersion, List<Interval> intervals,
                                  double lastPosition, Instant lastOccurredAt) {}

    @Transactional
    public EventReceipt accept(StudentContext student, PlatformRequests.ClientEvent request) {
        long courseId = parse(request.courseId()), lessonId = parse(request.lessonId());
        access.requireStudy(student, courseId);
        var lesson = curriculum.getLesson(student, courseId, lessonId);
        if (request.eventId() == null || !request.eventId().matches("^[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}$")) throw invalid();
        if (request.type() == null || !Set.of(EventType.LESSON_OPENED, EventType.RESOURCE_PROGRESS,
                EventType.LESSON_MARKED_COMPLETE).contains(request.type())) throw invalid();
        Instant now = Instant.now();
        if (request.occurredAt() == null || request.occurredAt().isAfter(now.plusSeconds(300))
                || request.occurredAt().isBefore(now.minus(Duration.ofDays(30)))) throw invalid();
        JsonNode payload = validatePayload(lesson, request.type(), request.payload());
        return append(UUID.fromString(request.eventId()).toString(), student.userId(), courseId, lessonId,
                request.type(), "CLIENT", request.occurredAt(), payload);
    }

    private JsonNode validatePayload(Lesson lesson, EventType type, JsonNode payload) {
        if (payload == null || !payload.isObject()) throw invalid();
        if (type != EventType.RESOURCE_PROGRESS) {
            if (!payload.isEmpty()) throw invalid();
            return payload;
        }
        var names = new java.util.HashSet<String>(); payload.fieldNames().forEachRemaining(names::add);
        if (!names.equals(Set.of("resourceId", "resourceVersion", "intervals", "lastPosition", "durationSeconds"))
                || !payload.path("resourceId").isTextual() || !payload.path("resourceVersion").isTextual()
                || !payload.path("lastPosition").isNumber() || !payload.path("durationSeconds").isNumber()
                || !payload.path("intervals").isArray()) throw invalid();
        PlatformRequests.ProgressPayload value;
        try { value = json.treeToValue(payload, PlatformRequests.ProgressPayload.class); }
        catch (Exception e) { throw invalid(); }
        var resource = lesson.resources().stream().filter(r -> r.id().equals(value.resourceId())).findFirst().orElseThrow(this::invalid);
        if (!"video".equals(resource.resourceType()) || !resource.version().equals(value.resourceVersion())
                || value.intervals() == null || value.intervals().size() > 100
                || !Double.isFinite(value.durationSeconds()) || value.durationSeconds() <= 0 || value.durationSeconds() > 86400
                || !Double.isFinite(value.lastPosition()) || value.lastPosition() < 0 || value.lastPosition() > value.durationSeconds()) throw invalid();
        for (Interval interval : value.intervals()) {
            if (interval == null || interval.end() > value.durationSeconds()) throw invalid();
        }
        return json.valueToTree(value);
    }

    @Transactional
    public EventReceipt recordJudged(Submission submission, long revision) {
        var payload = json.createObjectNode();
        payload.put("submissionId", format(submission.getId())); payload.put("problemId", format(submission.getProblemId()));
        payload.put("status", submission.getStatus()); payload.put("revision", revision);
        payload.put("assistanceLevel", submission.getAssistanceLevel() == null ? "UNKNOWN" : submission.getAssistanceLevel());
        return append("submission:" + submission.getId() + ":judged:" + revision, submission.getUserId(),
                submission.getCourseId(), submission.getLessonId(), EventType.SUBMISSION_JUDGED, "JUDGE", Instant.now(), payload);
    }

    @Transactional
    public EventReceipt recordHint(StudentContext student, long courseId, Long lessonId, long messageId, AssistanceLevel level) {
        access.requireStudy(student, courseId);
        if (lessonId != null) curriculum.getLesson(student, courseId, lessonId);
        if (level != AssistanceLevel.HINT && level != AssistanceLevel.SOLUTION) throw invalid();
        var payload = json.createObjectNode(); payload.put("messageId", format(messageId)); payload.put("assistanceLevel", level.name());
        return append("assistant:" + messageId + ":hint", student.userId(), courseId, lessonId,
                EventType.AI_HINT_USED, "ASSISTANT", Instant.now(), payload);
    }

    private EventReceipt append(String eventId, long userId, Long courseId, Long lessonId,
                                 EventType type, String source, Instant occurredAt, JsonNode payload) {
        String encoded = encode(payload);
        String hash = DigestUtil.sha256Hex(userId + "|" + courseId + "|" + lessonId + "|" + type + "|" + source
                + "|" + (source.equals("CLIENT") ? occurredAt.toString() : "server") + "|" + encode(canonical(payload)));
        // The unique key handles concurrent duplicates; lookup never grants a foreign event identity.
        var row = new LearningEventEntity(); row.setEventId(eventId); row.setUserId(userId); row.setCourseId(courseId);
        row.setLessonId(lessonId); row.setType(type.name()); row.setSource(source); row.setOccurredAt(occurredAt);
        row.setPayload(encoded); row.setPayloadHash(hash); row.setProcessingStatus("PENDING"); row.setAttempts(0); row.setCreatedAt(Instant.now());
        boolean duplicate = false;
        try { events.insert(row); }
        catch (org.springframework.dao.DuplicateKeyException e) { duplicate = true; }
        var saved = events.selectOne(new LambdaQueryWrapper<LearningEventEntity>().eq(LearningEventEntity::getEventId, eventId));
        if (saved == null) throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        if (saved.getUserId() != userId) throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        if (!hash.equals(saved.getPayloadHash())) throw new BusinessException(40900, "Event ID reused with different content");
        return new EventReceipt(eventId, duplicate, saved.getProcessingStatus());
    }

    private Object canonical(JsonNode node) {
        if (node.isObject()) {
            var map = new TreeMap<String, Object>(); node.fields().forEachRemaining(e -> map.put(e.getKey(), canonical(e.getValue()))); return map;
        }
        if (node.isArray()) { var values = new ArrayList<Object>(); node.forEach(n -> values.add(canonical(n))); return values; }
        return node;
    }

    public int consumeBatch(int limit) {
        if (limit < 1 || limit > 100) throw invalid();
        int count = 0;
        for (long id : events.pending(Instant.now(), limit)) if (consume(id)) count++;
        return count;
    }

    public EventReceipt processAccepted(StudentContext student, EventReceipt receipt) {
        var row = events.selectOne(new LambdaQueryWrapper<LearningEventEntity>()
                .eq(LearningEventEntity::getEventId, receipt.eventId()).eq(LearningEventEntity::getUserId, student.userId()));
        if (row == null) throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        consume(row.getId());
        var current = events.selectById(row.getId());
        return new EventReceipt(receipt.eventId(), receipt.duplicate(), current.getProcessingStatus());
    }

    public boolean consume(long id) {
        String owner = UUID.randomUUID().toString(); var tx = new TransactionTemplate(transactions);
        Boolean claimed = tx.execute(status -> {
            var row = eventForUpdate(id);
            if (row == null || "PROCESSED".equals(row.getProcessingStatus()) || row.getAttempts() >= 3
                    || ("PROCESSING".equals(row.getProcessingStatus()) && row.getLeaseExpiresAt() != null
                    && row.getLeaseExpiresAt().isAfter(Instant.now()))) return false;
            row.setProcessingStatus("PROCESSING"); row.setAttempts(row.getAttempts() + 1);
            row.setLeaseOwner(owner); row.setLeaseExpiresAt(Instant.now().plusSeconds(30)); events.updateById(row); return true;
        });
        if (!Boolean.TRUE.equals(claimed)) return false;
        try {
            Boolean processed = tx.execute(status -> {
                var row = eventForUpdate(id);
                if (row == null || !"PROCESSING".equals(row.getProcessingStatus()) || !owner.equals(row.getLeaseOwner())) return false;
                if ("CLIENT".equals(row.getSource())) applyProgress(row);
                return events.update(null, new LambdaUpdateWrapper<LearningEventEntity>().eq(LearningEventEntity::getId, id)
                        .eq(LearningEventEntity::getLeaseOwner, owner).set(LearningEventEntity::getProcessingStatus, "PROCESSED")
                        .set(LearningEventEntity::getProcessedAt, Instant.now()).set(LearningEventEntity::getLeaseOwner, null)
                        .set(LearningEventEntity::getLeaseExpiresAt, null).set(LearningEventEntity::getError, null)) == 1;
            });
            return Boolean.TRUE.equals(processed);
        } catch (RuntimeException failure) {
            tx.executeWithoutResult(status -> events.update(null, new LambdaUpdateWrapper<LearningEventEntity>()
                    .eq(LearningEventEntity::getId, id).eq(LearningEventEntity::getLeaseOwner, owner)
                    .set(LearningEventEntity::getProcessingStatus, "FAILED").set(LearningEventEntity::getLeaseOwner, null)
                    .set(LearningEventEntity::getLeaseExpiresAt, null).set(LearningEventEntity::getError, "Processing failed")));
            return false;
        }
    }

    private LearningEventEntity eventForUpdate(long id) {
        return events.selectOne(new LambdaQueryWrapper<LearningEventEntity>().eq(LearningEventEntity::getId, id).last("FOR UPDATE"));
    }

    private void applyProgress(LearningEventEntity event) {
        progress.ensure(event.getUserId(), event.getLessonId(), event.getCourseId(), Instant.now());
        var row = progress.selectOne(new LambdaQueryWrapper<LessonProgressEntity>()
                .eq(LessonProgressEntity::getUserId, event.getUserId()).eq(LessonProgressEntity::getLessonId, event.getLessonId()).last("FOR UPDATE"));
        if (EventType.LESSON_MARKED_COMPLETE.name().equals(event.getType())) row.setSelfReportedComplete(1);
        if (EventType.RESOURCE_PROGRESS.name().equals(event.getType())) {
            PlatformRequests.ProgressPayload payload;
            try { payload = json.readValue(event.getPayload(), PlatformRequests.ProgressPayload.class); }
            catch (Exception e) { throw new IllegalArgumentException("Invalid saved event", e); }
            var states = new ArrayList<>(storedResources(row.getResourceProgress()));
            var prior = states.stream().filter(s -> s.resourceId().equals(payload.resourceId())
                    && s.resourceVersion().equals(payload.resourceVersion())).findFirst().orElse(null);
            var intervals = PlaybackIntervals.merge(prior == null ? List.of() : prior.intervals(), payload.intervals());
            boolean newest = prior == null || !event.getOccurredAt().isBefore(prior.lastOccurredAt());
            if (prior != null) states.remove(prior);
            states.add(new StoredResource(payload.resourceId(), payload.resourceVersion(), intervals,
                    newest ? payload.lastPosition() : prior.lastPosition(), newest ? event.getOccurredAt() : prior.lastOccurredAt()));
            row.setResourceProgress(encode(states));
        }
        row.setUpdatedAt(Instant.now()); progress.updateById(row);
    }

    private List<StoredResource> storedResources(String encoded) {
        try { return json.readValue(encoded, json.getTypeFactory().constructCollectionType(List.class, StoredResource.class)); }
        catch (Exception e) { throw new IllegalArgumentException("Invalid saved progress", e); }
    }

    @Override
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public LearningSummary getSummary(StudentContext student, long courseId, TimeRange range) {
        access.requireStudy(student, courseId); Objects.requireNonNull(range); Instant asOf = Instant.now();
        var query = new QueryWrapper<Submission>().select("status", "COUNT(*) AS cnt")
                .eq("user_id", student.userId()).eq("course_id", courseId)
                .le("create_time", Date.from(asOf))
                .ge(range.from() != null, "create_time", range.from() == null ? null : Date.from(range.from()))
                .lt(range.to() != null, "create_time", range.to() == null ? null : Date.from(range.to())).groupBy("status");
        var counts = new LinkedHashMap<String, Long>();
        for (var result : submissions.selectMaps(query)) counts.put((String) result.get("status"), ((Number) result.get("cnt")).longValue());
        long accepted = counts.getOrDefault("ACCEPTED", 0L);
        long judged = counts.entrySet().stream().filter(e -> !Set.of("PENDING", "JUDGING", "SYSTEM_ERROR").contains(e.getKey()))
                .mapToLong(java.util.Map.Entry::getValue).sum();
        var evidence = events.selectList(new LambdaQueryWrapper<LearningEventEntity>()
                .eq(LearningEventEntity::getUserId, student.userId()).eq(LearningEventEntity::getCourseId, courseId)
                .le(LearningEventEntity::getOccurredAt, asOf)
                .ge(range.from() != null, LearningEventEntity::getOccurredAt, range.from())
                .lt(range.to() != null, LearningEventEntity::getOccurredAt, range.to())
                .orderByDesc(LearningEventEntity::getOccurredAt, LearningEventEntity::getId).last("LIMIT 100"))
                .stream().map(e -> new Evidence(e.getEventId(), e.getType(), e.getOccurredAt())).toList();
        var lessonStates = progress.selectList(new LambdaQueryWrapper<LessonProgressEntity>()
                .eq(LessonProgressEntity::getUserId, student.userId()).eq(LessonProgressEntity::getCourseId, courseId)
                .orderByAsc(LessonProgressEntity::getLessonId)).stream().map(row -> new LessonProgress(format(row.getLessonId()),
                        Integer.valueOf(1).equals(row.getSelfReportedComplete()), row.getUpdatedAt(), storedResources(row.getResourceProgress())
                        .stream().map(s -> new ResourceProgress(s.resourceId(), s.resourceVersion(), s.intervals(),
                                PlaybackIntervals.seconds(s.intervals()), s.lastPosition())).toList())).toList();
        access.requireStudy(student, courseId);
        return new LearningSummary(format(courseId), range, asOf, accepted, judged,
                judged == 0 ? null : (double) accepted / judged, counts, lessonStates, evidence);
    }

    private String encode(Object value) {
        try { return json.writeValueAsString(value); }
        catch (Exception e) { throw new BusinessException(ErrorCode.SYSTEM_ERROR); }
    }
    private BusinessException invalid() { return new BusinessException(ErrorCode.PARAMS_ERROR); }
}
