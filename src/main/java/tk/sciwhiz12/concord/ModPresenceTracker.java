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

package tk.sciwhiz12.concord;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.event.EventNetworkChannel;

import javax.annotation.Nullable;

/**
 * Tracks if a client has this mod installed.
 *
 * @author SciWhiz12
 */
public class ModPresenceTracker {
    public static final ResourceLocation CHANNEL_NAME = new ResourceLocation(Concord.MODID, "exists");
    public static final EventNetworkChannel CHANNEL = NetworkRegistry.ChannelBuilder
        .named(CHANNEL_NAME)
        .networkProtocolVersion(() -> "yes")
        .clientAcceptedVersions(version -> true)
        .serverAcceptedVersions(version -> true)
        .eventNetworkChannel();

    public static void register() {
        // Channel is created as part of class initialization
    }

    public static boolean isModPresent(@Nullable ServerPlayer client) {
        return client != null && CHANNEL.isRemotePresent(client.connection.getConnection());
    }
}
