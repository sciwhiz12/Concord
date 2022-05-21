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

import net.minecraft.network.Connection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.ConnectionData;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.event.EventNetworkChannel;

import javax.annotation.Nullable;
import java.util.function.Predicate;

/**
 * Networking for Concord.
 *
 * <p>None of Concord's channels are required to be present on either server or client. This is to allow connections
 * between servers and clients which do not have the mod installed.</p>
 *
 * <p>The channel with name {@code concord:exists} communicates the existence of this mod between server and client. It
 * is mainly used for backwards compatibility with existing published Concord versions, and may be modified, replaced,
 * or removed in a future major version update.</p>
 */
public class ConcordNetwork {
    private static final Predicate<String> TRUE = str -> true;

    public static final ResourceLocation EXISTENCE_CHANNEL_NAME = new ResourceLocation(Concord.MODID, "exists");
    public static final EventNetworkChannel EXISTENCE_CHANNEL = NetworkRegistry.ChannelBuilder
            .named(EXISTENCE_CHANNEL_NAME)
            .networkProtocolVersion(() -> "yes")
            .clientAcceptedVersions(TRUE)
            .serverAcceptedVersions(TRUE)
            .eventNetworkChannel();

    public static void register() {
        // Existence channel is created as part of class initialization
    }

    public static boolean isModPresent(@Nullable ServerPlayer client) {
        return client != null && EXISTENCE_CHANNEL.isRemotePresent(client.connection.getConnection());
    }

    @Nullable
    public static String getChannelVersion(Connection connection, ResourceLocation channelName) {
        @Nullable final ConnectionData connectionData = NetworkHooks.getConnectionData(connection);
        if (connectionData == null) return null;
        return connectionData.getChannels().get(channelName);
    }
}
