package com.baymc.tipspro.service;

import com.baymc.tipspro.config.AnnouncementEntry;
import com.baymc.tipspro.config.LanguageCatalog;
import com.baymc.tipspro.config.PluginConfig;
import static com.baymc.tipspro.config.LanguageCatalog.placeholder;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * 保存已校验的公告内容, 并负责随机发送公告
 *
 * <p>这个服务不会直接读取 {@code config.yml} 或 {@code tips.yml}, 重载时会替换不可变配置快照
 * 定时发送和手动发送共用同一套随机选择与广播路径
 */
public final class AnnouncementService {
    /**
     * 所属 Bukkit 插件, 用于写入控制台日志
     */
    private final Plugin plugin;

    /**
     * 当前广播使用的配置快照
     */
    private volatile PluginConfig config;

    /**
     * 当前控制台日志使用的语言文本目录
     */
    private volatile LanguageCatalog language;

    /**
     * 为指定插件创建公告服务
     *
     * @param plugin 所属 Bukkit 插件
     */
    public AnnouncementService(Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 替换当前运行配置和语言文本目录
     *
     * @param config 后续广播使用的已校验配置
     * @param language 用于控制台输出的运行时语言文本目录
     */
    public void applyRuntime(PluginConfig config, LanguageCatalog language) {
        this.config = config;
        this.language = language;
    }

    /**
     * 向所有在线玩家广播一条随机有效公告
     *
     * <p>这个方法不会检查 {@code announcements.enabled}, 该开关只控制自动调度任务, 即使定时任务
     * 被禁用, 手动执行 {@code /tips next} 仍可用于测试公告效果
     *
     * @return 描述是否选中消息以及目标玩家数量的广播结果
     */
    public AnnouncementBroadcastResult broadcastRandomAnnouncement() {
        PluginConfig snapshot = config;
        if (snapshot == null || snapshot.validAnnouncements().isEmpty()) {
            return AnnouncementBroadcastResult.noAnnouncement();
        }

        List<AnnouncementEntry> announcements = snapshot.validAnnouncements();
        AnnouncementEntry announcement =
            announcements.get(ThreadLocalRandom.current().nextInt(announcements.size()));
        int onlinePlayers = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(announcement.component());
            onlinePlayers++;
        }

        if (snapshot.sendToConsole()) {
            LanguageCatalog languageSnapshot = language;
            plugin.getLogger()
                .info(
                    languageSnapshot.message(
                        "logs.announcement-sent",
                        placeholder("message", announcement.plainText())));
        }

        return AnnouncementBroadcastResult.sent(onlinePlayers, announcement);
    }
}
