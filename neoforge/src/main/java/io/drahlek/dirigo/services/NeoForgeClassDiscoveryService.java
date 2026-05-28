package io.drahlek.dirigo.services;

import io.drahlek.dirigo.Constants;
import io.drahlek.dirigo.services.services.IClassDiscoveryService;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.ModFileScanData;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Stream;

public class NeoForgeClassDiscoveryService implements IClassDiscoveryService {
    @Override
    public Set<Class<?>> getTypesAnnotatedWith(String packageName, Class<? extends Annotation> annotationType) {
        Set<Class<?>> classes = new LinkedHashSet<>();
        for (ModFileScanData scanData : ModList.get().getAllScanData()) {
            annotations(scanData, annotationType, ElementType.TYPE)
                    .map(annotationData -> annotationData.clazz().getClassName())
                    .filter(className -> isInPackage(className, packageName))
                    .map(this::loadClass)
                    .filter(clazz -> clazz != null && clazz.isAnnotationPresent(annotationType))
                    .forEach(classes::add);
        }
        return classes;
    }

    @Override
    public Set<Method> getMethodsAnnotatedWith(String packageName, Class<? extends Annotation> annotationType) {
        Set<Method> methods = new LinkedHashSet<>();
        for (ModFileScanData scanData : ModList.get().getAllScanData()) {
            annotations(scanData, annotationType, ElementType.METHOD)
                    .filter(annotationData -> isInPackage(annotationData.clazz().getClassName(), packageName))
                    .forEach(annotationData -> addAnnotatedMethods(methods, annotationData, annotationType));
            scanData.getClasses().stream()
                    .map(classData -> classData.clazz().getClassName())
                    .filter(className -> isInPackage(className, packageName))
                    .map(this::loadClass)
                    .filter(clazz -> clazz != null)
                    .forEach(clazz -> addAnnotatedMethods(methods, clazz, annotationType));
        }
        return methods;
    }

    @Override
    public Set<Field> getFieldsAnnotatedWith(String packageName, Class<? extends Annotation> annotationType) {
        Set<Field> fields = new LinkedHashSet<>();
        for (ModFileScanData scanData : ModList.get().getAllScanData()) {
            annotations(scanData, annotationType, ElementType.FIELD)
                    .filter(annotationData -> isInPackage(annotationData.clazz().getClassName(), packageName))
                    .forEach(annotationData -> addAnnotatedFields(fields, annotationData, annotationType));
            scanData.getClasses().stream()
                    .map(classData -> classData.clazz().getClassName())
                    .filter(className -> isInPackage(className, packageName))
                    .map(this::loadClass)
                    .filter(clazz -> clazz != null)
                    .forEach(clazz -> addAnnotatedFields(fields, clazz, annotationType));
        }
        return fields;
    }

    private static Stream<ModFileScanData.AnnotationData> annotations(
            ModFileScanData scanData,
            Class<? extends Annotation> annotationType,
            ElementType targetType
    ) {
        return scanData.getAnnotations().stream()
                .filter(annotationData -> annotationData.targetType() == targetType)
                .filter(annotationData -> annotationData.annotationType().getClassName().equals(annotationType.getName()));
    }

    private void addAnnotatedMethods(Set<Method> methods, ModFileScanData.AnnotationData annotationData, Class<? extends Annotation> annotationType) {
        Class<?> clazz = loadClass(annotationData.clazz().getClassName());
        if (clazz == null) {
            return;
        }
        String memberName = annotationData.memberName();
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.getName().equals(memberName) && method.isAnnotationPresent(annotationType)) {
                methods.add(method);
            }
        }
    }

    private void addAnnotatedMethods(Set<Method> methods, Class<?> clazz, Class<? extends Annotation> annotationType) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(annotationType)) {
                methods.add(method);
            }
        }
    }

    private void addAnnotatedFields(Set<Field> fields, ModFileScanData.AnnotationData annotationData, Class<? extends Annotation> annotationType) {
        Class<?> clazz = loadClass(annotationData.clazz().getClassName());
        if (clazz == null) {
            return;
        }
        String memberName = annotationData.memberName();
        for (Field field : clazz.getDeclaredFields()) {
            if (field.getName().equals(memberName) && field.isAnnotationPresent(annotationType)) {
                fields.add(field);
            }
        }
    }

    private void addAnnotatedFields(Set<Field> fields, Class<?> clazz, Class<? extends Annotation> annotationType) {
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(annotationType)) {
                fields.add(field);
            }
        }
    }

    private Class<?> loadClass(String className) {
        if (className.contains(".mixin.")) {
            return null;
        }
        try {
            return Class.forName(className, false, Thread.currentThread().getContextClassLoader());
        } catch (Throwable e) {
            if (e instanceof VirtualMachineError virtualMachineError) {
                throw virtualMachineError;
            }
            Constants.LOG.debug("Skipping discovered class {} because it could not be loaded", className, e);
            return null;
        }
    }

    private static boolean isInPackage(String className, String packageName) {
        return className.equals(packageName) || className.startsWith(packageName + ".");
    }
}