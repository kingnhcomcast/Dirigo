package io.drahlek.dirigo.mixin;


import io.drahlek.dirigo.event.PlayerMovedEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerMixin {
    //use ints instead of blockpos to avoid construction new blockpos object every time
    @Unique private int mods$lastX = Integer.MIN_VALUE;
    @Unique private int mods$lastY = Integer.MIN_VALUE;
    @Unique private int mods$lastZ = Integer.MIN_VALUE;

    @Inject(at = @At("TAIL"), method = "tick")
    @SuppressWarnings("resource")
    public void onTick(CallbackInfo ci) {
        Player player = (Player) (Object) this;
        if(player.level().isClientSide()) {
            return;
        }

        if (player.noPhysics || player.isPassenger() || player.isSleeping()) {
            return;
        }

        if(player.getBlockX() != mods$lastX || player.getBlockY() != mods$lastY || player.getBlockZ() != mods$lastZ) {
            PlayerMovedEvent.builder()
                    .player(player)
                    .newPos(new BlockPos(player.getBlockX(), player.getBlockY(), player.getBlockZ()))
                    .oldPos(new BlockPos(mods$lastX, mods$lastY, mods$lastZ))
                    .build()
                    .publish();

            mods$lastX = player.getBlockX();
            mods$lastY = player.getBlockY();
            mods$lastZ = player.getBlockZ();
        }
    }
}

