package com.arz_x.common.service_container;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by Rihter on 22.11.2015.
 * This annotation marks which constructor to use to create object
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface InstantiateConstructor {
}
