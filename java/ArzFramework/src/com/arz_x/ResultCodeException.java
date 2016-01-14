package com.arz_x;

/**
 * Created by Rihter on 22.11.2015.
 * General runtime exception
 */
public class ResultCodeException extends RuntimeException implements IExceptionResultCodeGetter {
    private ResultCodeContainer resultCodeContainer;

    public ResultCodeException(int resultCode) {
        this(resultCode, null);
    }

    public ResultCodeException(int resultCode, String message) {
        super(message);
        this.resultCodeContainer = new ResultCodeContainer(resultCode);
    }

    @Override
    public int getRawResultCode() {
        return this.resultCodeContainer.geResultCode();
    }

    @Override
    public String getMessage() {
        return this.resultCodeContainer.getErrorMessage(super.getMessage());
    }

    @Override
    public String toString() {
        return getClass().getName() + "'" + getMessage() + "'";
    }
}
