package com.baymc.tipspro.scheduler;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Small runtime adapter over Bukkit and Folia global scheduling APIs.
 *
 * <p>Folia classes are accessed reflectively so the same jar can load on normal Paper servers.
 * BayMcTipsPro only needs global timer work because it sends chat messages and never touches
 * region-owned world state.
 */
public final class SchedulerAdapter {
    private final Plugin plugin;
    private final boolean folia;

    /**
     * Creates a scheduler adapter and detects whether the server is Folia.
     *
     * @param plugin owning Bukkit plugin
     */
    public SchedulerAdapter(Plugin plugin) {
        this.plugin = plugin;
        this.folia = classExists("io.papermc.paper.threadedregions.RegionizedServer");
    }

    /**
     * Returns whether this server exposes Folia's regionized runtime marker class.
     *
     * @return true when Folia is detected
     */
    public boolean isFolia() {
        return folia;
    }

    /**
     * Returns a human-readable scheduler mode for status output.
     *
     * @return scheduler mode name
     */
    public String modeName() {
        return folia ? "Folia Global Scheduler" : "Bukkit Scheduler";
    }

    /**
     * Starts a repeating global task.
     *
     * @param runnable task body to execute
     * @param delayTicks delay before the first run, in server ticks
     * @param periodTicks delay between later runs, in server ticks
     * @return cancellable scheduled task handle
     */
    public ScheduledTaskHandle runGlobalTimer(
        Runnable runnable,
        long delayTicks,
        long periodTicks) {
        long safeDelay = Math.max(1L, delayTicks);
        long safePeriod = Math.max(1L, periodTicks);
        if (!folia) {
            BukkitTask task =
                Bukkit.getScheduler().runTaskTimer(plugin, runnable, safeDelay, safePeriod);
            return new BukkitScheduledTaskHandle(task);
        }

        Object scheduler = invoke(Bukkit.getServer(), "getGlobalRegionScheduler", new Class<?>[0]);
        Object task = invoke(
            scheduler,
            "runAtFixedRate",
            new Class<?>[]{Plugin.class, Consumer.class, long.class, long.class},
            plugin,
            (Consumer<Object>) ignored -> runnable.run(),
            safeDelay,
            safePeriod);
        return new FoliaScheduledTaskHandle(task);
    }

    private static boolean classExists(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    private static Object invoke(
        Object target,
        String methodName,
        Class<?>[] parameterTypes,
        Object... args) {
        try {
            Method method = target.getClass().getMethod(methodName, parameterTypes);
            method.setAccessible(true);
            return method.invoke(target, args);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException exception) {
            throw new IllegalStateException(
                "Unable to call scheduler method " + methodName,
                exception);
        }
    }

    private static Object invokeScheduledTask(Object task, String methodName) {
        if (task == null) {
            return null;
        }
        try {
            Method method =
                Class.forName("io.papermc.paper.threadedregions.scheduler.ScheduledTask")
                    .getMethod(methodName);
            return method.invoke(task);
        } catch (ClassNotFoundException
            | IllegalAccessException
            | InvocationTargetException
            | NoSuchMethodException exception) {
            throw new IllegalStateException(
                "Unable to call Folia scheduled task method " + methodName,
                exception);
        }
    }

    /**
     * Cancellable handle returned by the scheduler adapter.
     */
    public interface ScheduledTaskHandle {
        /**
         * Cancels the scheduled task.
         */
        void cancel();

        /**
         * Returns whether the scheduled task has already been cancelled.
         *
         * @return true when the task is cancelled
         */
        boolean isCancelled();
    }

    private record BukkitScheduledTaskHandle(BukkitTask task) implements ScheduledTaskHandle {
        @Override
        public void cancel() {
            task.cancel();
        }

        @Override
        public boolean isCancelled() {
            return task.isCancelled();
        }
    }

    private record FoliaScheduledTaskHandle(Object task) implements ScheduledTaskHandle {
        @Override
        public void cancel() {
            invokeScheduledTask(task, "cancel");
        }

        @Override
        public boolean isCancelled() {
            Object cancelled = invokeScheduledTask(task, "isCancelled");
            return cancelled instanceof Boolean value && value;
        }
    }
}
