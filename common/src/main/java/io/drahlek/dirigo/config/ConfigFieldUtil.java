package io.drahlek.dirigo.config;

import io.drahlek.dirigo.Constants;
import io.drahlek.dirigo.annotation.ConfigSetting;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

public final class ConfigFieldUtil {
    private ConfigFieldUtil() {
    }

    public static Stream<Field> configSettingFields(Config<?> config) {
        return Arrays.stream(config.getDataClass().getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(ConfigSetting.class))
                .sorted(Comparator.comparing(ConfigFieldUtil::settingName));
    }

    public static Field findSettingField(Config<?> config, String rawName) {
        String name = rawName == null ? "" : rawName.trim();
        return configSettingFields(config)
                .filter(field -> settingName(field).equals(name) || settingName(field).equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    public static Object readConfigValue(Config<?> config, Field field) {
        try {
            field.setAccessible(true);
            return field.get(config.get());
        } catch (Exception e) {
            throw new RuntimeException("Failed to read " + field.getName(), e);
        }
    }

    public static void setConfigValue(Config<?> config, Field field, Object value) {
        try {
            Object data = config.get();
            Method setter = findSetter(config.getDataClass(), field);
            if (setter != null) {
                setter.invoke(data, value);
                return;
            }

            field.setAccessible(true);
            field.set(data, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set " + field.getName(), e);
        }
    }

    public static Object defaultValue(Config<?> config, Field field) {
        ConfigSetting setting = field.getAnnotation(ConfigSetting.class);
        if (setting != null && !setting.defaultValue().isBlank()) {
            try {
                return parseFieldValue(field, setting.defaultValue());
            } catch (IllegalArgumentException e) {
                Constants.LOG.warn(
                        "Invalid @ConfigSetting default '{}' for '{}'; using class default",
                        setting.defaultValue(),
                        settingName(field)
                );
            }
        }

        return classDefaultValue(config, field);
    }

    public static String validateRange(Field field, Object value) {
        ConfigSetting setting = field.getAnnotation(ConfigSetting.class);
        if (setting == null || !(value instanceof Number number)) {
            return null;
        }

        double numericValue = number.doubleValue();
        if (numericValue < setting.min() || numericValue > setting.max()) {
            return String.format(
                    "Value for '%s' must be between %s and %s (got %s)",
                    settingName(field),
                    setting.min(),
                    setting.max(),
                    numericValue
            );
        }

        return null;
    }

    public static Object parseFieldValue(Field field, String raw) {
        String value = raw == null ? "" : raw.trim();
        Class<?> type = field.getType();

        try {
            if (type == int.class || type == Integer.class) {
                return Integer.parseInt(value);
            }

            if (type == long.class || type == Long.class) {
                return Long.parseLong(value);
            }

            if (type == float.class || type == Float.class) {
                return Float.parseFloat(stripFloatingSuffix(value));
            }

            if (type == double.class || type == Double.class) {
                return Double.parseDouble(stripFloatingSuffix(value));
            }

            if (type == boolean.class || type == Boolean.class) {
                if ("true".equalsIgnoreCase(value)) {
                    return true;
                }
                if ("false".equalsIgnoreCase(value)) {
                    return false;
                }
                throw new IllegalArgumentException("Value for '" + settingName(field) + "' must be true or false");
            }

            if (type == short.class || type == Short.class) {
                return Short.parseShort(value);
            }

            if (type == byte.class || type == Byte.class) {
                return Byte.parseByte(value);
            }

            return parseStringLikeFieldValue(field, raw);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Value for '" + settingName(field) + "' must be a valid " + type.getSimpleName()
            );
        }
    }

    public static Object parseStringLikeFieldValue(Field field, String raw) {
        String value = raw == null ? "" : raw;
        Class<?> type = field.getType();

        if (Set.class.isAssignableFrom(type)) {
            return new LinkedHashSet<>(splitDelimitedValues(value));
        }

        if (List.class.isAssignableFrom(type)) {
            return splitDelimitedValues(value);
        }

        if (type.isEnum()) {
            return parseEnum(type, value);
        }

        if (type == char.class || type == Character.class) {
            String characterValue = value.trim();
            if (characterValue.length() != 1) {
                throw new IllegalArgumentException("Value for '" + settingName(field) + "' must be one character");
            }
            return characterValue.charAt(0);
        }

        return value;
    }

    public static boolean hasFiniteMin(Field field) {
        ConfigSetting setting = field.getAnnotation(ConfigSetting.class);
        return setting != null && Double.isFinite(setting.min());
    }

    public static boolean hasFiniteMax(Field field) {
        ConfigSetting setting = field.getAnnotation(ConfigSetting.class);
        return setting != null && Double.isFinite(setting.max());
    }

    public static String settingName(Field field) {
        ConfigSetting setting = field.getAnnotation(ConfigSetting.class);
        if (setting == null || setting.value().isBlank()) {
            return field.getName();
        }

        return setting.value();
    }

    public static String displayName(Field field) {
        String name = settingName(field)
                .replace('-', ' ')
                .replace('_', ' ');
        StringBuilder spaced = new StringBuilder();

        for (int index = 0; index < name.length(); index++) {
            char current = name.charAt(index);
            if (index > 0
                    && Character.isUpperCase(current)
                    && Character.isLetterOrDigit(name.charAt(index - 1))) {
                spaced.append(' ');
            }
            spaced.append(current);
        }

        String[] words = spaced.toString().trim().split("\\s+");
        StringBuilder display = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (!display.isEmpty()) {
                display.append(' ');
            }
            display.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                display.append(word.substring(1));
            }
        }
        return display.isEmpty() ? field.getName() : display.toString();
    }

