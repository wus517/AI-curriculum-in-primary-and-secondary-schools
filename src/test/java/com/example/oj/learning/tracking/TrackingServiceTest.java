package com.example.oj.learning.tracking;

import com.example.oj.common.BusinessException;
import com.example.oj.learning.curriculum.CurriculumService;
import com.example.oj.learning.tracking.entity.LearningEventEntity;
import com.example.oj.learning.tracking.mapper.LearningEventEntityMapper;
import com.example.oj.mapper.SubmissionMapper;
import com.example.oj.model.entity.Submission;
import com.example.oj.model.entity.User;
import com.example.oj.platform.PlatformRequests;
import com.example.oj.platform.StudentContext;
import com.example.oj.service.UserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.example.oj.platform.PlatformDtos.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:tracking;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;NON_KEYWORDS=USER",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:sql/legacy-fixture.sql,classpath:sql/migrations/V001__learning_curriculum.sql,classpath:sql/migrations/V002__learning_tracking.sql,classpath:sql/tracking-json-h2.sql",
        "mybatis-plus.configuration.log-impl=org.apache.ibatis.logging.nologging.NoLoggingImpl",
        "logging.level.com.example.oj=INFO"
})
class TrackingServiceTest {
    @Autowired TrackingService tracking;
    @Autowired SubmissionQueries queries;
    @Autowired JudgedResultService judge;
    @Autowired CurriculumService curriculum;
    @Autowired SubmissionMapper submissions;
    @Autowired LearningEventEntityMapper events;
    @Autowired JdbcTemplate sql;
    @Autowired ObjectMapper json;
    @Autowired MockMvc http;
    private final StudentContext alice = new StudentContext(12, "user", "alice");
    private final StudentContext bob = new StudentContext(13, "user", "bob");
    private final StudentContext admin = new StudentContext(14, "admin", "admin");
    private Instant now;

    @BeforeEach
    void fixture() {
        for (String table : List.of("learning_event", "lesson_progress", "problem_knowledge_point", "lesson_knowledge_point",
                "lesson_problem", "lesson_resource", "knowledge_point", "course_lesson", "course_enrollment", "submission",
                "course_problem", "course_resource", "test_case", "problem", "course", "user")) sql.update("DELETE FROM " + table);
        now = Instant.now().truncatedTo(ChronoUnit.MILLIS).minusSeconds(10);
        sql.update("INSERT INTO course (id,title,teacher_id,status,is_delete) VALUES (1,'course',11,'published',0),(2,'other',11,'published',0)");
        sql.update("INSERT INTO `user` (id,username,password,role) VALUES (12,'alice','fixture','user'),(13,'bob','fixture','user'),(14,'admin','fixture','admin')");
        sql.update("INSERT INTO problem (id,title,content,create_user_id,difficulty) VALUES (1,'A+B','Public statement',11,1),(2,'Other','Other statement',11,2)");
        sql.update("INSERT INTO test_case (id,problem_id,input,output) VALUES (1,1,'hidden input','hidden output')");
        sql.update("INSERT INTO course_problem (course_id,problem_id,sort_order) VALUES (1,1,0),(2,2,0)");
        sql.update("INSERT INTO course_lesson (id,course_id,title,sort_order,status,active,created_at,updated_at) VALUES (10,1,'lesson',0,'PUBLISHED',1,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)");
        sql.update("INSERT INTO course_resource (id,course_id,resource_type,title,file_url,sort_order) VALUES (5,1,'video','video','/uploads/private.mp4',0)");
        sql.update("INSERT INTO lesson_resource (lesson_id,resource_id,sort_order,active) VALUES (10,5,0,1)");
        curriculum.join(alice, 1); curriculum.join(bob, 1);
    }

