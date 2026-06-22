package com.baymc.tipspro.command;

import com.baymc.tipspro.BayMcTipsProPlugin;
import com.baymc.tipspro.config.LanguageCatalog;
import com.baymc.tipspro.config.PluginConfig;
import com.baymc.tipspro.service.AnnouncementBroadcastResult;
import static com.baymc.tipspro.config.LanguageCatalog.placeholder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * 处理 {@code /baymctipspro} 命令及 {@code /tips} 别名
 *
 * <p>子命令顺序固定为 help, info, next, status, reload, 用于保证帮助文本, Tab 补全
 * 和实际分发顺序与约定的用户可见顺序一致
 */
public final class TipsCommand implements CommandExecutor, TabCompleter {
    /**
     * 查看帮助子命令需要的权限节点
     */
    public static final String HELP_PERMISSION = "baymctipspro.help";

    /**
     * 查看插件信息子命令需要的权限节点
     */
    public static final String INFO_PERMISSION = "baymctipspro.info";

    /**
     * 立即发送随机公告子命令需要的权限节点
     */
    public static final String NEXT_PERMISSION = "baymctipspro.next";

    /**
     * 查看运行状态子命令需要的权限节点
     */
    public static final String STATUS_PERMISSION = "baymctipspro.status";

    /**
     * 重载配置和语言文件子命令需要的权限节点
     */
    public static final String RELOAD_PERMISSION = "baymctipspro.reload";

    /**
     * 主流帮助顺序下的子命令和权限节点映射列表
     */
    private static final List<SubcommandPermission> SUBCOMMANDS =
        List.of(
            new SubcommandPermission("help", HELP_PERMISSION),
            new SubcommandPermission("info", INFO_PERMISSION),
            new SubcommandPermission("next", NEXT_PERMISSION),
            new SubcommandPermission("status", STATUS_PERMISSION),
            new SubcommandPermission("reload", RELOAD_PERMISSION));

    /**
     * 提供配置, 语言, 调度和广播服务的插件入口
     */
    private final BayMcTipsProPlugin plugin;

    /**
     * 解析命令反馈文本的 MiniMessage 实例
     */
    private final MiniMessage miniMessage;

    /**
     * 创建命令处理器
     *
     * @param plugin 用于访问运行时服务的插件实例
     * @param miniMessage 用于解析命令反馈文本的 MiniMessage 解析器
     */
    public TipsCommand(BayMcTipsProPlugin plugin, MiniMessage miniMessage) {
        this.plugin = plugin;
        this.miniMessage = miniMessage;
    }

    /**
     * 处理主命令和子命令分发
     *
     * @param sender 命令发送者
     * @param command Bukkit 命令对象
     * @param label 玩家实际输入的命令标签
     * @param args 命令参数
     * @return 命令已经被插件处理时返回 {@code true}
     */
    @Override
    public boolean onCommand(
        @NotNull CommandSender sender,
        @NotNull Command command,
        @NotNull String label,
        @NotNull String @NotNull [] args) {
        if (lacksCommandPermission(sender)) {
            send(sender, "messages.no-permission");
            return true;
        }

        String subcommand = args.length == 0 ? "info" : args[0].toLowerCase(Locale.ROOT);
        String permission = permissionFor(subcommand);
        if (permission == null) {
            send(sender, "messages.unknown-command");
            return true;
        }
        if (lacksCommandPermission(sender, permission)) {
            send(sender, "messages.no-permission");
            return true;
        }

        switch (subcommand) {
            case "help" ->
                showHelp(sender);
            case "info" ->
                showInfo(sender);
            case "next" ->
                sendNext(sender);
            case "status" ->
                showStatus(sender);
            case "reload" ->
                reload(sender);
            default ->
                send(sender, "messages.unknown-command");
        }
        return true;
    }

    /**
     * 返回发送者有权限查看的一级子命令补全列表
     *
     * @param sender 命令发送者
     * @param command Bukkit 命令对象
     * @param alias 玩家实际输入的命令别名
     * @param args 当前补全参数
     * @return 符合前缀和权限条件的子命令列表
     */
    @Override
    public @NotNull List<String> onTabComplete(
        @NotNull CommandSender sender,
        @NotNull Command command,
        @NotNull String alias,
        @NotNull String @NotNull [] args) {
        if (lacksCommandPermission(sender) || args.length != 1) {
            return List.of();
        }
        String prefix = args[0].toLowerCase(Locale.ROOT);
        List<String> completions = new ArrayList<>();
        for (SubcommandPermission subcommand : SUBCOMMANDS) {
            if (subcommand.name().startsWith(prefix)
                && !lacksCommandPermission(sender, subcommand.permission())) {
                completions.add(subcommand.name());
            }
        }
        return completions;
    }

