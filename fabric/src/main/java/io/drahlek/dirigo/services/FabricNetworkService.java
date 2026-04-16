package io.drahlek.dirigo.services;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.lang.reflect.InvocationTargetException;

public class FabricNetworkService implements INetworkService {
    @Override
    public void sendToServer(CustomPacketPayload payload) {
        try {
            Class<?> clientPlayNetworking = Class.forName("net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking");
            clientPlayNetworking.getMethod("send", CustomPacketPayload.class).invoke(null, payload);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("Fabric client networking is not available in this environment.", e);
        }
    }

    @Override
    public void sendToClient(ServerPlayer player, CustomPacketPayload payload) {
        if (player != null) {
            ServerPlayNetworking.send(player, payload);
        }
    }
}
