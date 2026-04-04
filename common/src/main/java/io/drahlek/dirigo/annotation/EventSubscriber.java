package io.drahlek.dirigo.annotation;

import io.drahlek.dirigo.event.EventBase;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface EventSubscriber {
    Class<? extends EventBase> value();
}