    @Test
    void duplicateEventIsIdempotentButChangedContentAndForeignIdentityAreRejected() {
        var request = event(EventType.LESSON_OPENED, now, json.createObjectNode());
        assertFalse(tracking.accept(alice, request).duplicate());
        assertTrue(tracking.accept(alice, request).duplicate());
        assertEquals(1, count("learning_event"));
        var changed = new PlatformRequests.ClientEvent(request.eventId(), "1", "10", EventType.LESSON_MARKED_COMPLETE, now, request.payload());
        assertEquals(40900, assertThrows(BusinessException.class, () -> tracking.accept(alice, changed)).getCode());
        assertEquals(40400, assertThrows(BusinessException.class, () -> tracking.accept(bob, request)).getCode());
        assertEquals(now, events.selectList(null).get(0).getOccurredAt());
    }

    @Test
    void concurrentDuplicateEventsAndConsumersUseDatabaseIdempotency() throws Exception {
        var request = event(EventType.RESOURCE_PROGRESS, now, progress("5", "5", "[[0,10]]", 10));
        var pool = Executors.newFixedThreadPool(2);
        try {
            var a = pool.submit(() -> tracking.accept(alice, request));
            var b = pool.submit(() -> tracking.accept(alice, request));
            assertNotEquals(a.get(5, TimeUnit.SECONDS).duplicate(), b.get(5, TimeUnit.SECONDS).duplicate());
            long id = events.selectList(null).get(0).getId();
            var c = pool.submit(() -> tracking.consume(id)); var d = pool.submit(() -> tracking.consume(id));
            assertNotEquals(c.get(5, TimeUnit.SECONDS), d.get(5, TimeUnit.SECONDS));
            assertEquals(1, count("learning_event")); assertEquals(1, count("lesson_progress"));
            assertEquals(10, resource().recordedSeconds());
        } finally { pool.shutdownNow(); }
    }

    @Test
    void clientCannotForgeBackendEventsOrInvalidResourceTimeAndIntervals() throws Exception {
        for (EventType type : List.of(EventType.SUBMISSION_JUDGED, EventType.AI_HINT_USED))
            assertThrows(BusinessException.class, () -> tracking.accept(alice, event(type, now, json.createObjectNode())));
        assertThrows(BusinessException.class, () -> tracking.accept(alice, event(EventType.LESSON_OPENED, now.plusSeconds(1000), json.createObjectNode())));
        assertThrows(BusinessException.class, () -> tracking.accept(alice, event(EventType.LESSON_OPENED, now.minus(31, ChronoUnit.DAYS), json.createObjectNode())));
        assertThrows(BusinessException.class, () -> tracking.accept(alice, event(EventType.RESOURCE_PROGRESS, now, progress("5", "wrong", "[[0,10]]", 10))));
        assertThrows(BusinessException.class, () -> tracking.accept(alice, event(EventType.RESOURCE_PROGRESS, now, progress("6", "6", "[[0,10]]", 10))));
        assertThrows(BusinessException.class, () -> tracking.accept(alice, event(EventType.RESOURCE_PROGRESS, now, progress("5", "5", "[[0,101]]", 10))));
        assertThrows(BusinessException.class, () -> tracking.accept(alice, event(EventType.RESOURCE_PROGRESS, now, progress("5", "5", "[[-1,10]]", 10))));
        assertThrows(BusinessException.class, () -> tracking.accept(alice, event(EventType.RESOURCE_PROGRESS, now, progress("5", "5", "[[10,10]]", 10))));
        var unknown = progress("5", "5", "[]", 0).deepCopy(); ((com.fasterxml.jackson.databind.node.ObjectNode) unknown).put("userId", "13");
        assertThrows(BusinessException.class, () -> tracking.accept(alice, event(EventType.RESOURCE_PROGRESS, now, unknown)));
        curriculum.withdraw(alice, 1);
        assertThrows(BusinessException.class, () -> tracking.accept(alice, event(EventType.LESSON_OPENED, now, json.createObjectNode())));
        assertEquals(0, count("learning_event"));
    }

