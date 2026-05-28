package io.drahlek.dirigo.networking;

import io.drahlek.dirigo.Constants;
import io.drahlek.dirigo.config.Config;
import io.drahlek.dirigo.config.ConfigPayload;
import io.drahlek.dirigo.permissions.PermissionHelper;
import io.drahlek.dirigo.services.Services;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.function.Supplier;

public class NeoForgeConfigNetworking {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ConfigPayload.UPDATE_CONFIG_ID,
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void registerPayloads() {
        CHANNEL.messageBuilder(ConfigPayload.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ConfigPayload::write)
                .decoder(ConfigPayload::read)
                .consumerMainThread(NeoForgeConfigNetworking::handleServerbound)
                .add();
        CHANNEL.messageBuilder(ConfigPayload.class, 1, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ConfigPayload::write)
                .decoder(ConfigPayload::read)
                .consumerMainThread(NeoForgeConfigNetworking::handleClientbound)
                .add();
    }

    private static void handleClientbound(ConfigPayload payload, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        Config.applyPayload(payload);
        context.setPacketHandled(true);
    }

    private static void handleServerbound(ConfigPayload payload, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer serverPlayer = context.getSender();
        if (serverPlayer == null) {
            context.setPacketHandled(true);
            return;
        }

        if (!PermissionHelper.canModifyConfig(serverPlayer)) {
            Constants.LOG.warn("Config sync: denied update from {}", serverPlayer.getName().getString());
            context.setPacketHandled(true);
            return;
        }

        Config.applyPayload(payload);
        Config.getRegistered(payload.modId())
                .ifPresent(config -> config.save(serverPlayer.level().getServer()));
        context.setPacketHandled(true);
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