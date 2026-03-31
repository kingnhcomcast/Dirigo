package io.drahlek.examplemod.mixin;

import io.drahlek.examplemod.Constants;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public class MineCraftServerMixin {
    @Inject(at = @At("HEAD"), method = "loadLevel")
    private void onLoadLevel(CallbackInfo ci) {
        Constants.LOG.info("Mixin Common Main");
    }
}