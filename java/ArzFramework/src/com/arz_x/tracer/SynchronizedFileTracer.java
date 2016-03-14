package com.arz_x.tracer;

import com.arz_x.CommonException;
import com.arz_x.CommonResultCode;
import com.arz_x.common.helpers.Contract;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Calendar;

public class SynchronizedFileTracer implements IFileTracer, Closeable {

    private String pathToFile;
    private Charset charset;
    private OutputStream traceFile;

    private volatile long currentFileSize;
    private volatile int minTraceLevel;

    public SynchronizedFileTracer(String pathToFile
            , TraceLevel minTraceLevel
            , boolean appendIfExist
            , Charset charset) {
        Contract.requireNotNull(pathToFile, minTraceLevel, charset);

        openFile(pathToFile, appendIfExist);
        this.charset = (this.charset != null) ? charset : Charset.forName("UTF-8");
        this.minTraceLevel = minTraceLevel.getValue();
    }

    public SynchronizedFileTracer(String pathToFile
            , TraceLevel minTraceLevel
            , boolean appendIfExist) {
        this(pathToFile, minTraceLevel, appendIfExist, Charset.forName("UTF-8"));
    }

    @Override
    public void openNewFile(String filepath) {
        openFile(filepath, false);
    }

    @Override
    public void openExistingFile(String filepath) {
        openFile(filepath, true);
    }

    @Override
    public String getCurrentFilePath() {
        return this.pathToFile;
    }

    @Override
    public long getFileSize() {
        return this.currentFileSize;
    }

    @Override
    public void traceMessage(TraceLevel traceLevel, String message) {
        if (traceLevel.getValue() >= this.minTraceLevel)
            internalTraceMessage(message);
    }

    @Override
    public synchronized void close() {
        if (this.traceFile == null)
            return;
        try {
            this.traceFile.flush();
            this.traceFile.close();
            this.traceFile = null;
        } catch (IOException exp) {
            /* it is impossible */
            throw new CommonException(CommonResultCode.UnExpected);
        }
    }

    private synchronized void openFile(String pathToFile, boolean appendIfExists) {
        try {
            close();
            this.pathToFile = pathToFile;

            this.traceFile = new FileOutputStream(this.pathToFile, appendIfExists);
            this.currentFileSize = new File(this.pathToFile).length();
        } catch (FileNotFoundException exp) {
            throw new CommonException(CommonResultCode.NotFound);
        }
    }

    private synchronized void internalTraceMessage(String message) {
        // check if the file is closed already
        if (this.traceFile == null)
            return;

        final Calendar currentDateTime = Calendar.getInstance();
        final int milliseconds = currentDateTime.get(Calendar.MILLISECOND);
        final int seconds = currentDateTime.get(Calendar.SECOND);
        final int minutes = currentDateTime.get(Calendar.MINUTE);
        final int hours = currentDateTime.get(Calendar.HOUR_OF_DAY);

        final long currentThreadId = Thread.currentThread().getId();

        final String traceMessage = String.format("%d:%d:%d:%d\t%d\t%s\n"
                , hours, minutes, seconds, milliseconds, currentThreadId, message);
        final byte[] messageBytes = traceMessage.getBytes(this.charset);

        try {
            this.traceFile.write(messageBytes);
            this.traceFile.flush();
            this.currentFileSize += messageBytes.length;
        } catch (IOException exp) {
            throw new CommonException(CommonResultCode.UnExpected);
        }
    }
}
