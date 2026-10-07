package com.example.oj.platform;

import com.example.oj.common.BusinessException;
import com.example.oj.common.ErrorCode;

public final class ContractIds {
    public static final String PATTERN = "^[1-9][0-9]{0,18}$";

    private ContractIds() {}

    public static long parse(String value) {
        if (value == null || !value.matches(PATTERN)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Invalid ID");
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "ID exceeds signed BIGINT");
        }
    }

    public static String format(long value) {
        if (value <= 0) throw new IllegalArgumentException("Invalid ID");
        return Long.toString(value);
    }
}
