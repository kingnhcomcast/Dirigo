package io.drahlek.dirigo.event;

import io.drahlek.dirigo.Constants;
import io.drahlek.dirigo.schedule.EventCallback;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class EventBus {
    public static final EventBus INSTANCE = new EventBus();

    private final Map<Class<?>, List<EventCallback>> eventSubscribers = new ConcurrentHashMap<>();
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    public void register(Method method) {
        // Ensure the method is static since we aren't passing an object instance
        if (!Modifier.isStatic(method.getModifiers())) {
            Constants.LOG.error("Method {} must be static to be registered via Method discovery.", method.getName());
            return;
        }

        try {
            Class<?>[] params = method.getParameterTypes();
            if (params.length != 1) {
                Constants.LOG.warn("Method {} must have exactly one parameter of type EventCallback.", method.getName());
                return;
            }

            Class<?> eventType = params[0];
            method.setAccessible(true);
            MethodHandle rawHandle = LOOKUP.unreflect(method);
            MethodHandle finalHandle = rawHandle.asType(MethodType.methodType(void.class, Object.class));

            eventSubscribers.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
                    .add(new EventCallback(finalHandle));

            Constants.LOG.info("Registered {}.{}() for event {}.", method.getDeclaringClass().getName(), method.getName(), eventType.getSimpleName());
        } catch (IllegalAccessException e) {
            Constants.LOG.error("Error registering method {} {}", method.getName(), e);
        }
    }

    public void publish(Object event) {
        List<EventCallback> callbacks = eventSubscribers.get(event.getClass());
        if (callbacks != null) {
            for (EventCallback callback : callbacks) {
                callback.invoke(event);
            }
        }
    }
}
