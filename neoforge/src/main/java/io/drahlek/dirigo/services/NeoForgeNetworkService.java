package io.drahlek.dirigo.services;

import io.drahlek.dirigo.Constants;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public class NeoForgeNetworkService implements INetworkService {
    @Override
    public void sendToServer(CustomPacketPayload payload) {
        PacketDistributor.sendToServer(payload);
    }

    @Override
    public void sendToClient(ServerPlayer player, CustomPacketPayload payload) {
        if (player != null) {
            PacketDistributor.sendToPlayer(player, payload);
        }
    }
}
