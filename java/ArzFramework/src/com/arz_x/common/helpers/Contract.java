package com.arz_x.common.helpers;

import com.arz_x.CommonException;
import com.arz_x.CommonResultCode;

/**
 * Created by Rihter on 10.12.2015.
 * Helper for creating assertions, wrapping them into Arz platform exceptions
 */
public class Contract {
    public static void unusedVariable(Object... objects) { }

    public static void requireTrue(boolean expression) {
        if (!expression)
            throw new CommonException(CommonResultCode.AssertError);
    }

    public static void requireNotNull(Object... objects) {
        for (Object obj : objects) {
            if (obj == null)
                throw new CommonException(CommonResultCode.InvalidArgument);
        }
    }

    public static void requireStringNotNullOrEmpty(String... strings) {
        for (String str : strings) {
            if ((str == null) || (str.isEmpty()))
                throw new CommonException(CommonResultCode.InvalidArgument);
        }
    }
}
