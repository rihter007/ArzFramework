package com.arz_x.common.helpers;

/**
 * Created by Rihter on 18.06.2015.
 * Contains different helpers for string processing
 */
public class StringHelper {
    public static String getEmptyIfNull(String str) {
        return str != null ? str : null;
    }
}
