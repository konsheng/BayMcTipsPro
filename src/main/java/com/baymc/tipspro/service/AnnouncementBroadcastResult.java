package com.baymc.tipspro.service;

import com.baymc.tipspro.config.AnnouncementEntry;

/**
 * 公告广播尝试后的返回结果
 *
 * @param sent 是否已经选择有效公告并进入广播路径
 * @param onlinePlayers 广播时目标在线玩家数量
 * @param announcement 已选择的公告, 没有可用公告时为 {@code null}
 */
public record AnnouncementBroadcastResult(
    boolean sent,
    int onlinePlayers,
    AnnouncementEntry announcement) {

    /**
     * 创建没有可用公告时的结果
     *
     * @return 未成功广播的结果
     */
    public static AnnouncementBroadcastResult noAnnouncement() {
        return new AnnouncementBroadcastResult(false, 0, null);
    }

    /**
     * 创建成功广播后的结果
     *
     * @param onlinePlayers 目标在线玩家数量
     * @param announcement 已选择的公告条目
     * @return 成功广播的结果
     */
    public static AnnouncementBroadcastResult sent(
        int onlinePlayers,
        AnnouncementEntry announcement) {
        return new AnnouncementBroadcastResult(true, onlinePlayers, announcement);
    }
}
