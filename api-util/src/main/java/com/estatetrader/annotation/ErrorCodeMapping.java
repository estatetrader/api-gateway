package com.estatetrader.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by nick on 22/08/2017.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ErrorCodeMapping {
    int[] mapping();

    int[] mapping1() default {};

    int[] mapping2() default {};

    int[] mapping3() default {};

    int[] mapping4() default {};

    int[] mapping5() default {};

    int[] mapping6() default {};

    int[] mapping7() default {};

    int[] mapping8() default {};

    int[] mapping9() default {};

    int[] mapping10() default {};

    int[] mapping11() default {};

    int[] mapping12() default {};

    int[] mapping13() default {};

    int[] mapping14() default {};

    int[] mapping15() default {};

    int[] mapping16() default {};

    int[] mapping17() default {};

    int[] mapping18() default {};

    int[] mapping19() default {};
}
