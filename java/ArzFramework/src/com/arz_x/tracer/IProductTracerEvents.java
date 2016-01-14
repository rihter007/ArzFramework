package com.arz_x.tracer;

import java.io.File;

/**
 * Created by Rihter on 07.01.2016.
 * For handling sufficient ProductTracer events
 */
public interface IProductTracerEvents {
    /**
     * Invoked before trace files will be removed due to keeping too much disk space
     * @param removedFiles Files that will be removed
     */
    void onRemoveFiles(File[] removedFiles);
}
