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

package tk.sciwhiz12.concord.compat.emojiful;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;

import com.hrznstudio.emojiful.ClientProxy;
import com.hrznstudio.emojiful.Emojiful;
import com.hrznstudio.emojiful.api.Emoji;
import com.hrznstudio.emojiful.api.EmojiCategory;

import tk.sciwhiz12.concord.Concord;

public class EmojifulCompat {

    private static MethodHandle indexEmojisMethod;
    private static List<String> registeredGuildCategories = new ArrayList<>();

    public static void loadDiscordEmoji(String guildName, long emojiId, String emojiName, boolean animated) {
        final Emoji emojifulEmoji = new EmojiFromDiscord(emojiId, emojiName, animated);
        Emojiful.EMOJI_MAP.computeIfAbsent(guildName, n -> new ArrayList<>()).add(emojifulEmoji);
        Emojiful.EMOJI_LIST.add(emojifulEmoji);
        if (!registeredGuildCategories.contains(guildName)) {
            registeredGuildCategories.add(guildName);
            final EmojiCategory cat = new EmojiCategory(guildName, false);
            ClientProxy.CATEGORIES.add(cat);
        }
    }

    public static void removeDiscordEmoji(String guildName, long emojiId, String emojiName, boolean animated) {
        Emojiful.EMOJI_MAP.computeIfAbsent(guildName, n -> new ArrayList<>()).stream()
            .filter(e -> e instanceof EmojiFromDiscord dcEmoji && dcEmoji.isSame(emojiId, emojiName, animated))
            .findAny()
            .ifPresent(emoji -> {
                Emojiful.EMOJI_MAP.computeIfAbsent(guildName, k -> new ArrayList<>()).remove(emoji);
                Emojiful.EMOJI_LIST.remove(emoji);
            });
    }

    public static void indexEmojis() {
        try {
            indexEmojisMethod.bindTo(ClientProxy.PROXY).invoke();
        } catch (Throwable e) {
            Concord.LOGGER.error("Exception while trying to re-index Emojiful emojis: ", e);
        }
    }

    public static void lookupIndexEmojisMethod() throws NoSuchMethodException, IllegalAccessException {
        final MethodType type = MethodType.methodType(void.class);
        indexEmojisMethod = MethodHandles.privateLookupIn(ClientProxy.class, MethodHandles.lookup())
            .findVirtual(ClientProxy.class, "indexEmojis", type);
    }

}
