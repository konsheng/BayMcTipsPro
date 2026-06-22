package com.baymc.tipspro.scheduler;

import com.baymc.tipspro.config.LanguageCatalog;
import static com.baymc.tipspro.config.LanguageCatalog.placeholder;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Supplier;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Bukkit 与 Folia 全局调度 API 的轻量运行时适配器
 *
 * <p>Folia 类通过反射访问, 因此同一个 jar 可以在普通 Paper 服务器上加载, BayMcTipsPro 只发送
 * 聊天消息, 不触碰由区域线程拥有的世界状态, 所以这里只需要全局定时任务能力
 */
public final class SchedulerAdapter {
    private static final String FOLIA_RUNTIME_CLASS =
        "io.papermc.paper.threadedregions.RegionizedServer";

    private final Plugin plugin;
    private final Supplier<LanguageCatalog> languageSupplier;
    private final boolean folia;

    /**
     * 创建调度适配器, 并检测当前服务器是否为 Folia
     *
     * @param plugin 所属 Bukkit 插件
     * @param languageSupplier 当前语言文本目录供应器, 用于运行时错误文本
     */
    public SchedulerAdapter(Plugin plugin, Supplier<LanguageCatalog> languageSupplier) {
        this.plugin = plugin;
        this.languageSupplier = languageSupplier;
        this.folia = foliaRuntimeAvailable();
    }

    /**
     * 返回当前服务器是否暴露 Folia 区域化运行时标记类
     *
     * @return 检测到 Folia 时返回 {@code true}
     */
    public boolean isFolia() {
        return folia;
    }

    /**
     * 启动一个重复执行的全局任务
     *
     * @param runnable 要执行的任务内容
     * @param delayTicks 首次执行前的延迟, 单位为服务器 tick
     * @param periodTicks 后续两次执行之间的间隔, 单位为服务器 tick
     * @return 可取消的调度任务句柄
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
        return new FoliaScheduledTaskHandle(task, languageSupplier);
    }

    private static boolean foliaRuntimeAvailable() {
        try {
            Class.forName(FOLIA_RUNTIME_CLASS);
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    private Object invoke(
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
                languageSupplier.get()
                    .message(
                        "errors.scheduler-method-call",
                        placeholder("method", methodName)),
                exception);
        }
    }

    private static Object invokeScheduledTask(
        Object task,
        String methodName,
        Supplier<LanguageCatalog> languageSupplier) {
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
                languageSupplier.get()
                    .message(
                        "errors.folia-task-method-call",
                        placeholder("method", methodName)),
                exception);
        }
    }

    /**
     * 调度适配器返回的可取消任务句柄
     */
    public interface ScheduledTaskHandle {
        /**
         * 取消调度任务
         */
        void cancel();

        /**
         * 返回调度任务是否仍然活跃
         *
         * @return 任务仍然活跃时返回 {@code true}
         */
        boolean isActive();
    }

    private record BukkitScheduledTaskHandle(BukkitTask task) implements ScheduledTaskHandle {
        @Override
        public void cancel() {
            task.cancel();
        }

        @Override
        public boolean isActive() {
            return !task.isCancelled();
        }
    }

    private record FoliaScheduledTaskHandle(
        Object task,
        Supplier<LanguageCatalog> languageSupplier) implements ScheduledTaskHandle {
        @Override
        public void cancel() {
            invokeScheduledTask(task, "cancel", languageSupplier);
        }

        @Override
        public boolean isActive() {
            Object cancelled = invokeScheduledTask(task, "isCancelled", languageSupplier);
            return !(cancelled instanceof Boolean value) || !value;
        }
    }
}
