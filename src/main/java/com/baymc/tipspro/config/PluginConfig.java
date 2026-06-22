package com.baymc.tipspro.config;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Immutable runtime view of BayMcTipsPro's configuration.
 *
 * <p>The loader performs all normalization and MiniMessage validation up front. Services can then
 * use this value object without touching Bukkit configuration APIs or handling parse failures.
 *
 * @param enabled whether the automatic scheduled task is enabled
 * @param intervalSeconds normalized automatic announcement interval in seconds
 * @param initialDelaySeconds normalized startup or reload delay in seconds
 * @param sendToConsole whether sent announcements should be logged as plain text
 * @param validAnnouncements validated announcements ready for broadcast
 * @param invalidAnnouncements rejected announcement entries
 * @param notices non-fatal normalization notices gathered while loading
 */
public record PluginConfig(
    boolean enabled,
    int intervalSeconds,
    int initialDelaySeconds,
    boolean sendToConsole,
    List<AnnouncementEntry> validAnnouncements,
    List<InvalidAnnouncement> invalidAnnouncements,
    List<ConfigNotice> notices) {

    public static final int DEFAULT_INTERVAL_SECONDS = 300;
    public static final int DEFAULT_INITIAL_DELAY_SECONDS = 30;
    public static final int MIN_INTERVAL_SECONDS = 5;
    public static final int MIN_INITIAL_DELAY_SECONDS = 1;

    private static final PlainTextComponentSerializer PLAIN_TEXT =
        PlainTextComponentSerializer.plainText();

    /**
     * Copies collections defensively so consumers cannot mutate live runtime state.
     */
    public PluginConfig {
        validAnnouncements = List.copyOf(validAnnouncements);
        invalidAnnouncements = List.copyOf(invalidAnnouncements);
        notices = List.copyOf(notices);
    }

    /**
     * Loads and validates plugin settings from Bukkit's configuration object.
     *
     * @param configuration Bukkit configuration loaded from config.yml
     * @param miniMessage MiniMessage parser used for announcement validation
     * @return immutable validated runtime configuration
     */
    public static PluginConfig load(FileConfiguration configuration, MiniMessage miniMessage) {
        List<ConfigNotice> notices = new ArrayList<>();
        boolean enabled = configuration.getBoolean("announcements.enabled", true);
        int intervalSeconds = normalizeSeconds(
            configuration.getInt("announcements.interval-seconds", DEFAULT_INTERVAL_SECONDS),
            MIN_INTERVAL_SECONDS,
            "announcements.interval-seconds",
            notices);
        int initialDelaySeconds = normalizeSeconds(
            configuration.getInt(
                "announcements.initial-delay-seconds",
                DEFAULT_INITIAL_DELAY_SECONDS),
            MIN_INITIAL_DELAY_SECONDS,
            "announcements.initial-delay-seconds",
            notices);
        boolean sendToConsole =
            configuration.getBoolean("announcements.send-to-console", true);

        List<AnnouncementEntry> validAnnouncements = new ArrayList<>();
        List<InvalidAnnouncement> invalidAnnouncements = new ArrayList<>();
        List<String> messages = configuration.getStringList("announcements.messages");
        for (int i = 0; i < messages.size(); i++) {
            int index = i + 1;
            String rawMessage = messages.get(i);
            if (rawMessage == null || rawMessage.isBlank()) {
                invalidAnnouncements.add(
                    new InvalidAnnouncement(index, "", "validation.blank-message", ""));
                continue;
            }
            try {
                Component component = miniMessage.deserialize(rawMessage);
                validAnnouncements.add(
                    new AnnouncementEntry(
                        index,
                        rawMessage,
                        component,
                        PLAIN_TEXT.serialize(component)));
            } catch (RuntimeException exception) {
                invalidAnnouncements.add(
                    new InvalidAnnouncement(
                        index,
                        rawMessage,
                        "validation.parse-error",
                        messageOf(exception)));
            }
        }

        if (messages.isEmpty()) {
            notices.add(
                new ConfigNotice(
                    "validation.messages-empty",
                    "announcements.messages",
                    "",
                    ""));
        }

        return new PluginConfig(
            enabled,
            intervalSeconds,
            initialDelaySeconds,
            sendToConsole,
            validAnnouncements,
            invalidAnnouncements,
            notices);
    }

    /**
     * Returns whether at least one announcement can be sent.
     *
     * @return true when the validated announcement list is not empty
     */
    public boolean hasValidAnnouncements() {
        return !validAnnouncements.isEmpty();
    }

    /**
     * Returns the number of valid announcements.
     *
     * @return validated announcement count
     */
    public int validAnnouncementCount() {
        return validAnnouncements.size();
    }

    /**
     * Returns the number of invalid announcements.
     *
     * @return rejected announcement count
     */
    public int invalidAnnouncementCount() {
        return invalidAnnouncements.size();
    }

    private static int normalizeSeconds(
        int value,
        int minimum,
        String path,
        List<ConfigNotice> notices) {
        if (value >= minimum) {
            return value;
        }
        notices.add(
            new ConfigNotice(
                "validation.minimum-raised",
                path,
                String.valueOf(value),
                String.valueOf(minimum)));
        return minimum;
    }

    private static String messageOf(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return message;
    }
}