    @Test
    void overlappingAndOutOfOrderReportsMergeWithoutCountingSkippedPlayback() {
        ingest(event(EventType.RESOURCE_PROGRESS, now, progress("5", "5", "[[0,10],[5,20],[90,100]]", 100)));
        assertEquals(30, resource().recordedSeconds());
        ingest(event(EventType.RESOURCE_PROGRESS, now.minusSeconds(2), progress("5", "5", "[[10,25]]", 25)));
        assertEquals(List.of(new Interval(0, 25), new Interval(90, 100)), resource().intervals());
        assertEquals(35, resource().recordedSeconds()); assertEquals(100, resource().lastPosition());
        var receipt = tracking.accept(alice, event(EventType.RESOURCE_PROGRESS, now.plusSeconds(1), progress("5", "5", "[]", 30)));
        tracking.processAccepted(alice, receipt); tracking.processAccepted(alice, receipt);
        assertEquals(35, resource().recordedSeconds()); assertEquals(30, resource().lastPosition());
    }

    @Test
    void replacingImmutableResourceRetainsSeparateVersionsAndSelfReportedCompletion() {
        ingest(event(EventType.RESOURCE_PROGRESS, now, progress("5", "5", "[[0,10]]", 10)));
        sql.update("UPDATE lesson_resource SET active=0 WHERE resource_id=5");
        sql.update("INSERT INTO course_resource (id,course_id,resource_type,title) VALUES (7,1,'video','new version')");
        sql.update("INSERT INTO lesson_resource (lesson_id,resource_id,sort_order,active) VALUES (10,7,0,1)");
        ingest(event(EventType.RESOURCE_PROGRESS, now.plusSeconds(1), progress("7", "7", "[[0,20]]", 20)));
        ingest(event(EventType.LESSON_MARKED_COMPLETE, now.plusSeconds(2), json.createObjectNode()));
        var summary = tracking.getSummary(alice, 1, new TimeRange(null, null));
        assertTrue(summary.lessons().get(0).selfReportedComplete());
        assertEquals(2, summary.lessons().get(0).resources().size());
        assertEquals(3, summary.evidence().size());
        assertThrows(BusinessException.class, () -> tracking.accept(alice, event(EventType.RESOURCE_PROGRESS, now, progress("5", "5", "[]", 0))));
    }

    @Test
    void expiredLeasesRecoverWhileLiveLeasesAndProcessedRowsAreNotConsumedAgain() {
        var receipt = tracking.accept(alice, event(EventType.LESSON_OPENED, now, json.createObjectNode()));
        long id = events.selectList(null).get(0).getId();
        var row = events.selectById(id); row.setProcessingStatus("PROCESSING"); row.setAttempts(1);
        row.setLeaseOwner("crashed-worker"); row.setLeaseExpiresAt(Instant.now().plusSeconds(60)); events.updateById(row);
        assertEquals(0, tracking.consumeBatch(10)); assertFalse(tracking.consume(id));
        row.setLeaseExpiresAt(Instant.now().minusSeconds(1)); events.updateById(row);
        assertEquals(1, tracking.consumeBatch(10)); assertEquals(0, tracking.consumeBatch(10));
        var processed = events.selectById(id);
        assertEquals(2, processed.getAttempts()); assertNull(processed.getLeaseOwner()); assertNull(processed.getLeaseExpiresAt());
        assertEquals("PROCESSED", tracking.processAccepted(alice, receipt).processingStatus());
    }

    @Test
    void failedProgressRollsBackAndStopsAfterBoundedRetries() {
        tracking.accept(alice, event(EventType.RESOURCE_PROGRESS, now, progress("5", "5", "[[0,10]]", 10)));
        sql.update("UPDATE learning_event SET payload='{}'");
        long id = events.selectList(null).get(0).getId();
        for (int i = 0; i < 3; i++) { assertFalse(tracking.consume(id)); assertEquals(0, count("lesson_progress")); }
        assertEquals("FAILED", events.selectById(id).getProcessingStatus());
        assertEquals(3, events.selectById(id).getAttempts()); assertEquals(0, tracking.consumeBatch(10));
    }

