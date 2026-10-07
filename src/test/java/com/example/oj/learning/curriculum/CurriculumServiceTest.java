package com.example.oj.learning.curriculum;

import com.example.oj.common.BusinessException;
import com.example.oj.platform.CourseAccessService;
import com.example.oj.platform.PlatformRequests;
import com.example.oj.platform.StudentContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;
import com.example.oj.model.entity.User;
import com.example.oj.service.UserService;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import static com.example.oj.platform.PlatformDtos.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:curriculum;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;NON_KEYWORDS=USER",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:sql/legacy-fixture.sql,classpath:sql/migrations/V001__learning_curriculum.sql",
        "mybatis-plus.configuration.log-impl=org.apache.ibatis.logging.nologging.NoLoggingImpl"
})
class CurriculumServiceTest {
    @Autowired CurriculumService service;
    @Autowired CourseAccessService access;
    @Autowired JdbcTemplate sql;
    @Autowired MockMvc http;
    private final StudentContext owner = new StudentContext(11, "user", "owner");
    private final StudentContext alice = new StudentContext(12, "user", "alice");
    private final StudentContext bob = new StudentContext(13, "user", "bob");

    @BeforeEach
    void fixture() {
        for (String table : List.of("problem_knowledge_point", "lesson_knowledge_point", "lesson_problem", "lesson_resource",
                "knowledge_point", "course_lesson", "course_enrollment", "submission", "course_problem", "course_resource", "problem", "course", "user")) {
            sql.update("DELETE FROM " + table);
        }
        sql.update("INSERT INTO course (id,title,teacher_id,status,is_delete) VALUES (1,'published',11,'published',0),(2,'draft',11,'draft',0),(3,'other',13,'published',0)");
        sql.update("INSERT INTO `user` (id,username,password,role) VALUES (11,'owner','fixture','user'),(12,'alice','fixture','user'),(13,'bob','fixture','user')");
        sql.update("INSERT INTO problem (id,title,content,create_user_id,difficulty) VALUES (1,'A+B','Add integers',90,1),(2,'Loop','Loop boundaries',90,2)");
        sql.update("INSERT INTO course_problem (course_id,problem_id,sort_order) VALUES (1,1,0)");
        sql.update("INSERT INTO course_resource (id,course_id,resource_type,title,file_url,sort_order) VALUES (5,1,'video','fixture','/uploads/private.mp4',0),(6,3,'ppt','other','/uploads/private.pdf',0)");
    }

    @Test
    void restUsesSessionAndActualHttpErrorStatuses() throws Exception {
        http.perform(get("/api/learning/courses/1/outline")).andExpect(status().isUnauthorized());
        var session = new MockHttpSession(); var user = new User(); user.setId(12L); user.setRole("admin");
        session.setAttribute(UserService.USER_LOGIN_STATE, user);
        http.perform(post("/api/learning/enrollments").session(session).contentType(MediaType.APPLICATION_JSON)
                .content("{\"courseId\":\"1\",\"userId\":\"13\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.userId").value("12"));
        assertFalse(service.isActive(13, 1));
        http.perform(get("/api/learning/courses/2/outline").session(session))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value(40400));
        http.perform(get("/api/learning/courses/01/outline").session(session)).andExpect(status().isBadRequest());
        http.perform(post("/api/learning/courses/1/lessons").session(session).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Forbidden\",\"objectives\":null,\"sortOrder\":0,\"status\":\"PUBLISHED\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void enrollmentIsIdempotentAndNeverSelectsAnotherStudent() {
        var first = service.join(alice, 1); var repeated = service.join(alice, 1);
        assertEquals(first.id(), repeated.id()); assertEquals(first.joinedAt(), repeated.joinedAt());
        assertFalse(service.isActive(bob.userId(), 1));
        service.join(bob, 1); assertEquals(2, sql.queryForObject("SELECT COUNT(*) FROM course_enrollment", Integer.class));
        service.withdraw(alice, 1); assertFalse(service.isActive(alice.userId(), 1));
        assertEquals(first.id(), service.join(alice, 1).id());
        assertThrows(BusinessException.class, () -> service.join(alice, 2));
    }

