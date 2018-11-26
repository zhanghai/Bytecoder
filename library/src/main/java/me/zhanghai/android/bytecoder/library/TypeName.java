/*
 * Copyright (c) 2018 Zhang Hai <Dreaming.in.Code.ZH@Gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.bytecoder.library;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Specify a actual type name to be used for the parameter or return value of the method.
 */
@Target({ ElementType.METHOD, ElementType.PARAMETER })
public @interface TypeName {

    /**
     * The type name, e.g. {@code "java.lang.Object"}.
     */
    String value();
}
