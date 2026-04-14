package io.drahlek.dirigo.commands;

import com.mojang.brigadier.context.CommandContext;
import io.drahlek.dirigo.annotation.Command;
import io.drahlek.dirigo.config.Config;
import net.minecraft.commands.CommandSourceStack;

@Command(
        value = "config.reload",
        requiresOp = true,
        requiresConfig = true
)
public class ConfigReloadConfigCommand {
    public static int run(CommandContext<CommandSourceStack> context) {
        Config<?> config = ConfigCommandUtil.getConfig(context);
        if (config == null) {
            return 0;
        }

        config.reload();
        CommandUtil.sendString(context, "[" + config.getModId() + "] Config reloaded.");
        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
    }
}
