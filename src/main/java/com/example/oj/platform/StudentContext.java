package com.example.oj.platform;

import java.util.Objects;

public record StudentContext(long userId, String role, String requestId) {
    public StudentContext {
        if (userId <= 0) throw new IllegalArgumentException("userId must be positive");
        if (!"user".equals(role) && !"admin".equals(role)) {
            throw new IllegalArgumentException("Unknown role");
        }
        Objects.requireNonNull(requestId, "requestId");
        if (requestId.isBlank() || requestId.length() > 128) {
            throw new IllegalArgumentException("Invalid requestId");
        }
    }

    public boolean isAdmin() { return "admin".equals(role); }
}
