package com.baymc.tipspro;

import com.baymc.tipspro.command.TipsCommand;
import com.baymc.tipspro.config.InvalidAnnouncement;
import com.baymc.tipspro.config.PluginConfig;
import com.baymc.tipspro.scheduler.AnnouncementScheduler;
import com.baymc.tipspro.scheduler.SchedulerAdapter;
import com.baymc.tipspro.service.AnnouncementService;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Composition root for the BayMcTipsPro Paper/Folia plugin.
 *
 * <p>The plugin stays intentionally narrow: it loads MiniMessage chat announcements, validates
 * them, starts one Folia-aware global timer, and exposes the /baymctipspro command with /tips as
 * an alias. World, chunk, entity, database, GUI, title, action bar, and boss bar features are
 * outside this plugin's responsibility.
 */
public final class BayMcTipsProPlugin extends JavaPlugin {
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    private SchedulerAdapter schedulerAdapter;
    private AnnouncementService announcementService;
    private AnnouncementScheduler announcementScheduler;
    private PluginConfig currentConfig;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        schedulerAdapter = new SchedulerAdapter(this);
        announcementService = new AnnouncementService(this);
        announcementScheduler = new AnnouncementScheduler(schedulerAdapter, announcementService);
        reloadAnnouncements();
        registerCommand();
        getLogger().info("BayMcTipsPro enabled using " + schedulerAdapter.modeName() + ".");
    }

    @Override
    public void onDisable() {
        if (announcementScheduler != null) {
            announcementScheduler.stop();
        }
        getLogger().info("BayMcTipsPro disabled.");
    }

    /**
     * Reloads config.yml, validates announcements, and restarts the automatic timer.
     *
     * @return active validated configuration after reload
     */
    public PluginConfig reloadAnnouncements() {
        reloadConfig();
        currentConfig = PluginConfig.load(getConfig(), miniMessage);
        announcementService.applyConfig(currentConfig);
        announcementScheduler.restart(currentConfig);
        logConfigState(currentConfig);
        return currentConfig;
    }

    /**
     * Returns the active validated runtime configuration.
     *
     * @return current plugin configuration
     */
    public PluginConfig currentConfig() {
        return currentConfig;
    }

    /**
     * Returns the announcement delivery service used by commands and the timer.
     *
     * @return announcement service
     */
    public AnnouncementService announcementService() {
        return announcementService;
    }

    /**
     * Returns the automatic announcement scheduler.
     *
     * @return announcement scheduler
     */
    public AnnouncementScheduler announcementScheduler() {
        return announcementScheduler;
    }

    /**
     * Returns the active Paper/Folia scheduler adapter.
     *
     * @return scheduler adapter
     */
    public SchedulerAdapter schedulerAdapter() {
        return schedulerAdapter;
    }

    private void registerCommand() {
        PluginCommand command = getCommand("baymctipspro");
        if (command == null) {
            getLogger().severe("Command baymctipspro is missing from plugin.yml.");
            return;
        }
        TipsCommand tipsCommand = new TipsCommand(this, miniMessage);
        command.setExecutor(tipsCommand);
        command.setTabCompleter(tipsCommand);
    }

    private void logConfigState(PluginConfig config) {
        for (String warning : config.warnings()) {
            getLogger().warning(warning);
        }
        for (InvalidAnnouncement invalidAnnouncement : config.invalidAnnouncements()) {
            getLogger()
                .warning(
                    "Skipped announcement #"
                        + invalidAnnouncement.index()
                        + ": "
                        + invalidAnnouncement.reason());
        }

        if (!config.enabled()) {
            getLogger().info("Automatic announcements are disabled.");
            return;
        }
        if (!config.hasValidAnnouncements()) {
            getLogger().warning("No valid announcements are available; timer was not started.");
            return;
        }
        getLogger()
            .info(
                "Automatic announcements enabled: "
                    + config.validAnnouncementCount()
                    + " valid, "
                    + config.invalidAnnouncementCount()
                    + " invalid, interval "
                    + config.intervalSeconds()
                    + "s, initial delay "
                    + config.initialDelaySeconds()
                    + "s.");
    }
}
