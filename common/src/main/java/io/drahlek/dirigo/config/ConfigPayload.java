package io.drahlek.dirigo.config;

import io.drahlek.dirigo.Constants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record ConfigPayload(String modId, String json) {
    public static final ResourceLocation UPDATE_CONFIG_ID =
            new ResourceLocation(Constants.MOD_ID, "update_config");

    public void write(FriendlyByteBuf buffer) {
        buffer.writeUtf(modId);
        buffer.writeUtf(json);
    }

    public static ConfigPayload read(FriendlyByteBuf buffer) {
        return new ConfigPayload(buffer.readUtf(), buffer.readUtf());
    }
}