package com.baymc.tipspro.config;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Reads MiniMessage runtime text from lang/zh_CN.yml.
 *
 * <p>Command feedback, status labels, validation notices, and console logs all pass through this
 * catalog so visible wording can be adjusted without changing Java source.
 */
public final class LanguageCatalog {
    private final FileConfiguration config;

    /**
     * Creates a language catalog backed by a Bukkit YAML configuration.
     *
     * @param config loaded lang/zh_CN.yml configuration
     */
    public LanguageCatalog(FileConfiguration config) {
        this.config = config;
    }

    /**
     * Reads one language entry and applies placeholder replacements.
     *
     * @param path language key
     * @param placeholders replacement values
     * @return formatted message, or the key itself when missing
     */
    public String message(String path, Placeholder... placeholders) {
        return apply(raw(path), placeholders);
    }

    /**
     * Reads a language list and applies placeholder replacements to each line.
     *
     * @param path language list key
     * @param placeholders replacement values
     * @return formatted message list, or a one-line key marker when missing
     */
    public List<String> messages(String path, Placeholder... placeholders) {
        List<String> values = config.getStringList(path);
        if (values.isEmpty()) {
            return List.of(path);
        }
        List<String> formatted = new ArrayList<>(values.size());
        for (String value : values) {
            formatted.add(apply(value, placeholders));
        }
        return formatted;
    }

    /**
     * Formats an enabled or disabled state label.
     *
     * @param enabled state value
     * @return localized state label
     */
    public String enabledText(boolean enabled) {
        return message(enabled ? "words.enabled" : "words.disabled");
    }

    /**
     * Formats a running or stopped task state label.
     *
     * @param running task state
     * @return localized task state label
     */
    public String runningText(boolean running) {
        return message(running ? "words.running" : "words.stopped");
    }

    /**
     * Formats the scheduler mode used by status and startup logs.
     *
     * @param folia whether Folia scheduling is active
     * @return localized scheduler mode label
     */
    public String schedulerMode(boolean folia) {
        return message(folia ? "scheduler.folia" : "scheduler.bukkit");
    }

    /**
     * Formats an invalid announcement reason.
     *
     * @param invalidAnnouncement invalid announcement metadata
     * @return localized reason text
     */
    public String invalidReason(InvalidAnnouncement invalidAnnouncement) {
        return message(
            invalidAnnouncement.reasonKey(),
            placeholder("detail", invalidAnnouncement.detail()));
    }

    /**
     * Formats a non-fatal configuration notice.
     *
     * @param notice configuration notice metadata
     * @return localized notice text
     */
    public String notice(ConfigNotice notice) {
        return message(
            notice.messageKey(),
            placeholder("path", notice.path()),
            placeholder("value", notice.value()),
            placeholder("minimum", notice.minimum()));
    }

    /**
     * Creates a placeholder replacement.
     *
     * @param key placeholder name without surrounding percent signs
     * @param value replacement value
     * @return placeholder descriptor
     */
    public static Placeholder placeholder(String key, Object value) {
        return new Placeholder("%" + key + "%", String.valueOf(value));
    }

    private String raw(String path) {
        String value = config.getString(path);
        return value == null || value.isBlank() ? path : value;
    }

    private static String apply(String value, Placeholder... placeholders) {
        String formatted = value;
        for (Placeholder placeholder : placeholders) {
            formatted = formatted.replace(placeholder.token(), placeholder.value());
        }
        return formatted;
    }

    /**
     * One resolved placeholder replacement pair.
     *
     * @param token token including percent signs
     * @param value replacement value
     */
    public record Placeholder(String token, String value) {
    }
}
