package com.baymc.tipspro.service;

import com.baymc.tipspro.config.AnnouncementEntry;

/**
 * Result returned after an announcement broadcast attempt.
 *
 * @param sent whether a valid announcement was selected and delivered to the broadcast path
 * @param onlinePlayers number of online players targeted at the time of broadcast
 * @param announcement selected announcement, or null when no announcement was available
 */
public record AnnouncementBroadcastResult(
    boolean sent,
    int onlinePlayers,
    AnnouncementEntry announcement) {

    /**
     * Creates a result for the no-valid-announcement case.
     *
     * @return unsuccessful broadcast result
     */
    public static AnnouncementBroadcastResult noAnnouncement() {
        return new AnnouncementBroadcastResult(false, 0, null);
    }

    /**
     * Creates a successful broadcast result.
     *
     * @param onlinePlayers number of online players targeted
     * @param announcement selected announcement entry
     * @return successful broadcast result
     */
    public static AnnouncementBroadcastResult sent(
        int onlinePlayers,
        AnnouncementEntry announcement) {
        return new AnnouncementBroadcastResult(true, onlinePlayers, announcement);
    }
}
