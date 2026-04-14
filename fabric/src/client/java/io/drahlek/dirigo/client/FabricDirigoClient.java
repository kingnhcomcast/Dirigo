package io.drahlek.dirigo.client;

import io.drahlek.dirigo.client.services.ClientServices;
import io.drahlek.dirigo.services.services.IConfigScreenRegistrar;
import net.fabricmc.api.ClientModInitializer;

public final class FabricDirigoClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientServices.configScreenRegistrar().ifPresent(IConfigScreenRegistrar::initialize);
    }
}
