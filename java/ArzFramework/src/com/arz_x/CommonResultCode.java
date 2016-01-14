package com.arz_x;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Rihter on 18.01.2015.
 * Represents common operation result code
 */
public enum CommonResultCode
{
    UnknownReason(0xD7BA83EC),                   // a crc32 of 'CommonResultCode.UnknownReason'
    AssertError(0x69E30136),                     // a crc32 of 'CommonResultCode.AssertError'
    AccessDenied(0xD506CF53),                    // a crc32 of 'CommonResultCode.AccessDenied'
    NotFound(0x67E2835E),                        // a crc32 of 'CommonResultCode.NotFound'
    InvalidState(0x01111111),                    // a crc32 of 'CommonResultCode.InvalidState'
    InvalidArgument(0x713ECFD6),                 // a crc32 of 'CommonResultCode.InvalidArgument'
    InvalidExternalSource(0x9DA1E1CF),           // a crc32 of 'CommonResultCode.InvalidExternalSource'
    InvalidInternalState(0x266D38A0);            // a crc32 of 'CommonResultCode.InvalidInternalState'

    private int resultCode;

    private static final Map<Integer, CommonResultCode> TypesMap = new HashMap<Integer, CommonResultCode>()
    {
        {
            for (CommonResultCode code : CommonResultCode.values())
                put(code.getValue(), code);
        }
    };

    CommonResultCode(int value)
    {
        this.resultCode = value;
    }

    public int getValue()
    {
        return this.resultCode;
    }

    public static CommonResultCode getTypeByValue(int value)
    {
        return TypesMap.get(value);
    }
}
