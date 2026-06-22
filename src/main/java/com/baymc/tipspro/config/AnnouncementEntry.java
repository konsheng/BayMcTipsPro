package com.baymc.tipspro.config;

import net.kyori.adventure.text.Component;

/**
 * 表示一条已经通过校验的公告配置
 *
 * <p>原始 MiniMessage 文本会保留用于诊断, 解析后的组件和纯文本会被缓存, 避免定时广播时
 * 重复解析配置字符串
 *
 * @param index 配置列表中的一基序号
 * @param rawMessage 来自 {@code tips.yml} 的原始 MiniMessage 文本
 * @param component 可直接发送到聊天栏的 Adventure 组件
 * @param plainText 用于控制台日志的纯文本内容
 * @param sound 公告附带的可选声音配置, 没有声音时为 {@code null}
 */
public record AnnouncementEntry(
    int index,
    String rawMessage,
    Component component,
    String plainText,
    AnnouncementSound sound) {
}
