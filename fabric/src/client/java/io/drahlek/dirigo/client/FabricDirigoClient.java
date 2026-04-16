package io.drahlek.dirigo.client;

import io.drahlek.dirigo.client.services.ClientServices;
import io.drahlek.dirigo.config.Config;
import io.drahlek.dirigo.config.ConfigPayload;
import io.drahlek.dirigo.services.services.IConfigScreenRegistrar;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class FabricDirigoClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientServices.configScreenRegistrar().ifPresent(IConfigScreenRegistrar::initialize);
        ClientPlayNetworking.registerGlobalReceiver(ConfigPayload.ID, (payload, context) -> {
            context.client().execute(() -> Config.applyPayload(payload));
        });
    }
}
