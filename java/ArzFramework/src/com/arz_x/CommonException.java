package com.arz_x;

/**
 * Created by Rihter on 22.11.2015.
 * Runtime version of common exception
 */
public class CommonException extends ResultCodeException {
    public CommonException(CommonResultCode value) {
        super(value.getValue());
    }

    public CommonException(CommonResultCode resultCode, String message) {
        super(resultCode.getValue(), message);
    }

    public CommonResultCode getResultCode() {
        return CommonResultCode.getTypeByValue(super.getRawResultCode());
    }
}
