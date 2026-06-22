package com.baymc.tipspro.scheduler;

import com.baymc.tipspro.config.PluginConfig;
import com.baymc.tipspro.service.AnnouncementService;

/**
 * Controls the lifecycle of the automatic announcement timer.
 *
 * <p>Reload always cancels the old task first, then starts a new one only when automatic
 * announcements are enabled and at least one valid announcement exists.
 */
public final class AnnouncementScheduler {
    private static final long TICKS_PER_SECOND = 20L;

    private final SchedulerAdapter schedulerAdapter;
    private final AnnouncementService announcementService;
    private SchedulerAdapter.ScheduledTaskHandle taskHandle;

    /**
     * Creates a scheduler manager for announcement timers.
     *
     * @param schedulerAdapter Paper/Folia scheduler adapter
     * @param announcementService service invoked by the timer
     */
    public AnnouncementScheduler(
        SchedulerAdapter schedulerAdapter,
        AnnouncementService announcementService) {
        this.schedulerAdapter = schedulerAdapter;
        this.announcementService = announcementService;
    }

    /**
     * Restarts the timer according to the supplied configuration.
     *
     * @param config validated runtime configuration
     */
    public void restart(PluginConfig config) {
        stop();
        if (!config.enabled() || !config.hasValidAnnouncements()) {
            return;
        }
        taskHandle = schedulerAdapter.runGlobalTimer(
            announcementService::broadcastRandomAnnouncement,
            secondsToTicks(config.initialDelaySeconds()),
            secondsToTicks(config.intervalSeconds()));
    }

    /**
     * Cancels the active timer when one exists.
     */
    public void stop() {
        if (taskHandle != null && !taskHandle.isCancelled()) {
            taskHandle.cancel();
        }
        taskHandle = null;
    }

    /**
     * Returns whether an automatic announcement timer is currently active.
     *
     * @return true when a timer handle exists and has not been cancelled
     */
    public boolean isRunning() {
        return taskHandle != null && !taskHandle.isCancelled();
    }

    private static long secondsToTicks(int seconds) {
        return Math.max(1L, seconds * TICKS_PER_SECOND);
    }
}
