package com.example.oj.model.enums;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 判题状态枚举
 */
public enum JudgeStatusEnum {

    PENDING("PENDING", "等待判题"),
    JUDGING("JUDGING", "判题中"),
    ACCEPTED("ACCEPTED", "通过"),
    WRONG_ANSWER("WRONG_ANSWER", "答案错误"),
    COMPILE_ERROR("COMPILE_ERROR", "编译错误"),
    RUNTIME_ERROR("RUNTIME_ERROR", "运行错误"),
    TIME_LIMIT_EXCEEDED("TIME_LIMIT_EXCEEDED", "超时"),
    MEMORY_LIMIT_EXCEEDED("MEMORY_LIMIT_EXCEEDED", "超内存"),
    SYSTEM_ERROR("SYSTEM_ERROR", "系统错误");

    private final String value;
    private final String message;

    JudgeStatusEnum(String value, String message) {
        this.value = value;
        this.message = message;
    }

    public String getValue() {
        return value;
    }

    public String getMessage() {
        return message;
    }

    /**
     * 根据 value 获取枚举
     */
    public static JudgeStatusEnum getEnumByValue(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        for (JudgeStatusEnum statusEnum : JudgeStatusEnum.values()) {
            if (statusEnum.value.equals(value)) {
                return statusEnum;
            }
        }
        return null;
    }

    /**
     * 获取全部枚举值列表
     */
    public static List<String> getValues() {
        return Arrays.stream(values()).map(JudgeStatusEnum::getValue).collect(Collectors.toList());
    }
}
