package tk.sciwhiz12.concord.msg;

import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Member;
import net.minecraft.util.text.Color;

public enum MemberStatus {
    ONLINE(OnlineStatus.ONLINE, "chat.concord.status.online", 0x43b581, '\u25cf'),
    IDLE(OnlineStatus.IDLE, "chat.concord.status.idle", 0xfaa61a, '\u263d'),
    DO_NOT_DISTURB(OnlineStatus.DO_NOT_DISTURB, "chat.concord.status.do_not_disturb", 0xf04747, '\u2205'),
    STREAMING(OnlineStatus.DO_NOT_DISTURB, "chat.concord.status.streaming", 0x593695, '\u25b6'),
    OFFLINE(OnlineStatus.OFFLINE, "chat.concord.status.offline", 0x747f8d, '\u25cb'),
    UNKNOWN(OnlineStatus.UNKNOWN, "chat.concord.status.unknown", 0x7c0000, '\u003f');

    public static final char CROWN_ICON = '\u2606';

    private final OnlineStatus discordStatus;
    private final String translationKey;
    private final Color color;
    private final char icon;

    MemberStatus(OnlineStatus discordStatus, String translationKey, int colorHex, char icon) {
        this.discordStatus = discordStatus;
        this.translationKey = translationKey;
        this.color = Color.fromInt(colorHex);
        this.icon = icon;
    }

    public OnlineStatus getDiscordStatus() {
        return discordStatus;
    }

    public String getTranslationKey() {
        return translationKey;
    }

    public Color getColor() {
        return color;
    }

    public char getIcon() {
        return icon;
    }

    public static MemberStatus from(OnlineStatus status) {
        switch (status) {
            case ONLINE:
                return ONLINE;
            case IDLE:
                return IDLE;
            case INVISIBLE:
            case OFFLINE:
                return OFFLINE;
            case DO_NOT_DISTURB:
                return DO_NOT_DISTURB;
            case UNKNOWN:
            default:
                return UNKNOWN;
        }
    }

    public static MemberStatus from(Member member) {
        switch (member.getOnlineStatus()) {
            case ONLINE:
                return ONLINE;
            case IDLE:
                return IDLE;
            case INVISIBLE:
            case OFFLINE:
                return OFFLINE;
            case DO_NOT_DISTURB: {
                if (member.getActivities().stream().anyMatch(act -> act.getType() == Activity.ActivityType.STREAMING))
                    return STREAMING;
                else
                    return DO_NOT_DISTURB;
            }
            case UNKNOWN:
            default:
                return UNKNOWN;
        }
    }
}
