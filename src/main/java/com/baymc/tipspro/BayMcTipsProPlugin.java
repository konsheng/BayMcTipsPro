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
    /**
     * 配置未指定有效语言文件时使用的默认文件名
     */
    private static final String DEFAULT_LANGUAGE_FILE = "zh_CN.yml";

    /**
     * 插件数据目录下保存语言文件的目录名
     */
    private static final String LANGUAGE_DIRECTORY = "lang";

    /**
     * 解析公告和命令反馈文本的 MiniMessage 实例
     */
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    /**
     * 当前运行时使用的 Paper/Folia 调度适配器
     */
    private SchedulerAdapter schedulerAdapter;

    /**
     * 当前运行时使用的公告广播服务
     */
    private AnnouncementService announcementService;

    /**
     * 当前运行时使用的自动公告任务管理器
     */
    private AnnouncementScheduler announcementScheduler;

    /**
     * 当前已经加载和校验的配置快照
     */
    private PluginConfig currentConfig;

    /**
     * 当前已经加载的语言文本目录
     */
    private LanguageCatalog language;

    /**
     * 创建插件主类实例
     *
     * <p>Bukkit 在加载插件时通过无参构造器实例化主类
     */
    public BayMcTipsProPlugin() {
    }

    /**
     * 初始化默认资源, 运行时服务, 命令和自动公告任务
     */
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

    /**
     * 停止自动公告任务并输出插件关闭日志
     */
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

    /**
     * 注册主命令执行器和 Tab 补全器
     */
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

    /**
     * 输出配置归一化提示, 无效公告提示和自动公告任务状态
     *
     * @param config 已加载并校验的运行配置
     */
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
        if (config.hasNoValidAnnouncements()) {
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

    /**
     * 按当前配置选择语言文件并重建语言文本目录
     */
    private void reloadLanguage() {
        language = new LanguageCatalog(YamlConfiguration.loadConfiguration(languageFile()));
    }

    /**
     * 加载独立公告文件
     *
     * @return 从 {@code tips.yml} 读取的 Bukkit YAML 配置
     */
    private YamlConfiguration loadTipsConfiguration() {
        return YamlConfiguration.loadConfiguration(tipsFile());
    }

    /**
     * 在数据目录不存在公告文件时保存默认公告文件
     */
    private void saveDefaultTipsConfig() {
        File tipsFile = tipsFile();
        if (!tipsFile.exists()) {
            saveResource("tips.yml", false);
        }
    }

    /**
     * 在语言目录不存在默认语言文件时保存默认语言文件
     */
    private void saveDefaultLanguageConfig() {
        File defaultLanguageFile = new File(languageDirectory(), DEFAULT_LANGUAGE_FILE);
        if (!defaultLanguageFile.exists()) {
            saveResource(LANGUAGE_DIRECTORY + "/" + DEFAULT_LANGUAGE_FILE, false);
        }
    }

    /**
     * 返回当前应使用的语言文件, 配置无效或文件不存在时回退到默认语言文件
     *
     * @return 可读取的语言文件
     */
    private File languageFile() {
        File selectedFile = new File(languageDirectory(), configuredLanguageFile());
        if (selectedFile.isFile()) {
            return selectedFile;
        }
        return new File(languageDirectory(), DEFAULT_LANGUAGE_FILE);
    }

    /**
     * 返回公告文件路径
     *
     * @return 插件数据目录下的 {@code tips.yml}
     */
    private File tipsFile() {
        return new File(getDataFolder(), "tips.yml");
    }

    /**
     * 返回语言文件目录路径
     *
     * @return 插件数据目录下的语言目录
     */
    private File languageDirectory() {
        return new File(getDataFolder(), LANGUAGE_DIRECTORY);
    }

    /**
     * 读取并校验配置中的语言文件名
     *
     * @return 安全的语言文件名
     */
    private String configuredLanguageFile() {
        String configured = getConfig().getString("language.file", DEFAULT_LANGUAGE_FILE);
        String fileName = configured.trim();
        if (fileName.isBlank()
            || fileName.contains("/")
            || fileName.contains("\\")
            || fileName.contains("..")
            || !fileName.endsWith(".yml")) {
            return DEFAULT_LANGUAGE_FILE;
        }
        return fileName;
    }
}
