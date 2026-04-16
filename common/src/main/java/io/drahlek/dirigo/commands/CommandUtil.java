package io.drahlek.dirigo.commands;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

public final class CommandUtil {
    private CommandUtil() {
    }

    public static void sendString(CommandContext<CommandSourceStack> context, String message) {
        context.getSource().sendSuccess(
                () -> Component.literal(message),
                false
        );
    }
}
