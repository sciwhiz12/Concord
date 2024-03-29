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

package dev.sciwhiz12.concord.msg;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mojang.authlib.GameProfile;
import dev.sciwhiz12.concord.ChatBot;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.util.concurrent.TimeUnit;

public class SentMessageMemory {
    private final ChatBot bot;
    private final Cache<Long, RememberedMessageImpl> memory = CacheBuilder.newBuilder()
            .expireAfterAccess(6, TimeUnit.HOURS)
            .initialCapacity(1_000)
            .build();

    public SentMessageMemory(ChatBot bot) {
        this.bot = bot;
    }

    public void rememberMessage(long messageSnowflake, GameProfile player, Component message) {
        memory.put(messageSnowflake, new RememberedMessageImpl(player, message));
    }

    public @Nullable RememberedMessage findMessage(long messageSnowflake) {
        return memory.getIfPresent(messageSnowflake);
    }

    public interface RememberedMessage {
        GameProfile player();

        Component message();
    }

    record RememberedMessageImpl(GameProfile player, Component message) implements RememberedMessage {
    }
}