    /**
     * 向发送者显示帮助文本
     *
     * @param sender 命令发送者
     */
    private void showHelp(CommandSender sender) {
        sendLines(sender, "messages.help.lines");
    }

    /**
     * 向发送者显示插件信息
     *
     * @param sender 命令发送者
     */
    private void showInfo(CommandSender sender) {
        sendLines(
            sender,
            "messages.info.lines",
            placeholder("version", plugin.getPluginMeta().getVersion()));
    }

    /**
     * 立即广播一条随机公告
     *
     * @param sender 命令发送者
     */
    private void sendNext(CommandSender sender) {
        AnnouncementBroadcastResult result =
            plugin.announcementService().broadcastRandomAnnouncement();
        if (!result.sent()) {
            send(sender, "messages.next.no-announcement");
            return;
        }
        send(
            sender,
            "messages.next.success",
            placeholder("online", result.onlinePlayers()));
    }

    /**
     * 向发送者显示当前公告运行状态
     *
     * @param sender 命令发送者
     */
    private void showStatus(CommandSender sender) {
        PluginConfig config = plugin.currentConfig();
        LanguageCatalog language = plugin.language();
        sendLines(
            sender,
            "messages.status.lines",
            placeholder("announcements", language.enabledText(config.enabled())),
            placeholder("task", language.runningText(plugin.announcementScheduler().isRunning())),
            placeholder("valid", config.validAnnouncementCount()),
            placeholder("invalid", config.invalidAnnouncementCount()),
            placeholder("interval", config.intervalSeconds()),
            placeholder("initial_delay", config.initialDelaySeconds()),
            placeholder("console", language.enabledText(config.sendToConsole())),
            placeholder("scheduler", language.schedulerMode(plugin.schedulerAdapter().isFolia())));
    }

    /**
     * 重载插件配置, 公告文件和语言文件
     *
     * @param sender 命令发送者
     */
    private void reload(CommandSender sender) {
        PluginConfig config = plugin.reloadAnnouncements();
        send(sender, "messages.reload.success");
        send(
            sender,
            "messages.reload.summary",
            placeholder("valid", config.validAnnouncementCount()),
            placeholder("invalid", config.invalidAnnouncementCount()),
            placeholder("task", plugin.language().runningText(plugin.announcementScheduler().isRunning())));
    }

    /**
     * 查找子命令对应的权限节点
     *
     * @param subcommand 已归一化的小写子命令
     * @return 子命令权限节点, 未知子命令返回 {@code null}
     */
    private String permissionFor(String subcommand) {
        for (SubcommandPermission entry : SUBCOMMANDS) {
            if (entry.name().equals(subcommand)) {
                return entry.permission();
            }
        }
        return null;
    }

    /**
     * 判断发送者是否缺少指定权限
     *
     * @param sender 命令发送者
     * @param permission 需要检查的权限节点
     * @return 玩家缺少权限时返回 {@code true}, 控制台始终返回 {@code false}
     */
    private boolean lacksCommandPermission(CommandSender sender, String permission) {
        return sender instanceof Player && !sender.hasPermission(permission);
    }

    /**
     * 判断发送者是否没有任何 BayMcTipsPro 子命令权限
     *
     * @param sender 命令发送者
     * @return 玩家没有任何子命令权限时返回 {@code true}, 控制台始终返回 {@code false}
     */
    private boolean lacksCommandPermission(CommandSender sender) {
        if (!(sender instanceof Player)) {
            return false;
        }
        for (SubcommandPermission subcommand : SUBCOMMANDS) {
            if (sender.hasPermission(subcommand.permission())) {
                return false;
            }
        }
        return true;
    }

    /**
     * 发送单行语言文本
     *
     * @param sender 命令发送者
     * @param path 语言键
     * @param placeholders 占位符替换值
     */
    private void send(
        CommandSender sender,
        String path,
        LanguageCatalog.Placeholder... placeholders) {
        sender.sendMessage(miniMessage.deserialize(plugin.language().message(path, placeholders)));
    }

    /**
     * 发送多行语言文本
     *
     * @param sender 命令发送者
     * @param path 语言列表键
     * @param placeholders 占位符替换值
     */
    private void sendLines(
        CommandSender sender,
        String path,
        LanguageCatalog.Placeholder... placeholders) {
        for (String message : plugin.language().messages(path, placeholders)) {
            sender.sendMessage(miniMessage.deserialize(message));
        }
    }

    /**
     * 子命令名称和权限节点的固定映射
     *
     * @param name 子命令名称
     * @param permission 子命令权限节点
     */
    private record SubcommandPermission(String name, String permission) {
    }
}
