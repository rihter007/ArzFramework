package com.arz_x;

/**
 * Created by Rihter on 18.01.2015.
 * Describes different error situations with network communication
 */
public enum NetworkResultCode
{
    UnknownReason(0xBDF5C0F5),              // a crc32 of 'NetworkResultCode.UnknownReason'
    NoConnection(0x0F01EC12),               // a crc32 of 'NetworkResultCode.NoConnection'
    AuthenticationError(0x0AAED3E1);        // a crc32 of 'NetworkResultCode.AuthenticationError'

    NetworkResultCode(int _value)
    {
        m_value = _value;
    }

    public int getValue()
    {
        return m_value;
    }

    private int m_value;
}
