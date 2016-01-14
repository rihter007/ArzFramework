package com.arz_x;

/**
 * Created by Rihter on 22.11.2015.
 * Keeps common logic for keeping of result
 */
public class ResultCodeContainer {
    private int resultCode;

    public ResultCodeContainer(int resultCode) {
        this.resultCode = resultCode;
    }

    public int geResultCode() {
        return this.resultCode;
    }

    public String getErrorMessage(String textMessage) {
        if (textMessage == null)
            return "error code: " + this.resultCode;
        return String.format("Error code: \"%d\", message: \"%s\"", this.resultCode, textMessage);
    }
}
