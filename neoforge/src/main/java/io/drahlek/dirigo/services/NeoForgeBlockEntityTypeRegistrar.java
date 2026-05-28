package io.drahlek.dirigo.services;

import io.drahlek.dirigo.services.services.IBlockEntityTypeRegistrar;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class NeoForgeBlockEntityTypeRegistrar implements IBlockEntityTypeRegistrar {
    private static final Map<String, DeferredRegister<BlockEntityType<?>>> REGISTRIES = new ConcurrentHashMap<>();
    private static final Set<String> INITIALIZED_MODS = ConcurrentHashMap.newKeySet();

    public synchronized void initialize(IEventBus eventBus, String modId) {
        if (INITIALIZED_MODS.add(modId)) {
            getOrCreateRegistry(modId).register(eventBus);
        }
    }

    @Override
    public synchronized <T extends BlockEntity> Supplier<BlockEntityType<T>> registerBlockEntityType(
            String modId,
            String name,
            BlockEntityFactory<T> factory
    ) {
        RegistryObject<BlockEntityType<T>> holder = getOrCreateRegistry(modId)
                .register(name, () -> createType(factory));
        return holder::get;
    }

    private static DeferredRegister<BlockEntityType<?>> getOrCreateRegistry(String modId) {
        return REGISTRIES.computeIfAbsent(modId, id -> DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, id));
    }

    private static <T extends BlockEntity> BlockEntityType<T> createType(BlockEntityFactory<T> factory) {
        return BlockEntityType.Builder.of(factory::create).build(null);
    }
}