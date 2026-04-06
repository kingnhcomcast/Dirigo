package io.drahlek.dirigo;

import io.drahlek.dirigo.schedule.EventScheduler;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@Mod(Constants.MOD_ID)
public class NeoForgeDirigo {
    public NeoForgeDirigo(IEventBus eventBus) {
        // Perform logic in that should be executed on both sides
        Constants.LOG.info("{} Main Initialize", Constants.MOD_NAME);

        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) ->
                EventScheduler.INSTANCE.onServerTick(event.getServer()));
    }
}
