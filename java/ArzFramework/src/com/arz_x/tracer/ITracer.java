package com.arz_x.tracer;

/**
 * Created by Rihter on 22.04.2015.
 * Main interface for tracing
 */
public interface ITracer {
    void traceMessage(TraceLevel traceLevel, String message);
}
