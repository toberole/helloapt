package com.zhouwei;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by zhouwei on 2017/12/23.
 */

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface DB {
    String value() default "db";

    String dbName() default "db";

    int dbVersion() default 1;
}
