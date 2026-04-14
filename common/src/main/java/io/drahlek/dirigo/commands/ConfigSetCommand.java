package io.drahlek.dirigo.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import io.drahlek.dirigo.annotation.Command;
import io.drahlek.dirigo.annotation.CommandArgument;
import io.drahlek.dirigo.annotation.CommandArgumentType;
import io.drahlek.dirigo.config.Config;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Field;

@Command(
        value = "config.set",
        arguments = {
                @CommandArgument(name = "setting", type = CommandArgumentType.WORD),
                @CommandArgument(name = "value", type = CommandArgumentType.GREEDY_STRING)
        },
        requiresOp = true,
        requiresConfig = true
)
public class ConfigSetCommand {
    static final String SETTING_ARG = "setting";
    static final String VALUE_ARG = "value";

    public static int run(CommandContext<CommandSourceStack> context) {
        Config<?> config = ConfigCommandUtil.getConfig(context);
        if (config == null) {
            return 0;
        }

        String settingName = StringArgumentType.getString(context, SETTING_ARG);
        Field field = ConfigCommandUtil.findSettingField(config, settingName);
        if (field == null) {
            context.getSource().sendFailure(Component.literal("Unknown config setting '" + settingName + "'."));
            return 0;
        }

        Object parsedValue;
        try {
            parsedValue = ConfigCommandUtil.parseFieldValue(field, StringArgumentType.getString(context, VALUE_ARG));
        } catch (IllegalArgumentException e) {
            context.getSource().sendFailure(Component.literal(e.getMessage()));
            return 0;
        }

        String validationError = ConfigCommandUtil.validateRange(field, parsedValue);
        if (validationError != null) {
            context.getSource().sendFailure(Component.literal(validationError));
            return 0;
        }

        ConfigCommandUtil.setConfigValue(config, field, parsedValue);
        ConfigCommandUtil.save(config);
        ConfigCommandUtil.sendFieldValue(context, config, field);
        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
    }
}
