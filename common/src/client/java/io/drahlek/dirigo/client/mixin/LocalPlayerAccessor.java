package io.drahlek.dirigo.client.mixin;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.permissions.PermissionSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LocalPlayer.class)
public interface LocalPlayerAccessor {
    @Accessor("permissions")
    PermissionSet dirigo$getPermissions();
}
