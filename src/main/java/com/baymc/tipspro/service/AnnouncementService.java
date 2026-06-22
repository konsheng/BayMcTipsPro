package com.baymc.tipspro.service;

import com.baymc.tipspro.config.AnnouncementEntry;
import com.baymc.tipspro.config.PluginConfig;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Owns validated announcement content and delivers random announcements.
 *
 * <p>This service never reads config.yml directly. Reloading replaces the immutable configuration
 * snapshot, while scheduled and manual sends share the same random-selection and broadcast path.
 */
public final class AnnouncementService {
    private final Plugin plugin;
    private volatile PluginConfig config;

    /**
     * Creates an announcement service for the given plugin.
     *
     * @param plugin owning Bukkit plugin
     */
    public AnnouncementService(Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Replaces the active runtime configuration.
     *
     * @param config validated configuration to use for later broadcasts
     */
    public void applyConfig(PluginConfig config) {
        this.config = config;
    }

    /**
     * Broadcasts one random valid announcement to every online player.
     *
     * <p>The method intentionally does not check {@code announcements.enabled}; that flag controls
     * only the automatic scheduler. Manual {@code /tips next} sends remain useful for testing even
     * when the timer is disabled.
     *
     * @return broadcast result describing whether a message was selected and how many players were targeted
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
            plugin.getLogger().info("[Announcement] " + announcement.plainText());
        }

        return AnnouncementBroadcastResult.sent(onlinePlayers, announcement);
    }
}
