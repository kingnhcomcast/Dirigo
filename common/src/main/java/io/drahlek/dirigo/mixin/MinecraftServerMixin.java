package io.drahlek.dirigo.mixin;

import io.drahlek.dirigo.Constants;
import io.drahlek.dirigo.config.Config;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {
    @Inject(at = @At("TAIL"), method = "loadLevel")
    private void onLoadLevel(CallbackInfo ci) {
        Constants.LOG.info("Loading registered server configs");
        Config.loadRegistered((MinecraftServer) (Object) this);
    }
}
