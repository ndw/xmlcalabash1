package com.xmlcalabash.core;

import org.atteo.classindex.IndexAnnotated;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by ndw on 2/15/15.
 */

@Retention(RetentionPolicy.RUNTIME)
@IndexAnnotated
public @interface XMLCalabash {
    String name() default "unnamed";
    String type();
}
