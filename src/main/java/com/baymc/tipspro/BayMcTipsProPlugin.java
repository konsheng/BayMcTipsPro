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
 * BayMcTipsPro 在 Paper 与 Folia 环境中的插件组装入口
 *
 * <p>插件只负责加载和校验 MiniMessage 聊天栏公告, 启动一个兼容 Folia 的全局定时任务
 * 并注册 {@code /baymctipspro} 命令及 {@code /tips} 别名, 世界, 区块, 实体, 数据库
 * GUI, 标题, 动作栏和 BossBar 都不属于这个插件的职责范围
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
        saveDefaultTipsConfig();
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
     * 重新加载 {@code config.yml}, {@code tips.yml} 与语言文件, 校验公告并重启自动公告任务
     *
     * @return 重载后的有效运行配置
     */
    public PluginConfig reloadAnnouncements() {
        reloadConfig();
        reloadLanguage();
        currentConfig =
            PluginConfig.load(getConfig(), loadTipsConfiguration(), miniMessage);
        announcementService.applyRuntime(currentConfig, language);
        announcementScheduler.restart(currentConfig);
        logConfigState(currentConfig);
        return currentConfig;
    }

    /**
     * 返回当前已经校验过的运行配置
     *
     * @return 当前插件配置
     */
    public PluginConfig currentConfig() {
        return currentConfig;
    }

    /**
     * 返回命令和定时任务共用的公告发送服务
     *
     * @return 公告发送服务
     */
    public AnnouncementService announcementService() {
        return announcementService;
    }

    /**
     * 返回自动公告任务调度器
     *
     * @return 公告调度器
     */
    public AnnouncementScheduler announcementScheduler() {
        return announcementScheduler;
    }

    /**
     * 返回当前使用的 Paper/Folia 调度适配器
     *
     * @return 调度适配器
     */
    public SchedulerAdapter schedulerAdapter() {
        return schedulerAdapter;
    }

    /**
     * 返回当前语言文本目录
     *
     * @return 运行时语言文本目录
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

    private YamlConfiguration loadTipsConfiguration() {
        return YamlConfiguration.loadConfiguration(tipsFile());
    }

    private void saveDefaultTipsConfig() {
        File tipsFile = tipsFile();
        if (!tipsFile.exists()) {
            saveResource("tips.yml", false);
        }
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

    private File tipsFile() {
        return new File(getDataFolder(), "tips.yml");
    }
}
