package com.baymc.tipspro.config;

/**
 * 描述加载 {@code config.yml} 时发现的非致命配置提示
 *
 * <p>提示只保存语言键和结构化数值, 不直接保存完整文本, 从而让运行时文案继续集中在
 * {@code lang/zh_CN.yml} 中
 *
 * @param messageKey 用于格式化提示的语言键
 * @param path 与提示相关的配置路径
 * @param value 实际配置值
 * @param minimum 归一化后的最小值
 */
public record ConfigNotice(
    String messageKey,
    String path,
    String value,
    String minimum) {
}
