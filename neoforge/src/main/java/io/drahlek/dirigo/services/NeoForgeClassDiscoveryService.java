package io.drahlek.dirigo.services;

import io.drahlek.dirigo.Constants;
import io.drahlek.dirigo.services.services.IClassDiscoveryService;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.ModFileScanData;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Set;

public class NeoForgeClassDiscoveryService implements IClassDiscoveryService {
    @Override
    public Set<Class<?>> getTypesAnnotatedWith(String packageName, Class<? extends Annotation> annotationType) {
        Set<Class<?>> classes = new LinkedHashSet<>();
        for (ModFileScanData scanData : ModList.get().getAllScanData()) {
            scanData.getAnnotatedBy(annotationType, ElementType.TYPE)
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
            scanData.getAnnotatedBy(annotationType, ElementType.METHOD)
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

    private void addAnnotatedMethods(
            Set<Method> methods,
            ModFileScanData.AnnotationData annotationData,
            Class<? extends Annotation> annotationType
    ) {
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

    private void addAnnotatedMethods(
            Set<Method> methods,
            Class<?> clazz,
            Class<? extends Annotation> annotationType
    ) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(annotationType)) {
                methods.add(method);
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
