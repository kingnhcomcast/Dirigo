package io.drahlek.dirigo.client.config;

import io.drahlek.dirigo.annotation.ConfigSetting;
import io.drahlek.dirigo.client.mixin.LocalPlayerAccessor;
import io.drahlek.dirigo.config.Config;
import io.drahlek.dirigo.config.ConfigFieldUtil;
import io.drahlek.dirigo.permissions.PermissionHelper;
import io.drahlek.dirigo.services.Services;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class GeneratedConfigScreen {
    private static final long MAX_SLIDER_STEPS = 1000L;

    private GeneratedConfigScreen() {
    }

    public static Screen create(Screen parent, Config<?> config) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.literal(config.getModId() + " Config"));
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        ConfigCategory general = builder.getOrCreateCategory(Component.literal("General"));
        boolean canEdit = canModifyConfig();

        if (!canEdit) {
            general.addEntry(entryBuilder.startTextDescription(
                    Component.literal("Server config is read-only for this player.")
            ).build());
        }

        ConfigFieldUtil.configSettingFields(config)
                .map(field -> buildEntry(entryBuilder, config, field))
                .map(entry -> setEditable(entry, canEdit))
                .forEach(general::addEntry);

        builder.setSavingRunnable(() -> {
            if (canEdit) {
                Services.NETWORK_SERVICE.sendToServer(config.toPayload());
            }
        });
        return builder.build();
    }

    private static boolean canModifyConfig() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player != null
                && ((LocalPlayerAccessor) minecraft.player)
                        .dirigo$getPermissions()
                        .hasPermission(PermissionHelper.CONFIG_MODIFY_PERMISSION);
    }

    private static AbstractConfigListEntry<?> setEditable(AbstractConfigListEntry<?> entry, boolean editable) {
        entry.setEditable(editable);
        return entry;
    }

    private static AbstractConfigListEntry<?> buildEntry(ConfigEntryBuilder entryBuilder, Config<?> config, Field field) {
        Class<?> type = field.getType();
        Component label = Component.literal(ConfigFieldUtil.displayName(field));
        Object value = valueOrDefault(ConfigFieldUtil.readConfigValue(config, field), ConfigFieldUtil.defaultValue(config, field));
        Object defaultValue = ConfigFieldUtil.defaultValue(config, field);

        if (type == boolean.class || type == Boolean.class) {
            return entryBuilder.startBooleanToggle(label, booleanValue(value))
                    .setDefaultValue(booleanValue(defaultValue))
                    .setSaveConsumer(newValue -> ConfigFieldUtil.setConfigValue(config, field, newValue))
                    .build();
        }

        if (type == int.class || type == Integer.class) {
            return buildIntEntry(entryBuilder, config, field, label, intValue(value), intValue(defaultValue));
        }

        if (type == long.class || type == Long.class) {
            return buildLongEntry(entryBuilder, config, field, label, longValue(value), longValue(defaultValue));
        }

        if (type == short.class || type == Short.class) {
            return buildShortEntry(entryBuilder, config, field, label, intValue(value), intValue(defaultValue));
        }

        if (type == byte.class || type == Byte.class) {
            return buildByteEntry(entryBuilder, config, field, label, intValue(value), intValue(defaultValue));
        }

        if (type == float.class || type == Float.class) {
            return entryBuilder.startFloatField(label, floatValue(value))
                    .setDefaultValue(floatValue(defaultValue))
                    .setMin(floatMin(field))
                    .setMax(floatMax(field))
                    .setErrorSupplier(newValue -> rangeError(field, newValue))
                    .setSaveConsumer(newValue -> ConfigFieldUtil.setConfigValue(config, field, newValue))
                    .build();
        }

        if (type == double.class || type == Double.class) {
            return entryBuilder.startDoubleField(label, doubleValue(value))
                    .setDefaultValue(doubleValue(defaultValue))
                    .setMin(doubleMin(field))
                    .setMax(doubleMax(field))
                    .setErrorSupplier(newValue -> rangeError(field, newValue))
                    .setSaveConsumer(newValue -> ConfigFieldUtil.setConfigValue(config, field, newValue))
                    .build();
        }

        if (type.isEnum()) {
            return buildEnumEntry(entryBuilder, config, field, label, value, defaultValue);
        }

        if (type == char.class || type == Character.class) {
            return buildCharEntry(entryBuilder, config, field, label, value, defaultValue);
        }

        if (List.class.isAssignableFrom(type) || Set.class.isAssignableFrom(type)) {
            return buildStringListEntry(entryBuilder, config, field, label, value, defaultValue);
        }

        if (type == String.class) {
            return entryBuilder.startStrField(label, stringValue(value))
                    .setDefaultValue(stringValue(defaultValue))
                    .setSaveConsumer(newValue -> ConfigFieldUtil.setConfigValue(config, field, newValue))
                    .build();
        }

        return entryBuilder.startTextDescription(Component.literal(
                ConfigFieldUtil.settingName(field) + " has unsupported type " + type.getSimpleName()
        )).build();
    }

    private static AbstractConfigListEntry<?> buildIntEntry(
            ConfigEntryBuilder entryBuilder,
            Config<?> config,
            Field field,
            Component label,
            int value,
            int defaultValue
    ) {
        int min = intMin(field, Integer.MIN_VALUE);
        int max = intMax(field, Integer.MAX_VALUE);
        if (useSlider(min, max)) {
            return entryBuilder.startIntSlider(label, clamp(value, min, max), min, max)
                    .setDefaultValue(clamp(defaultValue, min, max))
                    .setSaveConsumer(newValue -> ConfigFieldUtil.setConfigValue(config, field, newValue))
                    .build();
        }

        return entryBuilder.startIntField(label, value)
                .setDefaultValue(defaultValue)
                .setMin(min)
                .setMax(max)
                .setErrorSupplier(newValue -> rangeError(field, newValue))
                .setSaveConsumer(newValue -> ConfigFieldUtil.setConfigValue(config, field, newValue))
                .build();
    }

    private static AbstractConfigListEntry<?> buildLongEntry(
            ConfigEntryBuilder entryBuilder,
            Config<?> config,
            Field field,
            Component label,
            long value,
            long defaultValue
    ) {
        long min = longMin(field, Long.MIN_VALUE);
        long max = longMax(field, Long.MAX_VALUE);
        if (useSlider(min, max)) {
            return entryBuilder.startLongSlider(label, clamp(value, min, max), min, max)
                    .setDefaultValue(clamp(defaultValue, min, max))
                    .setSaveConsumer(newValue -> ConfigFieldUtil.setConfigValue(config, field, newValue))
                    .build();
        }

        return entryBuilder.startLongField(label, value)
                .setDefaultValue(defaultValue)
                .setMin(min)
                .setMax(max)
                .setErrorSupplier(newValue -> rangeError(field, newValue))
                .setSaveConsumer(newValue -> ConfigFieldUtil.setConfigValue(config, field, newValue))
                .build();
    }

    private static AbstractConfigListEntry<?> buildShortEntry(
            ConfigEntryBuilder entryBuilder,
            Config<?> config,
            Field field,
            Component label,
            int value,
            int defaultValue
    ) {
        int min = intMin(field, Short.MIN_VALUE);
        int max = intMax(field, Short.MAX_VALUE);
        return entryBuilder.startIntField(label, clamp(value, min, max))
                .setDefaultValue(clamp(defaultValue, min, max))
                .setMin(min)
                .setMax(max)
                .setErrorSupplier(newValue -> rangeError(field, newValue.shortValue()))
                .setSaveConsumer(newValue -> ConfigFieldUtil.setConfigValue(config, field, newValue.shortValue()))
                .build();
    }

    private static AbstractConfigListEntry<?> buildByteEntry(
            ConfigEntryBuilder entryBuilder,
            Config<?> config,
            Field field,
            Component label,
            int value,
            int defaultValue
    ) {
        int min = intMin(field, Byte.MIN_VALUE);
        int max = intMax(field, Byte.MAX_VALUE);
        return entryBuilder.startIntField(label, clamp(value, min, max))
                .setDefaultValue(clamp(defaultValue, min, max))
                .setMin(min)
                .setMax(max)
                .setErrorSupplier(newValue -> rangeError(field, newValue.byteValue()))
                .setSaveConsumer(newValue -> ConfigFieldUtil.setConfigValue(config, field, newValue.byteValue()))
                .build();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static AbstractConfigListEntry<?> buildEnumEntry(
            ConfigEntryBuilder entryBuilder,
            Config<?> config,
            Field field,
            Component label,
            Object value,
            Object defaultValue
    ) {
        Class enumType = field.getType().asSubclass(Enum.class);
        Enum current = enumValue(enumType, value);
        Enum defaultEnumValue = enumValue(enumType, defaultValue);
        return entryBuilder.startEnumSelector(label, enumType, current)
                .setDefaultValue(defaultEnumValue)
                .setSaveConsumer(newValue -> ConfigFieldUtil.setConfigValue(config, field, newValue))
                .build();
    }

    private static AbstractConfigListEntry<?> buildCharEntry(
            ConfigEntryBuilder entryBuilder,
            Config<?> config,
            Field field,
            Component label,
            Object value,
            Object defaultValue
    ) {
        return entryBuilder.startStrField(label, stringValue(value))
                .setDefaultValue(stringValue(defaultValue))
                .setErrorSupplier(newValue -> newValue.length() == 1
                        ? Optional.empty()
                        : Optional.of(Component.literal("Value must be one character")))
                .setSaveConsumer(newValue -> {
                    if (newValue.length() == 1) {
                        ConfigFieldUtil.setConfigValue(config, field, newValue.charAt(0));
                    }
                })
                .build();
    }

    private static AbstractConfigListEntry<?> buildStringListEntry(
            ConfigEntryBuilder entryBuilder,
            Config<?> config,
            Field field,
            Component label,
            Object value,
            Object defaultValue
    ) {
        return entryBuilder.startStrList(label, stringListValue(value))
                .setDefaultValue(stringListValue(defaultValue))
                .setSaveConsumer(newValue -> {
                    if (Set.class.isAssignableFrom(field.getType())) {
                        ConfigFieldUtil.setConfigValue(config, field, new LinkedHashSet<>(newValue));
                        return;
                    }

                    ConfigFieldUtil.setConfigValue(config, field, new ArrayList<>(newValue));
                })
                .build();
    }

    private static Object valueOrDefault(Object value, Object defaultValue) {
        return value == null ? defaultValue : value;
    }

    private static Optional<Component> rangeError(Field field, Number value) {
        String error = ConfigFieldUtil.validateRange(field, value);
        return error == null ? Optional.empty() : Optional.of(Component.literal(error));
    }

    private static boolean useSlider(long min, long max) {
        long range = max - min;
        return max >= min && range >= 0L && range <= MAX_SLIDER_STEPS;
    }

    private static int intMin(Field field, int hardMin) {
        ConfigSetting setting = field.getAnnotation(ConfigSetting.class);
        if (setting == null || !Double.isFinite(setting.min())) {
            return hardMin;
        }

        return Math.max(hardMin, (int) Math.ceil(setting.min()));
    }

    private static int intMax(Field field, int hardMax) {
        ConfigSetting setting = field.getAnnotation(ConfigSetting.class);
        if (setting == null || !Double.isFinite(setting.max())) {
            return hardMax;
        }

        return Math.min(hardMax, (int) Math.floor(setting.max()));
    }

    private static long longMin(Field field, long hardMin) {
        ConfigSetting setting = field.getAnnotation(ConfigSetting.class);
        if (setting == null || !Double.isFinite(setting.min())) {
            return hardMin;
        }

        return Math.max(hardMin, (long) Math.ceil(setting.min()));
    }

    private static long longMax(Field field, long hardMax) {
        ConfigSetting setting = field.getAnnotation(ConfigSetting.class);
        if (setting == null || !Double.isFinite(setting.max())) {
            return hardMax;
        }

        return Math.min(hardMax, (long) Math.floor(setting.max()));
    }

    private static float floatMin(Field field) {
        ConfigSetting setting = field.getAnnotation(ConfigSetting.class);
        if (setting == null || !Double.isFinite(setting.min())) {
            return -Float.MAX_VALUE;
        }

        return (float) setting.min();
    }

    private static float floatMax(Field field) {
        ConfigSetting setting = field.getAnnotation(ConfigSetting.class);
        if (setting == null || !Double.isFinite(setting.max())) {
            return Float.MAX_VALUE;
        }

        return (float) setting.max();
    }

    private static double doubleMin(Field field) {
        ConfigSetting setting = field.getAnnotation(ConfigSetting.class);
        if (setting == null || !Double.isFinite(setting.min())) {
            return -Double.MAX_VALUE;
        }

        return setting.min();
    }

    private static double doubleMax(Field field) {
        ConfigSetting setting = field.getAnnotation(ConfigSetting.class);
        if (setting == null || !Double.isFinite(setting.max())) {
            return Double.MAX_VALUE;
        }

        return setting.max();
    }

    private static int intValue(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    private static long longValue(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private static float floatValue(Object value) {
        return value instanceof Number number ? number.floatValue() : 0.0f;
    }

    private static double doubleValue(Object value) {
        return value instanceof Number number ? number.doubleValue() : 0.0;
    }

    private static boolean booleanValue(Object value) {
        return value instanceof Boolean bool && bool;
    }

    private static String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static List<String> stringListValue(Object value) {
        List<String> strings = new ArrayList<>();
        if (!(value instanceof Collection<?> collection)) {
            return strings;
        }

        for (Object item : collection) {
            strings.add(stringValue(item));
        }
        return strings;
    }

    @SuppressWarnings("rawtypes")
    private static Enum enumValue(Class<? extends Enum> enumType, Object value) {
        if (enumType.isInstance(value)) {
            return (Enum) value;
        }

        Object[] constants = enumType.getEnumConstants();
        if (constants.length == 0) {
            throw new IllegalArgumentException("Enum config setting has no constants: " + enumType.getName());
        }

        return (Enum) constants[0];
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static long clamp(long value, long min, long max) {
        return Math.max(min, Math.min(max, value));
    }
}
