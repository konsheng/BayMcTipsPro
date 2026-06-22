package com.baymc.tipspro.config;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * BayMcTipsPro 配置在运行时使用的不可变视图
 *
 * <p>加载器会提前完成数值归一化和 MiniMessage 校验, 后续服务只需要使用这个值对象
 * 不再直接接触 Bukkit 配置 API, 也不需要处理解析失败
 *
 * @param enabled 是否启用自动公告任务
 * @param intervalSeconds 归一化后的自动公告间隔, 单位为秒
 * @param initialDelaySeconds 归一化后的启动或重载后首次延迟, 单位为秒
 * @param sendToConsole 是否将已发送公告以纯文本形式写入控制台
 * @param validAnnouncements 已通过校验, 可以广播的公告
 * @param invalidAnnouncements 被拒绝的公告条目
 * @param notices 加载时收集到的非致命归一化提示
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
     * 防御性复制集合, 避免调用方修改运行时状态
     */
    public PluginConfig {
        validAnnouncements = List.copyOf(validAnnouncements);
        invalidAnnouncements = List.copyOf(invalidAnnouncements);
        notices = List.copyOf(notices);
    }

    /**
     * 从 Bukkit 配置对象中加载并校验插件设置
     *
     * @param configuration 从 {@code config.yml} 加载的 Bukkit 配置
     * @param miniMessage 用于校验公告文本的 MiniMessage 解析器
     * @return 已校验的不可变运行配置
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
     * 返回是否至少存在一条可以发送的公告
     *
     * @return 已校验公告列表非空时返回 {@code true}
     */
    public boolean hasValidAnnouncements() {
        return !validAnnouncements.isEmpty();
    }

    /**
     * 返回有效公告数量
     *
     * @return 已通过校验的公告数量
     */
    public int validAnnouncementCount() {
        return validAnnouncements.size();
    }

    /**
     * 返回无效公告数量
     *
     * @return 被拒绝的公告数量
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
