package io.drahlek.dirigo.services.services;

import net.minecraft.world.level.block.Block;

public interface IBlockRegistrar {
    public <T extends Block> Block registerBlock(String modId, String name, Class<T> clazz, boolean shouldRegisterItem);
}
