package com.baymc.tipspro.scheduler;

import com.baymc.tipspro.config.PluginConfig;
import com.baymc.tipspro.service.AnnouncementService;

/**
 * 控制自动公告定时任务的生命周期
 *
 * <p>重载时会先取消旧任务, 再仅在自动公告已启用且至少存在一条有效公告时启动新任务
 */
public final class AnnouncementScheduler {
    private static final long TICKS_PER_SECOND = 20L;

    private final SchedulerAdapter schedulerAdapter;
    private final AnnouncementService announcementService;
    private SchedulerAdapter.ScheduledTaskHandle taskHandle;

    /**
     * 创建公告定时任务管理器
     *
     * @param schedulerAdapter Paper/Folia 调度适配器
     * @param announcementService 定时任务调用的公告服务
     */
    public AnnouncementScheduler(
        SchedulerAdapter schedulerAdapter,
        AnnouncementService announcementService) {
        this.schedulerAdapter = schedulerAdapter;
        this.announcementService = announcementService;
    }

    /**
     * 根据给定配置重启定时任务
     *
     * @param config 已校验的运行配置
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
     * 取消当前正在运行的定时任务
     */
    public void stop() {
        if (taskHandle != null && !taskHandle.isCancelled()) {
            taskHandle.cancel();
        }
        taskHandle = null;
    }

    /**
     * 返回当前是否存在活跃的自动公告定时任务
     *
     * @return 任务句柄存在且未被取消时返回 {@code true}
     */
    public boolean isRunning() {
        return taskHandle != null && !taskHandle.isCancelled();
    }

    private static long secondsToTicks(int seconds) {
        return Math.max(1L, seconds * TICKS_PER_SECOND);
    }
}
