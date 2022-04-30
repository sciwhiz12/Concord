/*
 * Concord - Copyright (c) 2020-2022 SciWhiz12
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

import tk.sciwhiz12.concord.Concord;

public enum Translations implements Translation {
    // Member status

    STATUS_ONLINE("chat", "status.online", "Online"),
    STATUS_IDLE("chat", "status.idle", "Idle"),
    STATUS_DO_NOT_DISTURB("chat", "status.do_not_disturb", "Do Not Disturb"),
    STATUS_STREAMING("chat", "status.streaming", "Streaming"),
    STATUS_OFFLINE("chat", "status.offline", "Offline"),
    STATUS_UNKNOWN("chat", "status.unknown", "UNKNOWN!"),

    // Chat messages

    CHAT_HEADER("chat", "header", "(%s) %s"),
    CHAT_REPLY_USER("chat", "reply", "in reply to %s: "),
    CHAT_REPLY_UNKNOWN("chat", "reply.unknown", "an unknown user"),
    CHAT_ATTACHMENT_WITH_EXTENSION("chat", "attachment", "attachment:%s"),
    CHAT_ATTACHMENT_NO_EXTENSION("chat", "attachment.no_extension", "attachment"),

    // Hover text

    HOVER_HEADER("chat", "hover.header", "%s#%s %s- %s %s"),
    HOVER_ROLES("chat", "hover.roles", "Roles: "),
    HOVER_REPLY("chat", "hover.reply", "Replied message: %s"),

    HOVER_ATTACHMENT_FILENAME("chat", "attachment.hover.filename", "File name: %s"),
    HOVER_ATTACHMENT_CLICK("chat", "attachment.hover.click", "Click to open attachment in browser"),

    // Commands

    COMMAND_ENABLING("command", "enable", "Enabling discord integration..."),
    COMMAND_ALREADY_ENABLED("command", "enable.already_enabled", "Discord integration is already enabled!"),
    COMMAND_DISABLING("command", "disable", "Disabling discord integration..."),
    COMMAND_ALREADY_DISABLED("command", "disable.already_disabled", "Discord integration is already disabled!"),
    COMMAND_RELOADING("command", "reload", "Reloading discord integration..."),
    COMMAND_STATUS_PREFIX("command", "status", "Discord integration status: %s"),
    COMMAND_STATUS_ENABLED("command", "status.enabled", "ENABLED"),
    COMMAND_STATUS_DISABLED("command", "status.disabled", "DISABLED");

    private final String key;
    private final String englishText;

    Translations(String prefix, String path, String englishText) {
        this.key = prefix + '.' + Concord.MODID + '.' + path;
        this.englishText = englishText;
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public String englishText() {
        return englishText;
    }
}
