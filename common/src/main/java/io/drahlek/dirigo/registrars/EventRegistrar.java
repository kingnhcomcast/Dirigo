package io.drahlek.dirigo.registrars;

import io.drahlek.dirigo.annotation.EventSubscriber;
import io.drahlek.dirigo.event.EventBus;
import io.drahlek.dirigo.services.Services;

import java.lang.reflect.Method;
import java.util.Set;

public class EventRegistrar {
    public static void registerEvents(String packageName) {
        EventBus eventBus = EventBus.INSTANCE;

        Set<Method> annotatedMethods = Services.CLASS_DISCOVERY.getMethodsAnnotatedWith(packageName, EventSubscriber.class);

        for (Method method : annotatedMethods) {
            eventBus.register(method);
        }
    }
}
