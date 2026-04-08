package io.drahlek.dirigo.schedule;

import io.drahlek.dirigo.Constants;
import io.drahlek.dirigo.event.EventBase;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

/*
Previous implementation failed because it used a process-global, in-memory scheduler for world-owned behavior.
In EventScheduler.java, tasks lives on a static singleton and is just a TreeMap<Long, List<Runnable>>. In the old boots logic, each decay step scheduled a Runnable that captured:
•
the Level
•
the List<BlockPos>
•
the current stage
That creates two problems:
•
Those callbacks are not saved with the world, so a real shutdown/restart loses them entirely.
•
In singleplayer, closing a world does not end the JVM. The singleton survives at the title screen, so queued callbacks from the old world stay in memory and keep the old Level reachable.
That is why the old version could blow up memory and behave badly on world exit. It also has cross-world contamination: if you open another singleplayer world in the same client session, the same singleton scheduler is still there. As the new integrated server ticks upward, old callbacks can become “due” and execute on the server thread even though they still reference the previous world.
The new implementation works because the decay is now owned by Minecraft’s block tick system in CooledLava.java. That system is chunk/world scoped, saved with the world, and naturally stops/resumes with world lifecycle.
scheduleCallback is not inherently bad, but in its current form it is only safe for transient, non-persistent server-session work. It is not a good fit for block/world state.
Good use cases:
•
“Run this mod action 20 ticks later”
•
Short-lived delayed logic where losing it on restart is acceptable
•
Callbacks that capture lightweight data, not live world objects
Bad use cases:
•
Block decay/growth
•
Anything that must survive save/load
•
Anything tied to a specific world/server lifecycle
•
Callbacks that capture Level, chunks, entities, or large collections
If you want EventScheduler to be generally reliable, it should at least:
•
store tasks per MinecraftServer, not in one global map
•
clear that server’s tasks on stop
•
avoid callbacks capturing heavy world objects
•
be documented as non-persistent
So: conceptually yes, current implementation no, not for gameplay state like your lava decay.
 */
public final class EventScheduler {
    public static final EventScheduler INSTANCE = new EventScheduler();

    private final NavigableMap<Long, List<Runnable>> tasks = new TreeMap<>();

    private EventScheduler() {}

    public void scheduleEvent(@NonNull Level level, EventBase event, long ticks) {
        scheduleCallback(level, event::publish, ticks);
    }

    public void scheduleCallback(@NonNull Level level, @NonNull Runnable callback, long ticks) {
        MinecraftServer server = level.getServer();
        if (server == null) {
            throw new IllegalArgumentException("Cannot schedule callback for a level with no server");
        }

        if (ticks < 0) {
            throw new IllegalArgumentException("Attempted to schedule a callback with %s ticks: ticks must be >= 0".formatted(ticks));
        }

        long executeAt = server.getTickCount() + ticks;
        tasks.computeIfAbsent(executeAt, k -> new ArrayList<>()).add(callback);
    }

    public void onServerTick(MinecraftServer server) {
        long now = server.getTickCount();
        NavigableMap<Long, List<Runnable>> due = tasks.headMap(now, true);

        for (List<Runnable> list : new ArrayList<>(due.values())) {
            for (Runnable task : list) {
                try {
                    task.run();
                } catch (Exception e) {
                    Constants.LOG.warn("Error while running callback", e);
                }
            }
        }

        due.clear();
    }

}
