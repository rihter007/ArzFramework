package com.arz_x.common.service_container;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by Rihter on 02.12.2015.
 * Specifies that creation of this logic is not required.
 * But if possible the logic would be created
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface OptionalLogic {
}
