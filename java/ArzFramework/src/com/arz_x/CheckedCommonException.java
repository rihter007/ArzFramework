package com.arz_x;

/**
 * Created by Rihter on 18.01.2015.
 * Represents common operation exception
 */
public class CheckedCommonException extends CheckedResultCodeException {
    public CheckedCommonException(CommonResultCode value) {
        super(value.getValue());
    }

    public CheckedCommonException(CommonResultCode resultCode, String message) {
        super(resultCode.getValue(), message);
    }

    public CommonResultCode getResultCode() {
        return CommonResultCode.getTypeByValue(super.getRawResultCode());
    }
}
