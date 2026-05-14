package io.drahlek.dirigo.services.services;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.block.Block;
import java.util.function.Supplier;

public interface IBlockRegistrar {
    default <T extends Block> Supplier<Block> registerBlock(String modId, String name, Class<T> clazz, boolean shouldRegisterItem) {
        return registerBlock(modId, name, clazz, shouldRegisterItem, null);
    }
    <T extends Block> Supplier<Block> registerBlock(String modId, String name, Class<T> clazz, boolean shouldRegisterItem, ResourceKey<CreativeModeTab> creativeTab);
}
