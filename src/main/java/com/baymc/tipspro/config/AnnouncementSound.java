package com.baymc.tipspro.config;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;

/**
 * 表示一条公告附带的声音配置
 *
 * <p>声音使用 Adventure key 与 source, 避免绑定 Bukkit Sound 枚举版本
 * 也避免通过玩家位置播放声音
 *
 * @param key Minecraft 声音 key
 * @param source 声音分类
 * @param volume 音量
 * @param pitch 音调
 */
public record AnnouncementSound(
    Key key,
    Sound.Source source,
    float volume,
    float pitch) {

    /**
     * 转换为 Adventure 声音对象
     *
     * @return 可直接发送给玩家的声音对象
     */
    public Sound toAdventureSound() {
        return Sound.sound(key, source, volume, pitch);
    }
}
