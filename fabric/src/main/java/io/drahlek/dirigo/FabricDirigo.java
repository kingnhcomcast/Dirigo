package io.drahlek.dirigo;

import io.drahlek.dirigo.networking.FabricConfigNetworking;
import io.drahlek.dirigo.schedule.EventScheduler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public class FabricDirigo implements ModInitializer {
    @Override
    public void onInitialize() {
        Constants.LOG.info("{} Fabric init", Constants.MOD_NAME);

        FabricConfigNetworking.init();
        ServerTickEvents.END_SERVER_TICK.register(EventScheduler.INSTANCE::onServerTick);
    }
}
