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
    public static final String HELP_PERMISSION = "baymctipspro.help";
    public static final String INFO_PERMISSION = "baymctipspro.info";
    public static final String NEXT_PERMISSION = "baymctipspro.next";
    public static final String STATUS_PERMISSION = "baymctipspro.status";
    public static final String RELOAD_PERMISSION = "baymctipspro.reload";

    private static final List<SubcommandPermission> SUBCOMMANDS =
        List.of(
            new SubcommandPermission("help", HELP_PERMISSION),
            new SubcommandPermission("info", INFO_PERMISSION),
            new SubcommandPermission("next", NEXT_PERMISSION),
            new SubcommandPermission("status", STATUS_PERMISSION),
            new SubcommandPermission("reload", RELOAD_PERMISSION));

    private final BayMcTipsProPlugin plugin;
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

    private void showHelp(CommandSender sender) {
        sendLines(sender, "messages.help.lines");
    }

    private void showInfo(CommandSender sender) {
        sendLines(
            sender,
            "messages.info.lines",
            placeholder("version", plugin.getPluginMeta().getVersion()));
    }

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

    private String permissionFor(String subcommand) {
        for (SubcommandPermission entry : SUBCOMMANDS) {
            if (entry.name().equals(subcommand)) {
                return entry.permission();
            }
        }
        return null;
    }

    private boolean lacksCommandPermission(CommandSender sender, String permission) {
        return sender instanceof Player && !sender.hasPermission(permission);
    }

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

    private void send(
        CommandSender sender,
        String path,
        LanguageCatalog.Placeholder... placeholders) {
        sender.sendMessage(miniMessage.deserialize(plugin.language().message(path, placeholders)));
    }

    private void sendLines(
        CommandSender sender,
        String path,
        LanguageCatalog.Placeholder... placeholders) {
        for (String message : plugin.language().messages(path, placeholders)) {
            sender.sendMessage(miniMessage.deserialize(message));
        }
    }

    private record SubcommandPermission(String name, String permission) {
    }
}
