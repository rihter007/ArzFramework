package com.arz_x;

/**
 * Created by Rihter on 26.04.2015.
 * Base class for all result code exceptions
 */
public class CheckedResultCodeException extends Exception implements IExceptionResultCodeGetter {
    private ResultCodeContainer resultCodeContainer;

    public CheckedResultCodeException(int resultCode) {
        this(resultCode, null);
    }

    public CheckedResultCodeException(int resultCode, String message) {
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
