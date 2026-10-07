package com.example.oj.model.enums;

/**
 * 课程资源类型枚举
 */
public enum CourseResourceTypeEnum {

    VIDEO("video", "视频"),
    PPT("ppt", "课件");

    private final String value;
    private final String message;

    CourseResourceTypeEnum(String value, String message) {
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
    public static CourseResourceTypeEnum getEnumByValue(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        for (CourseResourceTypeEnum typeEnum : CourseResourceTypeEnum.values()) {
            if (typeEnum.value.equals(value)) {
                return typeEnum;
            }
        }
        return null;
    }

    /**
     * 判断类型值是否合法
     */
    public static boolean isSupport(String value) {
        return getEnumByValue(value) != null;
    }
}
