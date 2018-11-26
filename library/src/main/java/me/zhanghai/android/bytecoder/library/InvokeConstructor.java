/*
 * Copyright (c) 2018 Zhang Hai <Dreaming.in.Code.ZH@Gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.bytecoder.library;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Transform a method to invoke a constructor and return the new instance of the class.
 */
@Target({ ElementType.METHOD })
public @interface InvokeConstructor {

    /**
     * The class constant (e.g. {@code Object.class}) of the owner of the method.
     * <p>
     * Either this or {@link #className()} must be specified.
     */
    Class<?> classConstant() default void.class;

    /**
     * The class name (e.g. {@code "java.lang.Object"}) of the owner of the method.
     * <p>
     * Either this or {@link #classConstant()} must be specified.
     */
    String className() default "";
}
