package com.example.oj.platform;

import com.example.oj.common.BusinessException;
import com.example.oj.common.ErrorCode;
import com.example.oj.model.entity.User;
import com.example.oj.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class StudentContextFactory {
    private final UserService users;

    public StudentContextFactory(UserService users) { this.users = users; }

    public StudentContext from(HttpServletRequest request) {
        User sessionUser = users.getLoginUser(request);
        // Revalidate account and role so a stale Session cannot retain revoked access.
        User current = users.getById(sessionUser.getId());
        if (current == null || Integer.valueOf(1).equals(current.getIsDelete())) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        if (!"admin".equals(current.getRole()) && !"user".equals(current.getRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        return new StudentContext(current.getId(), current.getRole(), UUID.randomUUID().toString());
    }
}
