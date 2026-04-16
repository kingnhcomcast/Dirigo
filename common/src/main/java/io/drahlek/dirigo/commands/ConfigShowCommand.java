package io.drahlek.dirigo.commands;

import com.mojang.brigadier.context.CommandContext;
import io.drahlek.dirigo.annotation.Command;
import io.drahlek.dirigo.config.Config;
import net.minecraft.commands.CommandSourceStack;

@Command(
        value = "config.show",
        requiresOp = true,
        requiresConfig = true
)
public class ConfigShowCommand {
    public static int run(CommandContext<CommandSourceStack> context) {
        Config<?> config = ConfigCommandUtil.getConfig(context);
        if (config == null) {
            return 0;
        }

        CommandUtil.sendString(context, "[" + config.getModId() + "] Config:");
        ConfigCommandUtil.configSettingFields(config)
                .forEach(field -> ConfigCommandUtil.sendFieldValue(context, config, field));
        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
    }
}