    private static Object classDefaultValue(Config<?> config, Field field) {
        try {
            Constructor<?> constructor = config.getDataClass().getDeclaredConstructor();
            constructor.setAccessible(true);
            Object defaults = constructor.newInstance();

            field.setAccessible(true);
            return field.get(defaults);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read default value for " + field.getName(), e);
        }
    }

    private static String stripFloatingSuffix(String value) {
        if (value.endsWith("f") || value.endsWith("F") || value.endsWith("d") || value.endsWith("D")) {
            return value.substring(0, value.length() - 1);
        }

        return value;
    }

    private static List<String> splitDelimitedValues(String raw) {
        String normalized = raw.trim();
        if (normalized.startsWith("[") && normalized.endsWith("]") && normalized.length() >= 2) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }

        List<String> values = new ArrayList<>();
        if (normalized.isEmpty()) {
            return values;
        }

        for (String part : normalized.split(",")) {
            String value = part.trim();
            if (!value.isEmpty()) {
                values.add(value);
            }
        }
        return values;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object parseEnum(Class<?> type, String raw) {
        String value = raw.trim();
        Class<? extends Enum> enumType = type.asSubclass(Enum.class);

        for (Object constant : enumType.getEnumConstants()) {
            Enum<?> enumConstant = (Enum<?>) constant;
            if (enumConstant.name().equals(value) || enumConstant.name().equalsIgnoreCase(value)) {
                return enumConstant;
            }
        }

        throw new IllegalArgumentException(
                "Value must be one of " + Arrays.toString(enumType.getEnumConstants()).toLowerCase(Locale.ROOT)
        );
    }

    private static Method findSetter(Class<?> owner, Field field) {
        String fieldName = field.getName();
        String setterName = "set" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);

        Class<?> type = field.getType();
        Method setter = findSetter(owner, setterName, type);
        if (setter != null) {
            return setter;
        }

        if (type.isPrimitive()) {
            Class<?> boxed = box(type);
            return boxed == null ? null : findSetter(owner, setterName, boxed);
        }

        Class<?> unboxed = unbox(type);
        return unboxed == null ? null : findSetter(owner, setterName, unboxed);
    }

    private static Method findSetter(Class<?> owner, String setterName, Class<?> parameterType) {
        try {
            return owner.getMethod(setterName, parameterType);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private static Class<?> box(Class<?> primitive) {
        if (primitive == int.class) return Integer.class;
        if (primitive == double.class) return Double.class;
        if (primitive == boolean.class) return Boolean.class;
        if (primitive == long.class) return Long.class;
        if (primitive == float.class) return Float.class;
        if (primitive == short.class) return Short.class;
        if (primitive == byte.class) return Byte.class;
        if (primitive == char.class) return Character.class;
        return null;
    }

    private static Class<?> unbox(Class<?> boxed) {
        if (boxed == Integer.class) return int.class;
        if (boxed == Double.class) return double.class;
        if (boxed == Boolean.class) return boolean.class;
        if (boxed == Long.class) return long.class;
        if (boxed == Float.class) return float.class;
        if (boxed == Short.class) return short.class;
        if (boxed == Byte.class) return byte.class;
        if (boxed == Character.class) return char.class;
        return null;
    }
}
