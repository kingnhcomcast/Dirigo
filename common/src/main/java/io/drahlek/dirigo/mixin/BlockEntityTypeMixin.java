package io.drahlek.dirigo.mixin;

import io.drahlek.dirigo.blockentity.BlockEntityTypeCompat;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockEntityType.class)
public abstract class BlockEntityTypeMixin {
    @Inject(method = "isValid", at = @At("RETURN"), cancellable = true)
    private void dirigo$allowExtraValidBlocks(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            return;
        }

        BlockEntityType<?> self = (BlockEntityType<?>) (Object) this;
        if (BlockEntityTypeCompat.isValid(self, state)) {
            cir.setReturnValue(true);
        }
    }
}
