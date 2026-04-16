package io.drahlek.dirigo.commands;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.drahlek.dirigo.config.Config;
import io.drahlek.dirigo.config.ConfigFieldUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

import java.lang.reflect.Field;
import java.util.stream.Stream;

final class ConfigCommandUtil {
    private ConfigCommandUtil() {
    }

    static Stream<Field> configSettingFields(Config<?> config) {
        return ConfigFieldUtil.configSettingFields(config);
    }

    static Config<?> getConfig(CommandContext<CommandSourceStack> context) {
        String modId = getModId(context);
        if (modId == null) {
            context.getSource().sendFailure(Component.literal("Unable to resolve mod id for config command."));
            return null;
        }

        Config<?> config = Config.getRegistered(modId).orElse(null);
        if (config == null) {
            context.getSource().sendFailure(Component.literal("No config registered for '" + modId + "'."));
        }

        return config;
    }

    static Field findSettingField(Config<?> config, String rawName) {
        return ConfigFieldUtil.findSettingField(config, rawName);
    }

    static Field findSelectedSettingField(CommandContext<CommandSourceStack> context, Config<?> config) {
        for (int index = context.getNodes().size() - 1; index >= 0; index--) {
            if (!(context.getNodes().get(index).getNode() instanceof LiteralCommandNode<?>)) {
                continue;
            }

            Field field = findSettingField(config, context.getNodes().get(index).getNode().getName());
            if (field != null) {
                return field;
            }
        }

        return null;
    }

    static void sendFieldValue(CommandContext<CommandSourceStack> context, Config<?> config, Field field) {
        Object value = ConfigFieldUtil.readConfigValue(config, field);
        CommandUtil.sendString(context, String.format("  %s: %s", settingName(field), value));
    }

    static void setConfigValue(Config<?> config, Field field, Object value) {
        ConfigFieldUtil.setConfigValue(config, field, value);
    }

    static String validateRange(Field field, Object value) {
        return ConfigFieldUtil.validateRange(field, value);
    }

    static Object parseFieldValue(Field field, String raw) {
        return ConfigFieldUtil.parseFieldValue(field, raw);
    }

    static Object parseStringLikeFieldValue(Field field, String raw) {
        return ConfigFieldUtil.parseStringLikeFieldValue(field, raw);
    }

    static String settingName(Field field) {
        return ConfigFieldUtil.settingName(field);
    }

    static void save(Config<?> config, MinecraftServer server) {
        config.save(server);
    }

    private static String getModId(CommandContext<CommandSourceStack> context) {
        if (context.getNodes().isEmpty()) {
            return null;
        }

        return context.getNodes().get(0).getNode().getName();
    }
}
