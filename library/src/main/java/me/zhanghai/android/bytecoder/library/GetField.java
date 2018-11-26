/*
 * Copyright (c) 2018 Zhang Hai <Dreaming.in.Code.ZH@Gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.bytecoder.library;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Transform a method to get the value of a field.
 * <p>
 * The method must take one parameter, and it must be an instance of the owner of the field. The
 * return type of the method must be the same as the field.
 */
@Target({ ElementType.METHOD })
public @interface GetField {

    /**
     * The class constant (e.g. {@code Object.class}) of the owner of the field.
     * <p>
     * Either this or {@link #className()} must be specified.
     */
    Class<?> classConstant() default void.class;

    /**
     * The class name (e.g. {@code "java.lang.Object"}) of the owner of the field.
     * <p>
     * Either this or {@link #classConstant()} must be specified.
     */
    String className() default "";

    /**
     * The name of the field.
     */
    String fieldName();
}
