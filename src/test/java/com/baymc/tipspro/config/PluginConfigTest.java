package com.baymc.tipspro.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

/**
 * 保护公告配置加载和声音配置解析约定
 */
final class PluginConfigTest {

    /**
     * 校验 tips.yml 同时支持对象公告声音和旧字符串公告
     *
     * @throws InvalidConfigurationException YAML 测试内容解析失败时抛出
     */
    @Test
    void loadSupportsAnnouncementSoundAndLegacyStringTips()
        throws InvalidConfigurationException {
        YamlConfiguration configuration = new YamlConfiguration();
        YamlConfiguration tipsConfiguration = new YamlConfiguration();
        tipsConfiguration.loadFromString(
            """
            tips:
              - message: "<green>有声音公告</green>"
                sound:
                  name: "block.note_block.pling"
                  source: "ui"
                  volume: 0.8
                  pitch: 1.2
              - "<yellow>旧写法公告</yellow>"
            """);

        PluginConfig config =
            PluginConfig.load(configuration, tipsConfiguration, MiniMessage.miniMessage());

        assertEquals(2, config.validAnnouncementCount());
        assertEquals(0, config.invalidAnnouncementCount());
        AnnouncementSound sound = config.validAnnouncements().getFirst().sound();
        assertNotNull(sound);
        assertEquals("minecraft:block.note_block.pling", sound.key().asString());
        assertEquals(Sound.Source.UI, sound.source());
        assertEquals(0.8F, sound.volume(), 0.001F);
        assertEquals(1.2F, sound.pitch(), 0.001F);
        assertNull(config.validAnnouncements().get(1).sound());
    }

    /**
     * 校验无效声音名称会让对应公告进入无效公告列表
     *
     * @throws InvalidConfigurationException YAML 测试内容解析失败时抛出
     */
    @Test
    void loadRejectsInvalidAnnouncementSoundName()
        throws InvalidConfigurationException {
        YamlConfiguration configuration = new YamlConfiguration();
        YamlConfiguration tipsConfiguration = new YamlConfiguration();
        tipsConfiguration.loadFromString(
            """
            tips:
              - message: "<green>错误声音公告</green>"
                sound:
                  name: "错误 声音"
            """);

        PluginConfig config =
            PluginConfig.load(configuration, tipsConfiguration, MiniMessage.miniMessage());

        assertEquals(0, config.validAnnouncementCount());
        assertEquals(1, config.invalidAnnouncementCount());
        assertEquals("validation.invalid-sound-name", config.invalidAnnouncements().getFirst().reasonKey());
    }
}
