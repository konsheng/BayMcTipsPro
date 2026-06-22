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
            send(sender, "messages.no-permission");
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
                send(sender, "messages.unknown-command");
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

    private boolean hasCommandPermission(CommandSender sender) {
        return !(sender instanceof Player) || sender.hasPermission(PERMISSION);
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
}
