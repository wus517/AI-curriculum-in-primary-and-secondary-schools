package com.example.oj.platform;

import com.example.oj.common.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CourseAccessServiceTest {
    private final CourseCatalog catalog = mock(CourseCatalog.class);
    private final EnrollmentLookup enrollment = mock(EnrollmentLookup.class);
    @SuppressWarnings("unchecked")
    private final ObjectProvider<EnrollmentLookup> provider = mock(ObjectProvider.class);
    private final DefaultCourseAccessService service = new DefaultCourseAccessService(catalog, provider);

    @ParameterizedTest
    @CsvSource({
            "published,false,false,true,false,false,true", "published,false,true,true,true,false,true",
            "draft,false,true,false,false,false,false", "offline,false,true,false,false,false,false",
            "published,true,false,true,true,true,true", "draft,true,false,true,true,true,false",
            "offline,true,false,true,true,true,false"
    })
    void permissionMatrix(String status, boolean owner, boolean joined, boolean discover,
                          boolean study, boolean manage, boolean enroll) {
        var student = new StudentContext(owner ? 11 : 12, "user", "matrix");
        when(catalog.find(1)).thenReturn(Optional.of(new CourseCatalog.CourseScope(1, 11, status, false)));
        when(provider.getIfAvailable()).thenReturn(enrollment);
        when(enrollment.isActive(student.userId(), 1)).thenReturn(joined);
        check(discover, () -> service.requireDiscover(student, 1));
        check(study, () -> service.requireStudy(student, 1));
        check(manage, () -> service.requireManage(student, 1));
        check(enroll, () -> service.requireEnroll(student, 1));
    }

    @Test
    void adminCanPreviewButCannotEnrollOfflineCourse() {
        when(catalog.find(1)).thenReturn(Optional.of(new CourseCatalog.CourseScope(1, 11, "offline", false)));
        var admin = new StudentContext(90, "admin", "admin");
        assertDoesNotThrow(() -> service.requireDiscover(admin, 1));
        assertDoesNotThrow(() -> service.requireStudy(admin, 1));
        assertDoesNotThrow(() -> service.requireManage(admin, 1));
        assertThrows(BusinessException.class, () -> service.requireEnroll(admin, 1));
        verifyNoInteractions(enrollment);
    }

    @Test
    void deletedCourseIsInvisibleEvenToAdmin() {
        when(catalog.find(1)).thenReturn(Optional.of(new CourseCatalog.CourseScope(1, 11, "published", true)));
        var admin = new StudentContext(90, "admin", "deleted");
        assertEquals(40400, assertThrows(BusinessException.class, () -> service.requireStudy(admin, 1)).getCode());
        assertThrows(BusinessException.class, () -> service.requireManage(admin, 1));
    }

    @Test
    void absentEnrollmentAdapterDeniesOrdinaryStudent() {
        when(catalog.find(1)).thenReturn(Optional.of(new CourseCatalog.CourseScope(1, 11, "published", false)));
        assertEquals(40300, assertThrows(BusinessException.class,
                () -> service.requireStudy(new StudentContext(12, "user", "missing"), 1)).getCode());
    }

    @Test
    void enrollmentAlwaysUsesActualStudent() {
        when(catalog.find(1)).thenReturn(Optional.of(new CourseCatalog.CourseScope(1, 11, "published", false)));
        when(provider.getIfAvailable()).thenReturn(enrollment);
        when(enrollment.isActive(12, 1)).thenReturn(true);
        assertDoesNotThrow(() -> service.requireStudy(new StudentContext(12, "user", "a"), 1));
        assertThrows(BusinessException.class, () -> service.requireStudy(new StudentContext(13, "user", "b"), 1));
        verify(enrollment).isActive(12, 1);
        verify(enrollment).isActive(13, 1);
    }

    private void check(boolean permitted, org.junit.jupiter.api.function.Executable operation) {
        if (permitted) assertDoesNotThrow(operation); else assertThrows(BusinessException.class, operation);
    }
}