    @Test
    void summaryHasNullZeroRateAndExcludesIndependentForeignAndSystemErrorSubmissions() {
        assertNull(tracking.getSummary(alice, 1, new TimeRange(null, null)).passRate());
        submission(100, 12, 1L, "ACCEPTED", now); submission(101, 12, 1L, "WRONG_ANSWER", now);
        submission(102, 12, 1L, "SYSTEM_ERROR", now); submission(103, 12, 1L, "PENDING", now);
        submission(104, 12, 1L, "JUDGING", now); submission(105, 12, null, "ACCEPTED", now);
        submission(106, 13, 1L, "ACCEPTED", now); submission(107, 12, 2L, "ACCEPTED", now);
        ingest(event(EventType.LESSON_OPENED, now, json.createObjectNode()));
        var summary = tracking.getSummary(alice, 1, new TimeRange(null, null));
        assertEquals(1, summary.accepted()); assertEquals(2, summary.judged()); assertEquals(0.5, summary.passRate());
        assertEquals(1, summary.statusCounts().get("SYSTEM_ERROR")); assertEquals(5, summary.statusCounts().size());
        assertEquals(events.selectList(null).get(0).getEventId(), summary.evidence().get(0).id());
        assertTrue(tracking.getSummary(bob, 1, new TimeRange(null, null)).evidence().isEmpty());
        curriculum.withdraw(alice, 1);
        assertThrows(BusinessException.class, () -> tracking.getSummary(alice, 1, new TimeRange(null, null)));
    }

    @Test
    void timeRangeIsInclusiveExclusiveButProgressRemainsCumulative() {
        submission(100, 12, 1L, "ACCEPTED", now.minusSeconds(1));
        submission(101, 12, 1L, "WRONG_ANSWER", now); submission(102, 12, 1L, "ACCEPTED", now.plusSeconds(1));
        ingest(event(EventType.RESOURCE_PROGRESS, now.minusSeconds(1), progress("5", "5", "[[0,10]]", 10)));
        var summary = tracking.getSummary(alice, 1, new TimeRange(now, now.plusSeconds(1)));
        assertEquals(0, summary.accepted()); assertEquals(1, summary.judged()); assertEquals(0, summary.passRate());
        assertTrue(summary.evidence().isEmpty()); assertEquals(10, summary.lessons().get(0).resources().get(0).recordedSeconds());
        assertThrows(IllegalArgumentException.class, () -> new TimeRange(now, now));
    }

    @Test
    void personalDetailsNeverExposeAnotherStudentsCodeEvenToAdminAndRecheckCourseAccess() {
        submission(100, 12, 1L, "ACCEPTED", now);
        assertEquals(40400, assertThrows(BusinessException.class, () -> queries.getMine(bob, 100)).getCode());
        assertEquals(40400, assertThrows(BusinessException.class, () -> queries.getMine(admin, 100)).getCode());
        assertEquals("Public statement", queries.getMine(alice, 100).publicContent());
        sql.update("UPDATE course SET status='offline' WHERE id=1");
        assertThrows(BusinessException.class, () -> queries.getMine(alice, 100));
        submission(101, 12, null, "COMPILE_ERROR", now);
        assertDoesNotThrow(() -> queries.getMine(alice, 101));
    }

