package com.example.oj.platform;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record PlatformResponse<T>(int code, T data, String message) {
    public static <T> PlatformResponse<T> success(T value) { return new PlatformResponse<>(0, value, "ok"); }
    public static PlatformResponse<Void> error(int code, String message) { return new PlatformResponse<>(code, null, message); }
}
