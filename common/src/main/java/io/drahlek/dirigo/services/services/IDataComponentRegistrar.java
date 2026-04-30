package io.drahlek.dirigo.services.services;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public interface IDataComponentRegistrar {
    <T> DataComponentType<T> register(
            String namespace,
            String path,
            Codec<T> codec,
            StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec,
            boolean persistent,
            boolean networkSynchronized
    );
}
