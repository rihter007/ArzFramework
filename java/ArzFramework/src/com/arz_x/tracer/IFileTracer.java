package com.arz_x.tracer;

/**
 * Created by Rihter on 07.01.2016.
 * Additional interface for file tracers
 */
public interface IFileTracer extends ITracer {
    void openNewFile(String filepath);
    void openExistingFile(String filepath);
    String getCurrentFilePath();
    long getFileSize();
}
