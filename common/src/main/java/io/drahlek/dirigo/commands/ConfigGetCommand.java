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
        value = "config.get",
        arguments = @CommandArgument(name = "setting", type = CommandArgumentType.WORD),
        requiresOp = true,
        requiresConfig = true
)
public class ConfigGetCommand {
    static final String SETTING_ARG = "setting";

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

        ConfigCommandUtil.sendFieldValue(context, config, field);
        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
    }
}
