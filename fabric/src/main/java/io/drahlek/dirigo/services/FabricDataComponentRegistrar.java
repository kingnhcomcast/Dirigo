package io.drahlek.dirigo.services;

import com.mojang.serialization.Codec;
import io.drahlek.dirigo.services.services.IDataComponentRegistrar;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

public class FabricDataComponentRegistrar implements IDataComponentRegistrar {
    @Override
    public synchronized <T> DataComponentType<T> register(String namespace, String path, Codec<T> codec, StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec, boolean persistent, boolean networkSynchronized) {
        DataComponentType.Builder<T> builder = DataComponentType.builder();

        if (persistent) {
            builder.persistent(codec);
        }

        if (networkSynchronized) {
            builder.networkSynchronized(streamCodec);
        }

        DataComponentType<T> type = builder.build();

        return Registry.register(
                BuiltInRegistries.DATA_COMPONENT_TYPE,
                ResourceLocation.fromNamespaceAndPath(namespace, path),
                type
        );
    }
}
