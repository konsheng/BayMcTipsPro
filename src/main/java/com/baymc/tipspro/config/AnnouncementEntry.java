package com.baymc.tipspro.config;

import net.kyori.adventure.text.Component;

/**
 * Represents one validated announcement from the configuration file.
 *
 * <p>The raw MiniMessage text is kept for diagnostics, while the parsed component and plain text
 * are cached so scheduled broadcasts never need to re-parse configuration strings.
 *
 * @param index one-based index in the configured message list
 * @param rawMessage original MiniMessage text from config.yml
 * @param component parsed Adventure component ready for chat delivery
 * @param plainText plain-text form used for console logging
 */
public record AnnouncementEntry(
    int index,
    String rawMessage,
    Component component,
    String plainText) {
}
