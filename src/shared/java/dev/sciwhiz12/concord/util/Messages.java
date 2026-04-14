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

package dev.sciwhiz12.concord.util;

import dev.sciwhiz12.concord.Concord;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

public enum Messages implements Translation {
    BOT_START("bot.start", "1.0.0", "_Discord integration is now active!_"),
    BOT_STOP("bot.stop", "1.0.0", "_Discord integration is being disabled!_"),
    SERVER_START("server.start", "1.0.0", "_Server is now started!_"),
    SERVER_STOP("server.stop", "1.0.0", "_Server is stopping!_"),
    SAY_COMMAND("command.say", "1.0.0", "[**%s**] %s"),
    EMOTE_COMMAND("command.emote", "1.3.0", "_**%s** %s_"),
    PLAYER_JOIN("player.join", "1.0.0", "**%s** _joined the game._"),
    PLAYER_LEAVE("player.leave", "1.0.0", "**%s** _left the game._"),
    ADVANCEMENT_TASK("player.advancement.task", "1.0.0", "**%s** has made the advancement **%s**\n_%s_"),
    ADVANCEMENT_CHALLENGE("player.advancement.challenge", "1.0.0", "**%s** has completed the challenge **%s**\n_%s_"),
    ADVANCEMENT_GOAL("player.advancement.goal", "1.0.0", "**%s** has reached the goal **%s**\n_%s_"),
    BOT_STATUS_ONLINE("bot.status.online", "1.3.0", "Minecraft - %s online");

    private final String key;
    private final ArtifactVersion lastModifiedVersion;
    private final String englishText;

    Messages(String path, String lastModifiedVersion, String englishText) {
        this.key = "message." + Concord.MODID + '.' + path;
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
