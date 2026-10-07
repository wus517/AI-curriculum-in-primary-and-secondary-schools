package com.example.oj.model.enums;

/**
 * 课程状态枚举
 */
public enum CourseStatusEnum {

    DRAFT("draft", "草稿"),
    PUBLISHED("published", "已发布"),
    OFFLINE("offline", "已下线");

    private final String value;
    private final String message;

    CourseStatusEnum(String value, String message) {
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
     * 判断状态值是否合法
     */
    public static boolean isSupport(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        for (CourseStatusEnum statusEnum : CourseStatusEnum.values()) {
            if (statusEnum.value.equals(value)) {
                return true;
            }
        }
        return false;
    }
}
