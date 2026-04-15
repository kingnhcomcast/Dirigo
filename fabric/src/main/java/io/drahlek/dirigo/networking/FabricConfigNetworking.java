package io.drahlek.dirigo.networking;

import io.drahlek.dirigo.Constants;
import io.drahlek.dirigo.config.Config;
import io.drahlek.dirigo.config.ConfigPayload;
import io.drahlek.dirigo.permissions.PermissionHelper;
import io.drahlek.dirigo.services.Services;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class FabricConfigNetworking {
    public static void init() {
        //register payloads
        PayloadTypeRegistry.serverboundPlay().register(ConfigPayload.ID, ConfigPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ConfigPayload.ID, ConfigPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(ConfigPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                if (!PermissionHelper.canModifyConfig(context.player())) {
                    Constants.LOG.warn("Config sync: denied update from {}", context.player().getName().getString());
                    return;
                }

                Config.applyPayload(payload);
                Config.getRegistered(payload.modId())
                        .ifPresent(config -> config.save(context.server()));
            });
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            Constants.LOG.info("Config sync: JOIN for {}", handler.player.getName().getString());
            Config.registeredConfigs().values()
                    .forEach(config -> Services.NETWORK_SERVICE.sendToClient(handler.player, config.toPayload()));
        });
    }
}
