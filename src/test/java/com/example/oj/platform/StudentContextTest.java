package com.example.oj.platform;

import com.example.oj.common.BusinessException;
import com.example.oj.model.entity.User;
import com.example.oj.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StudentContextTest {
    @Test
    void refreshesSessionRoleAndDoesNotTrustRequestIdentity() {
        var users = mock(UserService.class);
        var request = new MockHttpServletRequest();
        request.addParameter("userId", "99");
        request.addHeader("X-User-Role", "admin");
        User stale = new User(); stale.setId(12L); stale.setRole("admin");
        User current = new User(); current.setId(12L); current.setRole("user"); current.setIsDelete(0);
        when(users.getLoginUser(request)).thenReturn(stale);
        when(users.getById(12L)).thenReturn(current);
        var context = new StudentContextFactory(users).from(request);
        assertEquals(12, context.userId());
        assertFalse(context.isAdmin());
        current.setRole("admin");
        assertFalse(context.isAdmin());
    }

    @Test
    void rejectsDeletedSessionUser() {
        var users = mock(UserService.class);
        var request = new MockHttpServletRequest();
        User stale = new User(); stale.setId(12L);
        when(users.getLoginUser(request)).thenReturn(stale);
        assertEquals(40100, assertThrows(BusinessException.class,
                () -> new StudentContextFactory(users).from(request)).getCode());
    }

    @Test
    void rejectsMalformedIdentityAndIds() {
        assertThrows(IllegalArgumentException.class, () -> new StudentContext(0, "user", "request"));
        assertThrows(IllegalArgumentException.class, () -> new StudentContext(1, "teacher", "request"));
        assertThrows(IllegalArgumentException.class, () -> new StudentContext(1, "user", ""));
        for (String id : new String[]{"0", "-1", "01", " 1", "1.0", "9223372036854775808"}) {
            assertThrows(BusinessException.class, () -> ContractIds.parse(id));
        }
        assertEquals(Long.MAX_VALUE, ContractIds.parse("9223372036854775807"));
        assertEquals("9007199254740993", ContractIds.format(9007199254740993L));
    }
}
