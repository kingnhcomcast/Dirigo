package io.drahlek.dirigo.services;

import io.drahlek.dirigo.config.ConfigPayload;
import net.minecraft.server.level.ServerPlayer;

public interface INetworkService {
    void sendToServer(ConfigPayload payload);
    void sendToClient(ServerPlayer player, ConfigPayload payload);
}