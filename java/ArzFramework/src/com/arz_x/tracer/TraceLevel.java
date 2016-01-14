package com.arz_x.tracer;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Rihter on 22.04.2015.
 * Defines trace level
 */
public enum TraceLevel
{
    Verbose(0),
    Debug(100),
    Info(200),
    Warning(300),
    Important(400),
    Error(500),
    Always(1000),
    Assert(2000);

    private int value;

    private static Map<Integer, TraceLevel> ValuesMap = new HashMap<Integer, TraceLevel>()
    {
        {
            final TraceLevel[] allTraceValues = TraceLevel.values();
            for (TraceLevel traceLevel : allTraceValues)
                put(traceLevel.getValue(), traceLevel);
        }
    };

    TraceLevel(int _value)
    {
        this.value = _value;
    }

    public static TraceLevel getTypeByValue(int value)
    {
        return ValuesMap.get(value);
    }

    public int getValue()
    {
        return this.value;
    }
}
