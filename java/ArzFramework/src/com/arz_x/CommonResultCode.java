package com.arz_x;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Rihter on 18.01.2015.
 * Represents common operation result code
 *
 * From c++ header arz/rtl/result_code.h
 * #define DECLARE_SUCC_RESULT_CODE(facility, errorCode) (facility << 16) + errorCode
 * #define DECLARE_FAIL_RESULT_CODE(facility, errorCode) (1 << 31) + (facility << 16) + errorCode
 */
public enum CommonResultCode {
    Unknown(0x833c0000),
    UnExpected(0x833c0001),
    NoImplented(0x833c0002),
    NotFound(0x833c0003),
    AlreadyDone(0x833c0004),
    InvalidState(0x833c0005),
    InvalidParameter(0x833c0006),
    NoInterface(0x833c0007),
    AlreadyExists(0x833c0008),
    AccessIsDenied(0x833c009);

    public static final int FACILITY = 0x33C; // 000011||001111||00

    private static final Map<Integer, CommonResultCode> TypesMap = new HashMap<Integer, CommonResultCode>() {
        {
            for (CommonResultCode code : CommonResultCode.values())
                put(code.getValue(), code);
        }
    };

    private int resultCode;

    CommonResultCode(int value) {
        this.resultCode = value;
    }

    public int getValue() {
        return this.resultCode;
    }

    public static CommonResultCode getTypeByValue(int value) {
        return TypesMap.get(value);
    }
}
