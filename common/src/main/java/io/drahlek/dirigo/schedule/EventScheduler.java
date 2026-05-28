package io.drahlek.dirigo.schedule;

import io.drahlek.dirigo.Constants;
import io.drahlek.dirigo.event.EventBase;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

public final class EventScheduler {
    public static final EventScheduler INSTANCE = new EventScheduler();

    private final NavigableMap<Long, List<Runnable>> tasks = new TreeMap<>();
    private final Object tasksLock = new Object();

    private EventScheduler() {}

    public void scheduleEvent(Level level, EventBase event, long ticks) {
        scheduleCallback(level, event::publish, ticks);
    }

    public void scheduleCallback(Level level, Runnable callback, long ticks) {
        MinecraftServer server = level.getServer();
        if (server == null) {
            throw new IllegalArgumentException("Cannot schedule callback for a level with no server");
        }

        if (ticks < 0) {
            throw new IllegalArgumentException("Attempted to schedule a callback with %s ticks: ticks must be >= 0".formatted(ticks));
        }

        long executeAt = server.getTickCount() + ticks;
        synchronized (tasksLock) {
            tasks.computeIfAbsent(executeAt, k -> new ArrayList<>()).add(callback);
        }
    }

    public void onServerTick(MinecraftServer server) {
        long now = server.getTickCount();
        List<Runnable> dueTasks = new ArrayList<>();

        synchronized (tasksLock) {
            NavigableMap<Long, List<Runnable>> due = tasks.headMap(now, true);
            for (List<Runnable> list : due.values()) {
                dueTasks.addAll(list);
            }
            due.clear();
        }

        for (Runnable task : dueTasks) {
            try {
                task.run();
            } catch (Exception e) {
                Constants.LOG.warn("Error while running callback", e);
            }
        }
    }
}