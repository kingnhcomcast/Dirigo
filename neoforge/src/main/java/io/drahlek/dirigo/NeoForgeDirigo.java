package io.drahlek.dirigo;

import io.drahlek.dirigo.networking.NeoForgeConfigNetworking;
import io.drahlek.dirigo.schedule.EventScheduler;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Constants.MOD_ID)
public class NeoForgeDirigo {
    public NeoForgeDirigo() {
        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
        Constants.LOG.info("{} Main Initialize", Constants.MOD_NAME);

        NeoForgeConfigNetworking.registerPayloads();

        MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) {
                EventScheduler.INSTANCE.onServerTick(event.getServer());
            }
        });
        MinecraftForge.EVENT_BUS.addListener(NeoForgeConfigNetworking::syncConfigsOnJoin);

        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> NeoForgeDirigo::initClient);
    }

    private static void initClient() {
        try {
            Class<?> clientClass = Class.forName("io.drahlek.dirigo.client.NeoForgeDirigoClient");
            clientClass.getMethod("init").invoke(null);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to initialize Dirigo NeoForge client hooks", e);
        }
    }
}