package io.drahlek.dirigo.registrars;

import com.mojang.serialization.Codec;
import io.drahlek.dirigo.Constants;
import io.drahlek.dirigo.annotation.DataComponent;
import io.drahlek.dirigo.services.Services;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Set;

public class DataComponentRegistrar {
    public static void registerDataComponents(String modId, String packageName) {
        Set<Field> fields = Services.CLASS_DISCOVERY.getFieldsAnnotatedWith(packageName, DataComponent.class);

        for (Field field : fields) {
            registerField(modId, field);
        }
    }

    private static void registerField(
            String modId,
            Field field
    ) {
        DataComponent annotation = field.getAnnotation(DataComponent.class);
        registerTypedField(modId, field, annotation, castComponentType(annotation.type()));
        Constants.LOG.info("Registered data component {}:{}", modId, field.getName());
    }

    private static <T> void registerTypedField(
            String modId,
            Field field,
            DataComponent annotation,
            Class<T> type
    ) {
        validateField(field);

        ComponentCodecs<T> codecs = codecsFor(type);

        DataComponentType<T> registered = Services.DATA_COMPONENT_REGISTRAR.register(
                modId,
                annotation.id(),
                codecs.codec(),
                codecs.streamCodec(),
                annotation.persistent(),
                annotation.networkSynchronized()
        );

        inject(field, registered);
    }

    private static void validateField(Field field) {
        if (!Modifier.isStatic(field.getModifiers())) {
            throw new IllegalStateException("@DataComponent field must be static: " + field);
        }

        if (!DataComponentType.class.isAssignableFrom(field.getType())) {
            throw new IllegalStateException("@DataComponent field must be DataComponentType<?>: " + field);
        }
    }

    private static void inject(Field field, DataComponentType<?> value) {
        try {
            field.setAccessible(true);
            field.set(null, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to inject data component into field: " + field, e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> Class<T> castComponentType(Class<?> type) {
        return (Class<T>) type;
    }

    @SuppressWarnings("unchecked")
    private static <T> ComponentCodecs<T> codecsFor(Class<T> type) {
        if (type == Boolean.class || type == boolean.class) {
            return new ComponentCodecs<>(
                    (Codec<T>) Codec.BOOL,
                    (StreamCodec<? super RegistryFriendlyByteBuf, T>) ByteBufCodecs.BOOL
            );
        }

        if (type == Integer.class || type == int.class) {
            return new ComponentCodecs<>(
                    (Codec<T>) Codec.INT,
                    (StreamCodec<? super RegistryFriendlyByteBuf, T>) ByteBufCodecs.INT
            );
        }

        if (type == Long.class || type == long.class) {
            return new ComponentCodecs<>(
                    (Codec<T>) Codec.LONG,
                    (StreamCodec<? super RegistryFriendlyByteBuf, T>) ByteBufCodecs.VAR_LONG
            );
        }

        if (type == Float.class || type == float.class) {
            return new ComponentCodecs<>(
                    (Codec<T>) Codec.FLOAT,
                    (StreamCodec<? super RegistryFriendlyByteBuf, T>) ByteBufCodecs.FLOAT
            );
        }

        if (type == String.class) {
            return new ComponentCodecs<>(
                    (Codec<T>) Codec.STRING,
                    (StreamCodec<? super RegistryFriendlyByteBuf, T>) ByteBufCodecs.STRING_UTF8
            );
        }

        throw new IllegalArgumentException("Unsupported @DataComponent type: " + type.getName());
    }

    private record ComponentCodecs<T>(
            Codec<T> codec,
            StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec
    ) {
    }
}

