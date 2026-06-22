package com.baymc.tipspro.config;

/**
 * Describes an announcement entry that could not be accepted during configuration loading.
 *
 * <p>Invalid entries are skipped instead of failing the whole plugin, which keeps reload safe
 * even when an administrator is testing MiniMessage syntax. The reason is stored as a language
 * key so visible validation text remains in lang/zh_CN.yml.
 *
 * @param index one-based index in the configured message list
 * @param rawMessage original text from config.yml
 * @param reasonKey language key for the rejection reason
 * @param detail optional parser detail for diagnostics
 */
public record InvalidAnnouncement(
    int index,
    String rawMessage,
    String reasonKey,
    String detail) {
}
