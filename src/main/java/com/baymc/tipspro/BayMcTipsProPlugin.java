package com.baymc.tipspro;

import com.baymc.tipspro.command.TipsCommand;
import com.baymc.tipspro.config.ConfigNotice;
import com.baymc.tipspro.config.InvalidAnnouncement;
import com.baymc.tipspro.config.LanguageCatalog;
import com.baymc.tipspro.config.PluginConfig;
import com.baymc.tipspro.scheduler.AnnouncementScheduler;
import com.baymc.tipspro.scheduler.SchedulerAdapter;
import com.baymc.tipspro.service.AnnouncementService;
import static com.baymc.tipspro.config.LanguageCatalog.placeholder;
import java.io.File;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
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
    private LanguageCatalog language;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveDefaultLanguageConfig();
        reloadLanguage();
        schedulerAdapter = new SchedulerAdapter(this, this::language);
        announcementService = new AnnouncementService(this);
        announcementScheduler = new AnnouncementScheduler(schedulerAdapter, announcementService);
        reloadAnnouncements();
        registerCommand();
        getLogger()
            .info(
                language.message(
                    "logs.enabled",
                    placeholder("scheduler", language.schedulerMode(schedulerAdapter.isFolia()))));
    }

    @Override
    public void onDisable() {
        if (announcementScheduler != null) {
            announcementScheduler.stop();
        }
        if (language != null) {
            getLogger().info(language.message("logs.disabled"));
        }
    }

    /**
     * Reloads config.yml, validates announcements, and restarts the automatic timer.
     *
     * @return active validated configuration after reload
     */
    public PluginConfig reloadAnnouncements() {
        reloadConfig();
        reloadLanguage();
        currentConfig = PluginConfig.load(getConfig(), miniMessage);
        announcementService.applyRuntime(currentConfig, language);
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

    /**
     * Returns the active language catalog.
     *
     * @return runtime language catalog
     */
    public LanguageCatalog language() {
        return language;
    }

    private void registerCommand() {
        PluginCommand command = getCommand("baymctipspro");
        if (command == null) {
            getLogger().severe(language.message("logs.command-missing"));
            return;
        }
        TipsCommand tipsCommand = new TipsCommand(this, miniMessage);
        command.setExecutor(tipsCommand);
        command.setTabCompleter(tipsCommand);
    }

    private void logConfigState(PluginConfig config) {
        for (ConfigNotice notice : config.notices()) {
            getLogger().warning(language.notice(notice));
        }
        for (InvalidAnnouncement invalidAnnouncement : config.invalidAnnouncements()) {
            getLogger()
                .warning(
                    language.message(
                        "logs.invalid-announcement",
                        placeholder("index", invalidAnnouncement.index()),
                        placeholder("reason", language.invalidReason(invalidAnnouncement))));
        }

        if (!config.enabled()) {
            getLogger().info(language.message("logs.automatic-disabled"));
            return;
        }
        if (!config.hasValidAnnouncements()) {
            getLogger().warning(language.message("logs.no-valid-announcements"));
            return;
        }
        getLogger()
            .info(
                language.message(
                    "logs.automatic-enabled",
                    placeholder("valid", config.validAnnouncementCount()),
                    placeholder("invalid", config.invalidAnnouncementCount()),
                    placeholder("interval", config.intervalSeconds()),
                    placeholder("initial_delay", config.initialDelaySeconds())));
    }

    private void reloadLanguage() {
        language = new LanguageCatalog(YamlConfiguration.loadConfiguration(languageFile()));
    }

    private void saveDefaultLanguageConfig() {
        File languageFile = languageFile();
        if (!languageFile.exists()) {
            saveResource("lang/zh_CN.yml", false);
        }
    }

    private File languageFile() {
        return new File(getDataFolder(), "lang/zh_CN.yml");
    }
}
