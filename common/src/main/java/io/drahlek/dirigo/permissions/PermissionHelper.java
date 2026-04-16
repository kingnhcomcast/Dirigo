package io.drahlek.dirigo.permissions;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.Permissions;

public final class PermissionHelper {
    public static final Permission CONFIG_MODIFY_PERMISSION = Permissions.COMMANDS_ADMIN;

    private PermissionHelper() {
    }

    public static boolean canUseOpCommands(CommandSourceStack source) {
        return source.permissions().hasPermission(CONFIG_MODIFY_PERMISSION);
    }

    public static boolean canModifyConfig(ServerPlayer player) {
        return canUseOpCommands(player.createCommandSourceStack());
    }
}
