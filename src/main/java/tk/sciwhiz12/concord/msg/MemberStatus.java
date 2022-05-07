/*
 * Concord - Copyright (c) 2020 SciWhiz12
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package tk.sciwhiz12.concord.msg;

import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Member;
import net.minecraft.network.chat.TextColor;
import tk.sciwhiz12.concord.util.Translations;
import tk.sciwhiz12.concord.util.Translation;

public enum MemberStatus {
    ONLINE(OnlineStatus.ONLINE, Translations.STATUS_ONLINE, 0x43b581, '\u25cf'),
    IDLE(OnlineStatus.IDLE, Translations.STATUS_IDLE, 0xfaa61a, '\u263d'),
    DO_NOT_DISTURB(OnlineStatus.DO_NOT_DISTURB, Translations.STATUS_DO_NOT_DISTURB, 0xf04747, '\u2205'),
    STREAMING(OnlineStatus.DO_NOT_DISTURB, Translations.STATUS_STREAMING, 0x593695, '\u25b6'),
    OFFLINE(OnlineStatus.OFFLINE, Translations.STATUS_OFFLINE, 0x747f8d, '\u25cb'),
    UNKNOWN(OnlineStatus.UNKNOWN, Translations.STATUS_UNKNOWN, 0x7c0000, '\u003f');

    public static final char CROWN_ICON = '\u2606';

    private final OnlineStatus discordStatus;
    private final Translation translationKey;
    private final TextColor color;
    private final char icon;

    MemberStatus(OnlineStatus discordStatus, Translation translationKey, int colorHex, char icon) {
        this.discordStatus = discordStatus;
        this.translationKey = translationKey;
        this.color = TextColor.fromRgb(colorHex);
        this.icon = icon;
    }

    public OnlineStatus getDiscordStatus() {
        return discordStatus;
    }

    public Translation getTranslation() {
        return translationKey;
    }

    public TextColor getColor() {
        return color;
    }

    public char getIcon() {
        return icon;
    }

    public static MemberStatus from(OnlineStatus status) {
        return switch (status) {
            case ONLINE -> ONLINE;
            case IDLE -> IDLE;
            case INVISIBLE, OFFLINE -> OFFLINE;
            case DO_NOT_DISTURB -> DO_NOT_DISTURB;
            default -> UNKNOWN;
        };
    }

    public static MemberStatus from(Member member) {
        return switch (member.getOnlineStatus()) {
            case ONLINE -> ONLINE;
            case IDLE -> IDLE;
            case INVISIBLE, OFFLINE -> OFFLINE;
            case DO_NOT_DISTURB -> member.getActivities().stream().anyMatch(act -> act.getType() == Activity.ActivityType.STREAMING)
                ? STREAMING : DO_NOT_DISTURB;
            default -> UNKNOWN;
        };
    }
}
