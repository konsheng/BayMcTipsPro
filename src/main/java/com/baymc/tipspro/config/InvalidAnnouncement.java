package com.baymc.tipspro.config;

/**
 * 描述加载配置时无法接受的公告条目
 *
 * <p>无效条目会被跳过, 而不是让整个插件加载失败, 管理员测试 MiniMessage 语法时
 * 重载流程仍然安全, 失败原因保存为语言键, 具体提示文本仍由 {@code lang/zh_CN.yml} 提供
 *
 * @param index 配置列表中的一基序号
 * @param rawMessage 来自 {@code tips.yml} 的原始文本
 * @param reasonKey 拒绝该条目的原因语言键
 * @param detail 用于诊断的可选解析细节
 */
public record InvalidAnnouncement(
    int index,
    String rawMessage,
    String reasonKey,
    String detail) {
}
