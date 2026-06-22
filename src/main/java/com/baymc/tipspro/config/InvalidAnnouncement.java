package com.baymc.tipspro.config;

/**
 * Describes an announcement entry that could not be accepted during configuration loading.
 *
 * <p>Invalid entries are skipped instead of failing the whole plugin, which keeps reload safe
 * even when an administrator is testing MiniMessage syntax.
 *
 * @param index one-based index in the configured message list
 * @param rawMessage original text from config.yml
 * @param reason human-readable reason for rejecting the entry
 */
public record InvalidAnnouncement(
    int index,
    String rawMessage,
    String reason) {
}
