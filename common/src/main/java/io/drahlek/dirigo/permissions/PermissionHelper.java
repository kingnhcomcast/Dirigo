package io.drahlek.dirigo.permissions;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

public final class PermissionHelper {
    public static final int CONFIG_MODIFY_PERMISSION_LEVEL = 2;

    private PermissionHelper() {
    }

    public static boolean canUseOpCommands(CommandSourceStack source) {
        return source.hasPermission(CONFIG_MODIFY_PERMISSION_LEVEL);
    }

    public static boolean canModifyConfig(ServerPlayer player) {
        return canUseOpCommands(player.createCommandSourceStack());
    }
}
