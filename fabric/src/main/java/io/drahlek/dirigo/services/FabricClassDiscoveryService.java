package io.drahlek.dirigo.services;

import io.drahlek.dirigo.services.services.IClassDiscoveryService;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;

public class FabricClassDiscoveryService implements IClassDiscoveryService {
    @Override
    public Set<Class<?>> getTypesAnnotatedWith(String packageName, Class<? extends Annotation> annotationType) {
        return new Reflections(packageName).getTypesAnnotatedWith(annotationType);
    }

    @Override
    public Set<Method> getMethodsAnnotatedWith(String packageName, Class<? extends Annotation> annotationType) {
        return new Reflections(packageName, Scanners.MethodsAnnotated).getMethodsAnnotatedWith(annotationType);
    }

    @Override
    public Set<Field> getFieldsAnnotatedWith(String packageName, Class<? extends Annotation> annotationType) {
        return new Reflections(packageName, Scanners.FieldsAnnotated).getFieldsAnnotatedWith(annotationType);
    }
}
