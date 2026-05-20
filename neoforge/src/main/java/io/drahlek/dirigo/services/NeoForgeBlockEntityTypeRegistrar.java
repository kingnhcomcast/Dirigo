package io.drahlek.dirigo.services;

import io.drahlek.dirigo.services.services.IBlockEntityTypeRegistrar;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.lang.reflect.Constructor;
import java.lang.reflect.Proxy;
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
        DeferredHolder<BlockEntityType<?>, BlockEntityType<T>> holder = getOrCreateRegistry(modId)
                .register(name, () -> createType(factory));
        return holder::get;
    }

    private static DeferredRegister<BlockEntityType<?>> getOrCreateRegistry(String modId) {
        return REGISTRIES.computeIfAbsent(modId, id -> DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, id));
    }

    @SuppressWarnings("unchecked")
    private static <T extends BlockEntity> BlockEntityType<T> createType(BlockEntityFactory<T> factory) {
        try {
            Class<?> supplierClass = Class.forName("net.minecraft.world.level.block.entity.BlockEntityType$BlockEntitySupplier");
            Object supplier = Proxy.newProxyInstance(
                    BlockEntityType.class.getClassLoader(),
                    new Class<?>[]{supplierClass},
                    (proxy, method, args) -> factory.create((BlockPos) args[0], (BlockState) args[1])
            );
            Constructor<?> constructor = BlockEntityType.class.getDeclaredConstructor(supplierClass, Set.class);
            constructor.setAccessible(true);
            return (BlockEntityType<T>) constructor.newInstance(supplier, Set.of());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to create block entity type", e);
        }
    }
}
