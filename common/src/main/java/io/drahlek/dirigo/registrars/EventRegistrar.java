package io.drahlek.dirigo.registrars;

import io.drahlek.dirigo.annotation.EventSubscriber;
import io.drahlek.dirigo.event.EventBus;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import java.lang.reflect.Method;
import java.util.Set;

public class EventRegistrar {
    public static void registerEvents(String packageName) {
        EventBus eventBus = EventBus.INSTANCE;

        Reflections reflections = new Reflections(packageName, Scanners.MethodsAnnotated);
        Set<Method> annotatedMethods = reflections.getMethodsAnnotatedWith(EventSubscriber.class);

        for (Method method : annotatedMethods) {
            eventBus.register(method);
        }
    }
}
