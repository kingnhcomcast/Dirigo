package io.drahlek.dirigo.services;

import com.mojang.serialization.Codec;
import io.drahlek.dirigo.services.services.IDataComponentRegistrar;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class NeoForgeDataComponentRegistrar implements IDataComponentRegistrar {
    private static final Map<String, DeferredRegister.DataComponents> DATA_COMPONENT_REGISTRIES = new ConcurrentHashMap<>();
    private static final Set<String> INITIALIZED_MODS = ConcurrentHashMap.newKeySet();

    @Override
    public synchronized <T> DataComponentType<T> register(
            String namespace,
            String path,
            Codec<T> codec,
            StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec,
            boolean persistent,
            boolean networkSynchronized
    ) {
        DeferredRegister.DataComponents registry = getOrCreateRegistry(namespace);
        initialize(namespace, registry);

        DataComponentType.Builder<T> builder = DataComponentType.builder();

        if (persistent) {
            builder.persistent(codec);
        }

        if (networkSynchronized) {
            builder.networkSynchronized(streamCodec);
        }

        DataComponentType<T> type = builder.build();
        registry.register(path, () -> type);
        return type;
    }

    private static DeferredRegister.DataComponents getOrCreateRegistry(String modId) {
        return DATA_COMPONENT_REGISTRIES.computeIfAbsent(
                modId,
                id -> DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, id)
        );
    }

    private static void initialize(String modId, DeferredRegister.DataComponents registry) {
        if (INITIALIZED_MODS.add(modId)) {
            registry.register(resolveEventBus(modId));
        }
    }

    private static IEventBus resolveEventBus(String modId) {
        ModList modList = ModList.get();
        if (modList == null) {
            throw new IllegalStateException("NeoForge mod list is not available while registering data components for " + modId);
        }

        IEventBus eventBus = modList.getModContainerById(modId)
                .orElseThrow(() -> new IllegalStateException("Could not find NeoForge mod container for " + modId))
                .getEventBus();

        if (eventBus == null) {
            throw new IllegalStateException("NeoForge mod container for " + modId + " does not expose a mod event bus");
        }

        return eventBus;
    }
}
