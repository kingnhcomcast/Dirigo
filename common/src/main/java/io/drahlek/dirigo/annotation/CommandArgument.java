package io.drahlek.dirigo.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface CommandArgument {
    String name();

    CommandArgumentType type() default CommandArgumentType.WORD;

    boolean optional() default false;
}