    @Test
    void personalCursorAndDetailAreBoundedAndErrorsAreSanitized() throws Exception {
        submission(100, 12, null, "COMPILE_ERROR", now); submission(101, 12, null, "ACCEPTED", now);
        submission(102, 13, null, "ACCEPTED", now);
        sql.update("UPDATE submission SET code=?,error_message=? WHERE id=100", "x".repeat(100001),
                "password=fixture-secret token=fixture-token Bearer abc.def /private/tmp/sandbox/Main.java C:\\sandbox\\Main.java " + "e".repeat(2200));
        var page = queries.listMine(alice, null, null, 1, null);
        assertEquals("101", page.items().get(0).id()); assertEquals("101", page.nextCursor());
        var next = queries.listMine(alice, null, null, 1, page.nextCursor());
        assertEquals("100", next.items().get(0).id()); assertNull(next.nextCursor());
        assertThrows(BusinessException.class, () -> queries.listMine(alice, null, null, 21, null));
        assertThrows(BusinessException.class, () -> queries.listMine(alice, null, null, 1, "1 OR 1=1"));
        var detail = queries.getMine(alice, 100);
        assertEquals(100000, detail.code().length()); assertTrue(detail.codeTruncated());
        assertTrue(detail.errorSummary().length() <= 2000);
        assertFalse(detail.errorSummary().contains("fixture-secret")); assertFalse(detail.errorSummary().contains("abc.def"));
        assertFalse(detail.errorSummary().contains("sandbox"));
        String output = json.writeValueAsString(detail);
        assertFalse(output.contains("hidden input")); assertFalse(output.contains("hidden output"));
    }

