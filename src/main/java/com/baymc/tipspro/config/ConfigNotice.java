package com.baymc.tipspro.config;

/**
 * Describes a non-fatal configuration notice discovered while loading config.yml.
 *
 * <p>The notice stores a language key plus structured values instead of ready-made text, allowing
 * runtime wording to stay in lang/zh_CN.yml.
 *
 * @param messageKey language key used to format the notice
 * @param path configuration path related to the notice
 * @param value actual configured value, when relevant
 * @param minimum normalized minimum value, when relevant
 */
public record ConfigNotice(
    String messageKey,
    String path,
    String value,
    String minimum) {
}
