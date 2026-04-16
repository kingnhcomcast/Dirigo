package io.drahlek.dirigo.commands;

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
        arguments = @CommandArgument(name = "setting", type = CommandArgumentType.CONFIG_SETTING),
        requiresOp = true,
        requiresConfig = true
)
public class ConfigGetCommand {
    public static int run(CommandContext<CommandSourceStack> context) {
        Config<?> config = ConfigCommandUtil.getConfig(context);
        if (config == null) {
            return 0;
        }

        Field field = ConfigCommandUtil.findSelectedSettingField(context, config);
        if (field == null) {
            context.getSource().sendFailure(Component.literal("Unknown config setting."));
            return 0;
        }

        ConfigCommandUtil.sendFieldValue(context, config, field);
        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
    }
}
