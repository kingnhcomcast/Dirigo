package io.drahlek.dirigo.config;

import com.google.gson.Gson;
import io.drahlek.dirigo.Constants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ConfigPayload(String modId, String json) implements CustomPacketPayload {
    public static final Identifier UPDATE_CONFIG_ID =
            Identifier.fromNamespaceAndPath(Constants.MOD_ID, "update_config");

    public static final CustomPacketPayload.Type<ConfigPayload> ID = new CustomPacketPayload.Type<>(UPDATE_CONFIG_ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, ConfigPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,
                    ConfigPayload::modId,
                    ByteBufCodecs.STRING_UTF8,
                    ConfigPayload::json,
                    ConfigPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
