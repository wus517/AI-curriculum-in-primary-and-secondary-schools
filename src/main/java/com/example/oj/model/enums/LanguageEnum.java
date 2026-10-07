package com.example.oj.model.enums;

/**
 * 编程语言枚举（第一版仅支持 Java）
 */
public enum LanguageEnum {

    JAVA("java", "Java");

    private final String value;
    private final String message;

    LanguageEnum(String value, String message) {
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
     * 判断语言是否被支持
     */
    public static boolean isSupport(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        for (LanguageEnum languageEnum : LanguageEnum.values()) {
            if (languageEnum.value.equals(value)) {
                return true;
            }
        }
        return false;
    }
}
