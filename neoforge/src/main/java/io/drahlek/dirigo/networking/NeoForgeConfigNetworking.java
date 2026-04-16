package io.drahlek.dirigo.networking;

import io.drahlek.dirigo.Constants;
import io.drahlek.dirigo.config.Config;
import io.drahlek.dirigo.config.ConfigPayload;
import io.drahlek.dirigo.permissions.PermissionHelper;
import io.drahlek.dirigo.services.Services;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class NeoForgeConfigNetworking {
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(Constants.MOD_ID);
        registrar.playBidirectional(
                ConfigPayload.ID,
                ConfigPayload.CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (!(context.player() instanceof ServerPlayer serverPlayer)) {
                        return;
                    }

                    if (!PermissionHelper.canModifyConfig(serverPlayer)) {
                        Constants.LOG.warn("Config sync: denied update from {}", serverPlayer.getName().getString());
                        return;
                    }

                    Config.applyPayload(payload);
                    Config.getRegistered(payload.modId())
                            .ifPresent(config -> config.save(serverPlayer.level().getServer()));
                }),
                (payload, context) -> context.enqueueWork(() ->
                        Config.applyPayload(payload)
                )
        );
    }

    public static void syncConfigsOnJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        Constants.LOG.info("Config sync: JOIN for {}", serverPlayer.getName().getString());
        Config.registeredConfigs().values()
                .forEach(config -> Services.NETWORK_SERVICE.sendToClient(serverPlayer, config.toPayload()));
    }
}
