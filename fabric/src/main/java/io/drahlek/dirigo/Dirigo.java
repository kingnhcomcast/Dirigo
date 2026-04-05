package io.drahlek.dirigo;

import net.fabricmc.api.ModInitializer;

public class Dirigo implements ModInitializer {
    @Override
    public void onInitialize() {
        Constants.LOG.info("{} Fabric init", Constants.MOD_NAME);
    }
}