    @Test
    void concurrentDuplicateJoinUsesDatabaseUniqueKey() throws Exception {
        var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> service.join(alice, 1));
            var second = executor.submit(() -> service.join(alice, 1));
            assertEquals(first.get(5, TimeUnit.SECONDS).id(), second.get(5, TimeUnit.SECONDS).id());
            assertEquals(1, sql.queryForObject("SELECT COUNT(*) FROM course_enrollment", Integer.class));
        } finally { executor.shutdownNow(); }
    }

    @Test
    void draftLessonsAndCourseChangesRespectCurrentPermissions() {
        var draft = service.createLesson(owner, 1, new PlatformRequests.LessonWrite("draft lesson", null, 0, LessonStatus.DRAFT));
        assertEquals(1, service.getOutline(owner, 1).lessons().size());
        assertTrue(service.getOutline(alice, 1).lessons().isEmpty());
        service.join(alice, 1);
        assertThrows(BusinessException.class, () -> service.getLesson(alice, 1, Long.parseLong(draft.id())));
        service.updateLesson(owner, 1, Long.parseLong(draft.id()), new PlatformRequests.LessonWrite("published", "Goal", 0, LessonStatus.PUBLISHED));
        assertEquals("published", service.getLesson(alice, 1, Long.parseLong(draft.id())).title());
        sql.update("UPDATE course SET status='offline' WHERE id=1");
        assertThrows(BusinessException.class, () -> access.requireStudy(alice, 1));
        assertDoesNotThrow(() -> access.requireStudy(owner, 1));
        sql.update("UPDATE course SET is_delete=1 WHERE id=1");
        assertThrows(BusinessException.class, () -> access.requireStudy(owner, 1));
    }

    @Test
    void associationsReactivateAndRejectForeignResourcesAndKnowledgePoints() {
        long lesson = Long.parseLong(service.createLesson(owner, 1,
                new PlatformRequests.LessonWrite("lesson", null, 0, LessonStatus.PUBLISHED)).id());
        var point = service.createPoint(owner, 1, new PlatformRequests.KnowledgePointWrite("Addition", null));
        var foreign = service.createPoint(bob, 3, new PlatformRequests.KnowledgePointWrite("Other", null));
        service.bindLesson(owner, 1, lesson, CurriculumService.LinkKind.resources, 5, 0);
        service.unbindLesson(owner, 1, lesson, CurriculumService.LinkKind.resources, 5);
        service.bindLesson(owner, 1, lesson, CurriculumService.LinkKind.resources, 5, 3);
        service.bindLesson(owner, 1, lesson, CurriculumService.LinkKind.resources, 5, 3);
        assertEquals(1, sql.queryForObject("SELECT COUNT(*) FROM lesson_resource", Integer.class));
        assertEquals("/api/learning/resources/5/content", service.getLesson(owner, 1, lesson).resources().get(0).contentUrl());
        assertThrows(BusinessException.class, () -> service.bindLesson(owner, 1, lesson, CurriculumService.LinkKind.resources, 6, 0));
        assertThrows(BusinessException.class, () -> service.bindLesson(owner, 1, lesson, CurriculumService.LinkKind.knowledge_points, Long.parseLong(foreign.id()), 0));
        service.bindLesson(owner, 1, lesson, CurriculumService.LinkKind.knowledge_points, Long.parseLong(point.id()), 0);
        service.bindProblemPoint(owner, 1, 1, Long.parseLong(point.id()), 0);
        service.unbindProblemPoint(owner, 1, 1, Long.parseLong(point.id()));
        service.bindProblemPoint(owner, 1, 1, Long.parseLong(point.id()), 0);
        assertEquals(1, sql.queryForObject("SELECT COUNT(*) FROM problem_knowledge_point", Integer.class));
        assertThrows(BusinessException.class, () -> service.deleteLesson(alice, 1, lesson));
    }

    @Test
    void submissionSourcesRequirePublishedLessonAndActualProblemAssociation() {
        long lesson = Long.parseLong(service.createLesson(owner, 1,
                new PlatformRequests.LessonWrite("lesson", null, 0, LessonStatus.PUBLISHED)).id());
        service.bindLesson(owner, 1, lesson, CurriculumService.LinkKind.problems, 1, 0);
        assertThrows(BusinessException.class, () -> service.requireSubmissionSource(alice, 1, lesson, 1));
        service.join(alice, 1);
        assertDoesNotThrow(() -> service.requireSubmissionSource(alice, 1, lesson, 1));
        assertThrows(BusinessException.class, () -> service.requireSubmissionSource(alice, 1, lesson, 2));
        assertThrows(BusinessException.class, () -> service.requireSubmissionSource(alice, 3, lesson, 1));
        service.deleteLesson(owner, 1, lesson);
        assertThrows(BusinessException.class, () -> service.requireSubmissionSource(alice, 1, lesson, 1));
    }
}
