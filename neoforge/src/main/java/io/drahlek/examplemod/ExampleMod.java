package io.drahlek.examplemod;


import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(Constants.MOD_ID)
public class ExampleMod {

    public ExampleMod(IEventBus eventBus) {
        // Perform logic in that should be executed on both sides

        Constants.LOG.info("{} Main Initialize", Constants.MOD_NAME);
        CommonClass.init();
    }
}