    @Test
    void httpUsesActualSessionIdentityRejectsForgeryAndReturnsProgressAndPrivacyStatuses() throws Exception {
        http.perform(get("/api/learning/courses/1/summary")).andExpect(status().isUnauthorized());
        var session = new MockHttpSession(); var user = new User(); user.setId(12L); user.setRole("admin");
        session.setAttribute(UserService.USER_LOGIN_STATE, user);
        String request = json.writeValueAsString(event(EventType.LESSON_OPENED, now, json.createObjectNode()));
        request = request.substring(0, request.length() - 1) + ",\"userId\":\"13\"}";
        http.perform(post("/api/learning/events").session(session).contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.processingStatus").value("PROCESSED"));
        assertEquals(12, events.selectList(null).get(0).getUserId());
        http.perform(post("/api/learning/events").session(session).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(event(EventType.SUBMISSION_JUDGED, now, json.createObjectNode()))))
                .andExpect(status().isBadRequest());
        http.perform(get("/api/learning/courses/1/summary").session(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.passRate").isEmpty());
        http.perform(get("/api/learning/courses/1/summary").session(session).param("from", now.toString()).param("to", now.toString()))
                .andExpect(status().isBadRequest());
        submission(100, 13, null, "ACCEPTED", now);
        http.perform(get("/api/learning/submissions/100").session(session)).andExpect(status().isNotFound());
        http.perform(get("/api/learning/submissions").session(session).param("pageSize", "21")).andExpect(status().isBadRequest());
    }

    @Test
    void terminalResultAndEventCommitTogetherAndLateSystemErrorDoesNotOverwriteSuccess() {
        submission(100, 12, 1L, "JUDGING", now);
        sql.update("UPDATE submission SET error_message='old transient error' WHERE id=100");
        judge.save(result(100, "ACCEPTED")); judge.save(result(100, "SYSTEM_ERROR")); judge.save(result(100, "ACCEPTED"));
        var saved = submissions.selectById(100);
        assertEquals("ACCEPTED", saved.getStatus()); assertEquals(1, saved.getJudgedRevision()); assertNull(saved.getErrorMessage());
        assertEquals(1, count("learning_event"));
        var event = events.selectList(null).get(0);
        assertEquals("submission:100:judged:1", event.getEventId()); assertEquals("JUDGE", event.getSource());
        assertEquals(1, tracking.consumeBatch(10)); assertEquals(0, count("lesson_progress"));
        submission(101, 12, null, "JUDGING", now); judge.save(result(101, "SYSTEM_ERROR"));
        assertNull(events.selectList(null).stream().filter(e -> e.getEventId().equals("submission:101:judged:1")).findFirst().orElseThrow().getCourseId());
        assertEquals(1, tracking.getSummary(alice, 1, new TimeRange(null, null)).judged());
        assertThrows(BusinessException.class, () -> judge.save(result(100, "JUDGING")));
    }

    @Test
    void eventConflictRollsBackTerminalResultInTheSameDatabaseTransaction() {
        submission(100, 12, 1L, "JUDGING", now);
        sql.update("INSERT INTO learning_event (event_id,user_id,course_id,type,source,occurred_at,payload,payload_hash,created_at) VALUES ('submission:100:judged:1',12,1,'SUBMISSION_JUDGED','JUDGE',CURRENT_TIMESTAMP,'{}',?,CURRENT_TIMESTAMP)", "0".repeat(64));
        assertThrows(BusinessException.class, () -> judge.save(result(100, "ACCEPTED")));
        assertEquals("JUDGING", submissions.selectById(100).getStatus());
        assertEquals(0, submissions.selectById(100).getJudgedRevision()); assertEquals(1, count("learning_event"));
    }

    @Test
    void backendHintIsIdempotentAndEnforcesStudyAndAllowedLevels() {
        assertFalse(tracking.recordHint(alice, 1, 10L, 200, AssistanceLevel.HINT).duplicate());
        assertTrue(tracking.recordHint(alice, 1, 10L, 200, AssistanceLevel.HINT).duplicate());
        assertThrows(BusinessException.class, () -> tracking.recordHint(alice, 1, 10L, 201, AssistanceLevel.NONE));
        assertThrows(BusinessException.class, () -> tracking.recordHint(alice, 2, null, 201, AssistanceLevel.HINT));
        assertEquals(1, tracking.consumeBatch(10)); assertEquals(0, count("lesson_progress"));
    }

    @Test
    void practiceAcceptedUsesAllOfTheStudentsCoursesAndIndependentHistory() {
        submission(100, 12, null, "ACCEPTED", now);
        assertTrue(curriculum.listPracticeCandidates(alice, 1, List.of(), null, 10).get(0).accepted());
        sql.update("UPDATE submission SET course_id=2 WHERE id=100");
        assertTrue(curriculum.listPracticeCandidates(alice, 1, List.of(), null, 10).get(0).accepted());
        assertFalse(curriculum.listPracticeCandidates(bob, 1, List.of(), null, 10).get(0).accepted());
        assertEquals(0, tracking.getSummary(alice, 1, new TimeRange(null, null)).accepted());
    }

    private int count(String table) { return sql.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class); }
    private PlatformRequests.ClientEvent event(EventType type, Instant time, JsonNode payload) {
        return new PlatformRequests.ClientEvent(UUID.randomUUID().toString(), "1", "10", type, time, payload);
    }
    private void ingest(PlatformRequests.ClientEvent request) {
        assertEquals("PROCESSED", tracking.processAccepted(alice, tracking.accept(alice, request)).processingStatus());
    }
    private ResourceProgress resource() {
        return tracking.getSummary(alice, 1, new TimeRange(null, null)).lessons().get(0).resources().get(0);
    }
    private JsonNode progress(String resource, String version, String intervals, double position) {
        try {
            var node = json.createObjectNode(); node.put("resourceId", resource); node.put("resourceVersion", version);
            var array = node.putArray("intervals");
            for (var pair : json.readTree(intervals)) { var interval = array.addObject(); interval.put("start", pair.get(0).asDouble()); interval.put("end", pair.get(1).asDouble()); }
            node.put("lastPosition", position); node.put("durationSeconds", 100); return node;
        } catch (Exception e) { throw new AssertionError(e); }
    }
    private void submission(long id, long user, Long course, String status, Instant time) {
        sql.update("INSERT INTO submission (id,user_id,problem_id,course_id,lesson_id,language,code,status,create_time,assistance_level) VALUES (?,?,1,?,?,'java','public class Main {}',?,?,'UNKNOWN')",
                id, user, course, course == null ? null : 10L, status, Timestamp.from(time));
    }
    private Submission result(long id, String status) { var result = new Submission(); result.setId(id); result.setStatus(status); return result; }
}
