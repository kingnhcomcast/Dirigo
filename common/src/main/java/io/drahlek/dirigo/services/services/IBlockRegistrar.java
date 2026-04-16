package io.drahlek.dirigo.services.services;

import net.minecraft.world.level.block.Block;
import java.util.function.Supplier;

public interface IBlockRegistrar {
    <T extends Block> Supplier<Block> registerBlock(String modId, String name, Class<T> clazz, boolean shouldRegisterItem);
}
