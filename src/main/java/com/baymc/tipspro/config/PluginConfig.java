package com.baymc.tipspro.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.sound.Sound;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * BayMcTipsPro 配置在运行时使用的不可变视图
 *
 * <p>加载器会从 {@code config.yml} 读取调度配置, 从 {@code tips.yml} 读取公告列表
 * 并提前完成数值归一化和 MiniMessage 校验, 后续服务只需要使用这个值对象
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

    /**
     * 默认自动公告间隔秒数
     */
    public static final int DEFAULT_INTERVAL_SECONDS = 300;

    /**
     * 默认首次公告延迟秒数
     */
    public static final int DEFAULT_INITIAL_DELAY_SECONDS = 30;

    /**
     * 自动公告间隔允许的最小秒数
     */
    public static final int MIN_INTERVAL_SECONDS = 5;

    /**
     * 首次公告延迟允许的最小秒数
     */
    public static final int MIN_INITIAL_DELAY_SECONDS = 1;

    /**
     * 默认声音分类
     */
    private static final Sound.Source DEFAULT_SOUND_SOURCE = Sound.Source.MASTER;

    /**
     * 默认声音音量
     */
    private static final float DEFAULT_SOUND_VOLUME = 1.0F;

    /**
     * 默认声音音调
     */
    private static final float DEFAULT_SOUND_PITCH = 1.0F;

    /**
     * 将 Adventure 组件转换为控制台纯文本的序列化器
     */
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
     * @param tipsConfiguration 从 {@code tips.yml} 加载的 Bukkit 配置
     * @param miniMessage 用于校验公告文本的 MiniMessage 解析器
     * @return 已校验的不可变运行配置
     */
    public static PluginConfig load(
        FileConfiguration configuration,
        FileConfiguration tipsConfiguration,
        MiniMessage miniMessage) {
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
        List<?> entries = tipsConfiguration.getList("tips", List.of());
        for (int i = 0; i < entries.size(); i++) {
            int index = i + 1;
            AnnouncementSource source = announcementSource(index, entries.get(i));
            if (source.invalidAnnouncement() != null) {
                invalidAnnouncements.add(source.invalidAnnouncement());
                continue;
            }
            if (source.message().isBlank()) {
                invalidAnnouncements.add(
                    new InvalidAnnouncement(index, "", "validation.blank-message", ""));
                continue;
            }
            SoundLoadResult soundResult = soundFrom(index, source.soundConfig());
            if (soundResult.invalidAnnouncement() != null) {
                invalidAnnouncements.add(soundResult.invalidAnnouncement());
                continue;
            }
            try {
                Component component = miniMessage.deserialize(source.message());
                validAnnouncements.add(
                    new AnnouncementEntry(
                        index,
                        source.message(),
                        component,
                        PLAIN_TEXT.serialize(component),
                        soundResult.sound()));
            } catch (RuntimeException exception) {
                invalidAnnouncements.add(
                    new InvalidAnnouncement(
                        index,
                        source.message(),
                        "validation.parse-error",
                        messageOf(exception)));
            }
        }

        if (entries.isEmpty()) {
            notices.add(
                new ConfigNotice(
                    "validation.messages-empty",
                    "tips",
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
     * 返回是否不存在可以发送的公告
     *
     * @return 已校验公告列表为空时返回 {@code true}
     */
    public boolean hasNoValidAnnouncements() {
        return validAnnouncements.isEmpty();
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

    /**
     * 从 tips 列表项提取公告文本和可选声音配置
     *
     * @param index 公告一基序号
     * @param entry tips 列表中的原始条目
     * @return 公告文本来源描述
     */
    private static AnnouncementSource announcementSource(int index, Object entry) {
        if (entry instanceof String message) {
            return AnnouncementSource.valid(message, null);
        }
        if (entry instanceof Map<?, ?> map) {
            Object message = map.get("message");
            return AnnouncementSource.valid(stringValue(message), map.get("sound"));
        }
        return AnnouncementSource.invalid(
            new InvalidAnnouncement(
                index,
                "",
                "validation.unsupported-tip-entry",
                className(entry)));
    }

    /**
     * 从公告声音配置中加载声音对象
     *
     * @param index 公告一基序号
     * @param soundConfig 原始声音配置
     * @return 声音加载结果
     */
    private static SoundLoadResult soundFrom(int index, Object soundConfig) {
        if (soundConfig == null) {
            return SoundLoadResult.valid(null);
        }
        if (soundConfig instanceof String soundName) {
            return soundFrom(index, soundName, DEFAULT_SOUND_SOURCE, DEFAULT_SOUND_VOLUME, DEFAULT_SOUND_PITCH);
        }
        if (!(soundConfig instanceof Map<?, ?> map)) {
            return SoundLoadResult.invalid(
                new InvalidAnnouncement(
                    index,
                    "",
                    "validation.unsupported-sound-entry",
                    className(soundConfig)));
        }

        if (!booleanValue(map.get("enabled"), true)) {
            return SoundLoadResult.valid(null);
        }
        String soundName = stringValue(map.get("name"));
        Sound.Source source = sourceFrom(stringValue(map.get("source")));
        if (source == null) {
            return SoundLoadResult.invalid(
                new InvalidAnnouncement(
                    index,
                    "",
                    "validation.invalid-sound-source",
                    stringValue(map.get("source"))));
        }

        Float volume = floatValue(map.get("volume"), DEFAULT_SOUND_VOLUME);
        if (volume == null || volume < 0.0F) {
            return SoundLoadResult.invalid(
                new InvalidAnnouncement(
                    index,
                    "",
                    "validation.invalid-sound-volume",
                    stringValue(map.get("volume"))));
        }

        Float pitch = floatValue(map.get("pitch"), DEFAULT_SOUND_PITCH);
        if (pitch == null || pitch <= 0.0F) {
            return SoundLoadResult.invalid(
                new InvalidAnnouncement(
                    index,
                    "",
                    "validation.invalid-sound-pitch",
                    stringValue(map.get("pitch"))));
        }

        return soundFrom(index, soundName, source, volume, pitch);
    }

    /**
     * 根据声音名称和数值构建声音配置
     *
     * @param index 公告一基序号
     * @param soundName 声音名称
     * @param source 声音分类
     * @param volume 音量
     * @param pitch 音调
     * @return 声音加载结果
     */
    private static SoundLoadResult soundFrom(
        int index,
        String soundName,
        Sound.Source source,
        float volume,
        float pitch) {
        String normalizedName = normalizeSoundName(soundName);
        if (normalizedName.isBlank()) {
            return SoundLoadResult.invalid(
                new InvalidAnnouncement(index, "", "validation.blank-sound-name", ""));
        }
        if (!Key.parseable(normalizedName)) {
            return SoundLoadResult.invalid(
                new InvalidAnnouncement(
                    index,
                    "",
                    "validation.invalid-sound-name",
                    normalizedName));
        }
        return SoundLoadResult.valid(
            new AnnouncementSound(Key.key(normalizedName), source, volume, pitch));
    }

    /**
     * 读取声音分类, 未配置时使用默认分类
     *
     * @param rawSource 原始声音分类文本
     * @return 声音分类, 文本无效时返回 {@code null}
     */
    private static Sound.Source sourceFrom(String rawSource) {
        if (rawSource.isBlank()) {
            return DEFAULT_SOUND_SOURCE;
        }
        try {
            return Sound.Source.valueOf(rawSource.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    /**
     * 补齐声音名称的默认命名空间
     *
     * @param soundName 原始声音名称
     * @return 可交给 Adventure Key 解析的声音名称
     */
    private static String normalizeSoundName(String soundName) {
        String normalized = soundName.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank() || normalized.contains(":")) {
            return normalized;
        }
        return Key.MINECRAFT_NAMESPACE + ":" + normalized;
    }

    /**
     * 将配置值读取为字符串
     *
     * @param value 原始配置值
     * @return 字符串形式, 空值返回空字符串
     */
    private static String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    /**
     * 将配置值读取为布尔值
     *
     * @param value 原始配置值
     * @param fallback 默认值
     * @return 布尔配置值
     */
    private static boolean booleanValue(Object value, boolean fallback) {
        if (value == null) {
            return fallback;
        }
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    /**
     * 将配置值读取为浮点数
     *
     * @param value 原始配置值
     * @param fallback 默认值
     * @return 浮点配置值, 无法解析时返回 {@code null}
     */
    private static Float floatValue(Object value, float fallback) {
        if (value == null) {
            return fallback;
        }
        if (value instanceof Number number) {
            return number.floatValue();
        }
        try {
            return Float.parseFloat(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    /**
     * 返回配置值的类名
     *
     * @param value 原始配置值
     * @return 配置值类名
     */
    private static String className(Object value) {
        return value == null ? "null" : value.getClass().getSimpleName();
    }

    /**
     * 将低于最小值的秒数提升到最小值, 并记录配置提示
     *
     * @param value 原始配置值
     * @param minimum 允许的最小值
     * @param path 配置路径
     * @param notices 用于追加提示的列表
     * @return 归一化后的秒数
     */
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

    /**
     * 提取异常消息, 异常没有消息时使用异常类名
     *
     * @param exception MiniMessage 解析时抛出的异常
     * @return 可写入诊断文本的异常摘要
     */
    private static String messageOf(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return message;
    }

    /**
     * tips 列表项解析结果
     *
     * @param message 公告 MiniMessage 文本
     * @param soundConfig 原始声音配置
     * @param invalidAnnouncement 无法解析时生成的无效公告
     */
    private record AnnouncementSource(
        String message,
        Object soundConfig,
        InvalidAnnouncement invalidAnnouncement) {

        /**
         * 创建有效公告来源
         *
         * @param message 公告 MiniMessage 文本
         * @param soundConfig 原始声音配置
         * @return 有效公告来源
         */
        private static AnnouncementSource valid(String message, Object soundConfig) {
            return new AnnouncementSource(message, soundConfig, null);
        }

        /**
         * 创建无效公告来源
         *
         * @param invalidAnnouncement 无效公告描述
         * @return 无效公告来源
         */
        private static AnnouncementSource invalid(InvalidAnnouncement invalidAnnouncement) {
            return new AnnouncementSource("", null, invalidAnnouncement);
        }
    }

    /**
     * 声音配置解析结果
     *
     * @param sound 可播放声音配置, 没有声音时为 {@code null}
     * @param invalidAnnouncement 无法解析时生成的无效公告
     */
    private record SoundLoadResult(
        AnnouncementSound sound,
        InvalidAnnouncement invalidAnnouncement) {

        /**
         * 创建有效声音解析结果
         *
         * @param sound 可播放声音配置, 没有声音时为 {@code null}
         * @return 有效声音解析结果
         */
        private static SoundLoadResult valid(AnnouncementSound sound) {
            return new SoundLoadResult(sound, null);
        }

        /**
         * 创建无效声音解析结果
         *
         * @param invalidAnnouncement 无效公告描述
         * @return 无效声音解析结果
         */
        private static SoundLoadResult invalid(InvalidAnnouncement invalidAnnouncement) {
            return new SoundLoadResult(null, invalidAnnouncement);
        }
    }
}
