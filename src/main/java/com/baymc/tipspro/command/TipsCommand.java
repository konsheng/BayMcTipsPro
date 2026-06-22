package com.baymc.tipspro.command;

import com.baymc.tipspro.BayMcTipsProPlugin;
import com.baymc.tipspro.config.PluginConfig;
import com.baymc.tipspro.service.AnnouncementBroadcastResult;
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
import org.jetbrains.annotations.Nullable;

/**
 * Handles the /baymctipspro command and its /tips alias.
 *
 * <p>The subcommand order is intentionally fixed as help, info, next, status, reload so command
 * help, tab completion, and implementation dispatch all match the agreed user-facing order.
 */
public final class TipsCommand implements CommandExecutor, TabCompleter {
    public static final String PERMISSION = "baymctipspro.command";

    private static final List<String> SUBCOMMANDS =
        List.of("help", "info", "next", "status", "reload");

    private final BayMcTipsProPlugin plugin;
    private final MiniMessage miniMessage;

    /**
     * Creates the command handler.
     *
     * @param plugin plugin facade used to access runtime services
     * @param miniMessage parser used for command feedback components
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
        @NotNull String[] args) {
        if (!hasCommandPermission(sender)) {
            send(sender, "<red>你没有权限使用 BayMcTipsPro 命令。</red>");
            return true;
        }

        String subcommand = args.length == 0 ? "info" : args[0].toLowerCase(Locale.ROOT);
        return switch (subcommand) {
            case "help" -> {
                showHelp(sender);
                yield true;
            }
            case "info" -> {
                showInfo(sender);
                yield true;
            }
            case "next" -> {
                sendNext(sender);
                yield true;
            }
            case "status" -> {
                showStatus(sender);
                yield true;
            }
            case "reload" -> {
                reload(sender);
                yield true;
            }
            default -> {
                send(sender, "<red>未知命令。</red> <gray>使用 <yellow>/tips help</yellow> 查看帮助。</gray>");
                yield true;
            }
        };
    }

    @Override
    public @Nullable List<String> onTabComplete(
        @NotNull CommandSender sender,
        @NotNull Command command,
        @NotNull String alias,
        @NotNull String[] args) {
        if (!hasCommandPermission(sender) || args.length != 1) {
            return List.of();
        }
        String prefix = args[0].toLowerCase(Locale.ROOT);
        List<String> completions = new ArrayList<>();
        for (String subcommand : SUBCOMMANDS) {
            if (subcommand.startsWith(prefix)) {
                completions.add(subcommand);
            }
        }
        return completions;
    }

    private void showHelp(CommandSender sender) {
        send(sender, "<gold>BayMcTipsPro 命令帮助</gold>");
        send(sender, "<yellow>/tips</yellow> <gray>- 查看插件信息</gray>");
        send(sender, "<yellow>/tips help</yellow> <gray>- 查看命令帮助</gray>");
        send(sender, "<yellow>/tips info</yellow> <gray>- 查看插件信息</gray>");
        send(sender, "<yellow>/tips next</yellow> <gray>- 立即发送一条随机公告</gray>");
        send(sender, "<yellow>/tips status</yellow> <gray>- 查看公告运行状态</gray>");
        send(sender, "<yellow>/tips reload</yellow> <gray>- 重载公告配置</gray>");
    }

    private void showInfo(CommandSender sender) {
        send(sender, "<gold>BayMcTipsPro</gold> <gray>v" + plugin.getPluginMeta().getVersion() + "</gray>");
        send(sender, "<gray>轻量级 MiniMessage 聊天栏公告插件</gray>");
        send(sender, "<gray>使用 <yellow>/tips help</yellow> 查看命令帮助。</gray>");
    }

    private void sendNext(CommandSender sender) {
        AnnouncementBroadcastResult result =
            plugin.announcementService().broadcastRandomAnnouncement();
        if (!result.sent()) {
            send(sender, "<red>没有可用公告，无法发送。</red>");
            return;
        }
        send(
            sender,
            "<green>已发送一条随机公告。</green> <gray>在线玩家: "
                + result.onlinePlayers()
                + "</gray>");
    }

    private void showStatus(CommandSender sender) {
        PluginConfig config = plugin.currentConfig();
        send(sender, "<gold>BayMcTipsPro 运行状态</gold>");
        send(sender, "<gray>公告功能: " + enabledText(config.enabled()) + "</gray>");
        send(sender, "<gray>任务状态: " + runningText(plugin.announcementScheduler().isRunning()) + "</gray>");
        send(sender, "<gray>有效公告数量: " + config.validAnnouncementCount() + "</gray>");
        send(sender, "<gray>无效公告数量: " + config.invalidAnnouncementCount() + "</gray>");
        send(sender, "<gray>公告间隔: " + config.intervalSeconds() + " 秒</gray>");
        send(sender, "<gray>首次延迟: " + config.initialDelaySeconds() + " 秒</gray>");
        send(sender, "<gray>控制台同步: " + enabledText(config.sendToConsole()) + "</gray>");
        send(sender, "<gray>调度模式: " + plugin.schedulerAdapter().modeName() + "</gray>");
    }

    private void reload(CommandSender sender) {
        PluginConfig config = plugin.reloadAnnouncements();
        send(sender, "<green>BayMcTipsPro 配置已重载。</green>");
        send(
            sender,
            "<gray>有效公告: "
                + config.validAnnouncementCount()
                + "，无效公告: "
                + config.invalidAnnouncementCount()
                + "，任务状态: "
                + runningText(plugin.announcementScheduler().isRunning())
                + "</gray>");
    }

    private boolean hasCommandPermission(CommandSender sender) {
        return !(sender instanceof Player) || sender.hasPermission(PERMISSION);
    }

    private void send(CommandSender sender, String message) {
        sender.sendMessage(miniMessage.deserialize(message));
    }

    private static String enabledText(boolean enabled) {
        return enabled ? "已启用" : "已禁用";
    }

    private static String runningText(boolean running) {
        return running ? "运行中" : "未运行";
    }
}
