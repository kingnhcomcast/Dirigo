package io.drahlek.dirigo.networking;

import io.drahlek.dirigo.Constants;
import io.drahlek.dirigo.config.Config;
import io.drahlek.dirigo.config.ConfigPayload;
import io.drahlek.dirigo.permissions.PermissionHelper;
import io.drahlek.dirigo.services.Services;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class FabricConfigNetworking {
    public static void init() {
        ServerPlayNetworking.registerGlobalReceiver(ConfigPayload.UPDATE_CONFIG_ID, (server, player, handler, buffer, responseSender) -> {
            ConfigPayload payload = ConfigPayload.read(buffer);
            server.execute(() -> {
                if (!PermissionHelper.canModifyConfig(player)) {
                    Constants.LOG.warn("Config sync: denied update from {}", player.getName().getString());
                    return;
                }

                Config.applyPayload(payload);
                Config.getRegistered(payload.modId())
                        .ifPresent(config -> config.save(server));
            });
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            Constants.LOG.info("Config sync: JOIN for {}", handler.player.getName().getString());
            Config.registeredConfigs().values()
                    .forEach(config -> Services.NETWORK_SERVICE.sendToClient(handler.player, config.toPayload()));
        });
    }
}