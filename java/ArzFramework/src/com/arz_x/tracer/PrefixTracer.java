package com.arz_x.tracer;

import com.arz_x.common.helpers.Contract;

/**
 * Created by Rihter on 07.01.2016.
 * Adds "[prefix]\t" to each trace message
 */
public class PrefixTracer implements ITracer {

    public static ITracer createPrefixTracer(ITracer wrappedTracer, String componentPrefix) {
        return new PrefixTracer(wrappedTracer, componentPrefix);
    }

    private ITracer wrappedTracer;
    private String componentPrefix;

    private PrefixTracer(ITracer tracer, String componentPrefix) {
        Contract.requireNotNull(tracer);
        Contract.requireStringNotNullOrEmpty(componentPrefix);

        this.wrappedTracer = tracer;
        this.componentPrefix = componentPrefix;
    }

    public void traceMessage(TraceLevel traceLevel, String message) {
        this.wrappedTracer.traceMessage(traceLevel, String.format("[%s]\t%s", this.componentPrefix, message));
    }
}
