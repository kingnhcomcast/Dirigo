package io.drahlek.dirigo.services;

import io.drahlek.dirigo.config.ConfigPayload;
import io.drahlek.dirigo.networking.NeoForgeConfigNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

public class NeoForgeNetworkService implements INetworkService {
    @Override
    public void sendToServer(ConfigPayload payload) {
        NeoForgeConfigNetworking.CHANNEL.sendToServer(payload);
    }

    @Override
    public void sendToClient(ServerPlayer player, ConfigPayload payload) {
        if (player != null) {
            NeoForgeConfigNetworking.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), payload);
        }
    }
}