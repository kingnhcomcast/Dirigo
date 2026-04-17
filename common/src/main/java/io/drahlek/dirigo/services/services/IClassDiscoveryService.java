package io.drahlek.dirigo.services.services;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Set;

public interface IClassDiscoveryService {
    Set<Class<?>> getTypesAnnotatedWith(String packageName, Class<? extends Annotation> annotationType);

    Set<Method> getMethodsAnnotatedWith(String packageName, Class<? extends Annotation> annotationType);
}
