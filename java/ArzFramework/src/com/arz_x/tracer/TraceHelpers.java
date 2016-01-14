package com.arz_x.tracer;

import com.arz_x.CheckedResultCodeException;
import com.arz_x.IExceptionResultCodeGetter;
import com.arz_x.ResultCodeException;
import com.arz_x.common.helpers.Contract;
import com.arz_x.common.helpers.StringHelper;

/**
 * Created by Rihter on 07.01.2016.
 * Basic helper tracer functions
 */
public class TraceHelpers {
    public static void traceMessage(ITracer tracer, TraceLevel traceLevel, String message) {
        if (tracer != null)
            tracer.traceMessage(traceLevel, StringHelper.getEmptyIfNull(message));
    }

    public static void traceException(ITracer tracer, TraceLevel traceLevel, Throwable exception) {
        Contract.requireNotNull(exception);

        if (tracer == null)
            return;

        String traceMessage = String.format("exp: '%s'", exception.getClass().getName());
        if (exception instanceof IExceptionResultCodeGetter) {
            traceMessage += String.format(" result code: '%d'", ((IExceptionResultCodeGetter) exception).getRawResultCode());
        }
        else {
            final String exceptionMessage = exception.getMessage();
            if (exceptionMessage != null)
                traceMessage += String.format(" message: '%s", exceptionMessage);
        }

        final StackTraceElement[] callStack = exception.getStackTrace();
        if (callStack != null) {
            for (StackTraceElement stackElement : callStack) {
                traceMessage += String.format("\n [%s:%s] %s:%s"
                        , stackElement.getFileName()
                        , stackElement.getLineNumber()
                        , stackElement.getClassName()
                        , stackElement.getMethodName());
            }
        }

        tracer.traceMessage(traceLevel, traceMessage);
    }
}
