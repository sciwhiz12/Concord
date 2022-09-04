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

package tk.sciwhiz12.concord.util;

import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import tk.sciwhiz12.concord.Concord;

public enum Translations implements Translation {
    // Member status

    STATUS_ONLINE("chat", "status.online", "1.0.0", "Online"),
    STATUS_IDLE("chat", "status.idle", "1.0.0", "Idle"),
    STATUS_DO_NOT_DISTURB("chat", "status.do_not_disturb", "1.0.0", "Do Not Disturb"),
    STATUS_STREAMING("chat", "status.streaming", "1.0.0", "Streaming"),
    STATUS_OFFLINE("chat", "status.offline", "1.0.0", "Offline"),
    STATUS_UNKNOWN("chat", "status.unknown", "1.0.0", "UNKNOWN!"),

    // Chat messages

    CHAT_HEADER("chat", "header", "1.0.0", "(%s) %s"),
    CHAT_REPLY_USER("chat", "reply", "1.1.0", "in reply to %s: "),
    CHAT_REPLY_UNKNOWN("chat", "reply.unknown", "1.1.0", "an unknown user"),
    CHAT_ATTACHMENT_WITH_EXTENSION("chat", "attachment", "1.1.0", "attachment:%s"),
    CHAT_ATTACHMENT_NO_EXTENSION("chat", "attachment.no_extension", "1.1.0", "attachment"),
    CHAT_STICKER("chat", "sticker", "1.1.0", "sticker:%s"),

    // Hover text

    HOVER_HEADER("chat", "hover.header", "1.0.0", "%s#%s %s- %s %s"),
    HOVER_ROLES("chat", "hover.roles", "1.0.0", "Roles: "),
    HOVER_REPLY("chat", "hover.reply", "1.1.0", "Replied message: %s"),

    HOVER_ATTACHMENT_FILENAME("chat", "attachment.hover.filename", "1.1.0", "File name: %s"),
    HOVER_ATTACHMENT_CLICK("chat", "attachment.hover.click", "1.1.0", "Click to open attachment in browser"),

    // Commands

    COMMAND_ENABLING("command", "enable", "1.0.0", "Enabling discord integration..."),
    COMMAND_ALREADY_ENABLED("command", "enable.already_enabled", "1.0.0", "Discord integration is already enabled!"),
    COMMAND_DISABLING("command", "disable", "1.0.0", "Disabling discord integration..."),
    COMMAND_ALREADY_DISABLED("command", "disable.already_disabled", "1.0.0", "Discord integration is already disabled!"),
    COMMAND_RELOADING("command", "reload", "1.0.0", "Reloading discord integration..."),
    COMMAND_REPORT_STATUS("command", "report.status", "1.1.0", "Reporting users is currently %s"),
    COMMAND_REPORT_SUCCESS("command", "report.success", "1.1.0", "Submitted report for %s for reason: %s"),

    COMMAND_SUPPORT_DISABLED("command", "support.disabled", "1.1.0", "Sorry, but Concord is currently disabled, you may not send a support ticket at this time."),
    COMMAND_SUPPORT_SUCCESS("command", "support.success", "1.1.0", "Support Ticket sent successfully."),
    COMMAND_STATUS_PREFIX("command", "status", "1.0.0", "Discord integration status: %s"),
    COMMAND_STATUS_ENABLED("command", "status.enabled", "1.0.0", "ENABLED"),
    COMMAND_STATUS_DISABLED("command", "status.disabled", "1.0.0", "DISABLED");

    private final String key;
    private final String englishText;
    private final ArtifactVersion lastModifiedVersion;

    Translations(String prefix, String path, String lastModifiedVersion, String englishText) {
        this.key = prefix + '.' + Concord.MODID + '.' + path;
        this.englishText = englishText;
        this.lastModifiedVersion = new DefaultArtifactVersion(lastModifiedVersion);
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public String englishText() {
        return englishText;
    }

    @Override
    public ArtifactVersion lastModifiedVersion() {
        return lastModifiedVersion;
    }
}
