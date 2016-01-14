package com.arz_x;

/**
 * Created by Rihter on 18.01.2015.
 * Represents the exception that can be thrown by network library
 */
public class CheckedNetworkException extends CheckedResultCodeException
{
    public CheckedNetworkException(NetworkResultCode resultCode)
    {
        super(resultCode.getValue());
        m_value = resultCode;
    }

    public NetworkResultCode getResultCode()
    {
        return m_value;
    }

    @Override
    public String getMessage()
    {
        return "error code: " + m_value.name();
    }

    private NetworkResultCode m_value;
}
