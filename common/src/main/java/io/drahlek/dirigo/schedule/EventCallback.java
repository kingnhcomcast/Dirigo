package io.drahlek.dirigo.schedule;

import io.drahlek.dirigo.Constants;

import java.lang.invoke.MethodHandle;

public class EventCallback {
    private final MethodHandle handle;

    public EventCallback(MethodHandle handle) {
        this.handle = handle;
    }

    public void invoke(Object event) {
        try {
            // invokeExact is fastest, but requires the exact type signature.
            // invoke is more flexible and still significantly faster than Reflection.
            handle.invoke(event);
        } catch (Throwable t) {
            Constants.LOG.error("Critical error in event subscriber %s", t);
        }
    }
}