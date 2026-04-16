package io.drahlek.dirigo.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Command {
    String value();

    CommandArgument[] arguments() default {};

    boolean requiresOp() default true;

    boolean requiresConfig() default false;
}
