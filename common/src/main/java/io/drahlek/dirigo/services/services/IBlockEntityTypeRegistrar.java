package io.drahlek.dirigo.services.services;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

public interface IBlockEntityTypeRegistrar {
    <T extends BlockEntity> Supplier<BlockEntityType<T>> registerBlockEntityType(
            String modId,
            String name,
            BlockEntityFactory<T> factory
    );

    @FunctionalInterface
    interface BlockEntityFactory<T extends BlockEntity> {
        T create(BlockPos blockPos, BlockState blockState);
    }
}
