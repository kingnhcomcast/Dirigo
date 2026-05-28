package io.drahlek.dirigo.services;

import io.drahlek.dirigo.config.ConfigPayload;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.lang.reflect.InvocationTargetException;

public class FabricNetworkService implements INetworkService {
    @Override
    public void sendToServer(ConfigPayload payload) {
        try {
            FriendlyByteBuf buffer = PacketByteBufs.create();
            payload.write(buffer);
            Class<?> clientPlayNetworking = Class.forName("net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking");
            clientPlayNetworking.getMethod("send", ResourceLocation.class, FriendlyByteBuf.class)
                    .invoke(null, ConfigPayload.UPDATE_CONFIG_ID, buffer);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("Fabric client networking is not available in this environment.", e);
        }
    }

    @Override
    public void sendToClient(ServerPlayer player, ConfigPayload payload) {
        if (player != null) {
            FriendlyByteBuf buffer = PacketByteBufs.create();
            payload.write(buffer);
            ServerPlayNetworking.send(player, ConfigPayload.UPDATE_CONFIG_ID, buffer);
        }
    }
}