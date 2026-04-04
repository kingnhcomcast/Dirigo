package io.drahlek.dirigo;

import net.fabricmc.api.ModInitializer;

public class Dirigo implements ModInitializer {
    @Override
    public void onInitialize() {
        Constants.LOG.info("{} init", Constants.MOD_NAME);
    }
}
