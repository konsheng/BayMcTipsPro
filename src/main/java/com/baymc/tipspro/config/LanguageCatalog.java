package com.baymc.tipspro.config;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * 从配置选择的语言文件读取 MiniMessage 运行时文本
 *
 * <p>命令反馈, 状态标签, 校验提示和控制台日志都会经过这个目录读取, 让可见文案可以在不修改
 * Java 源码的情况下调整
 */
public final class LanguageCatalog {
    private final FileConfiguration config;

    /**
     * 创建基于 Bukkit YAML 配置的语言文本目录
     *
     * @param config 已加载的语言文件配置
     */
    public LanguageCatalog(FileConfiguration config) {
        this.config = config;
    }

    /**
     * 读取一条语言文本, 并执行占位符替换
     *
     * @param path 语言键
     * @param placeholders 替换值
     * @return 格式化后的文本, 缺失时返回语言键本身
     */
    public String message(String path, Placeholder... placeholders) {
        return apply(raw(path), placeholders);
    }

    /**
     * 读取语言文本列表, 并对每一行执行占位符替换
     *
     * @param path 语言列表键
     * @param placeholders 替换值
     * @return 格式化后的文本列表, 缺失时返回只包含语言键的一行列表
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
     * 格式化启用或禁用状态标签
     *
     * @param enabled 状态值
     * @return 本地化后的状态标签
     */
    public String enabledText(boolean enabled) {
        return message(enabled ? "words.enabled" : "words.disabled");
    }

    /**
     * 格式化运行中或未运行任务状态标签
     *
     * @param running 任务状态
     * @return 本地化后的任务状态标签
     */
    public String runningText(boolean running) {
        return message(running ? "words.running" : "words.stopped");
    }

    /**
     * 格式化状态命令和启动日志使用的调度模式
     *
     * @param folia 是否正在使用 Folia 调度
     * @return 本地化后的调度模式标签
     */
    public String schedulerMode(boolean folia) {
        return message(folia ? "scheduler.folia" : "scheduler.bukkit");
    }

    /**
     * 格式化无效公告原因
     *
     * @param invalidAnnouncement 无效公告元数据
     * @return 本地化后的原因文本
     */
    public String invalidReason(InvalidAnnouncement invalidAnnouncement) {
        return message(
            invalidAnnouncement.reasonKey(),
            placeholder("detail", invalidAnnouncement.detail()));
    }

    /**
     * 格式化非致命配置提示
     *
     * @param notice 配置提示元数据
     * @return 本地化后的提示文本
     */
    public String notice(ConfigNotice notice) {
        return message(
            notice.messageKey(),
            placeholder("path", notice.path()),
            placeholder("value", notice.value()),
            placeholder("minimum", notice.minimum()));
    }

    /**
     * 创建占位符替换项
     *
     * @param key 不包含百分号的占位符名称
     * @param value 替换值
     * @return 占位符描述
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
     * 一组已经解析的占位符替换项
     *
     * @param token 包含百分号的占位符
     * @param value 替换值
     */
    public record Placeholder(String token, String value) {
    }
}
