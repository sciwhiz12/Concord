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

package tk.sciwhiz12.concord.network;

import java.util.Arrays;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent.Context;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import tk.sciwhiz12.concord.Concord;
import tk.sciwhiz12.concord.network.packet.Packet;
import tk.sciwhiz12.concord.network.packet.RegisterEmotePacket;
import tk.sciwhiz12.concord.network.packet.RemoveEmotePacket;

/**
 * Concord feature versions. Each constant is linked to a specific feature of Concord, and holds the current version
 * for that feature. By having each feature be versioned separately and communicated between client and server, Concord
 * can dynamically adapt to clients having different versions of Concord installed.
 */
public enum FeatureVersion {
    /**
     * The Emojiful compatibility feature.
     */
    EMOJIFUL_COMPAT("emojiful_compat", "1.0.0", 
            new PacketData<>(RegisterEmotePacket.class, RegisterEmotePacket::decode, "1.0.0"),
            new PacketData<>(RemoveEmotePacket.class, RemoveEmotePacket::decode, "1.0.0")),
    /**
     * The translation keys feature.
     */
    // 1.0.0: All previous releases
    // 1.1.0: v1.4.0
    TRANSLATIONS("translations", "1.1.0"),
    /**
     * The custom icon fonts feature.
     */
    // 1.0.0: All previous releases
    ICONS("icons", "1.0.0");

    private final String featureName;
    private final ResourceLocation channelName;
    private final ArtifactVersion currentVersion;
    private final PacketData<?>[] packets;
    
    private final Map<Class<?>, PacketData<?>> packetLookup;

    @SafeVarargs
    FeatureVersion(String featureName, String currentVersion, PacketData<?>... packets) {
        this.featureName = featureName;
        this.channelName = new ResourceLocation(Concord.MODID, featureName);
        this.currentVersion = new DefaultArtifactVersion(currentVersion);
        this.packets = packets;
        
        this.packetLookup = Arrays.stream(packets)
                .collect(Collectors.toUnmodifiableMap(PacketData::packetClass, Function.identity()));
    }

    /**
     * {@return the name of the feature} This is usually the name of the constant in lowercase.
     */
    public String featureName() {
        return featureName;
    }

    /**
     * {@return the name of the version carrier networking channel for this feature}
     */
    public ResourceLocation channelName() {
        return channelName;
    }

    /**
     * {@return the current version of this feature}
     */
    public ArtifactVersion currentVersion() {
        return currentVersion;
    }
    
    /**
     * {@return the packets associated with this feature}
     */
    public PacketData<?>[] packets() {
        return packets;
    }
    
    /**
     * Sends a packet to a player, if the player can handle that packet.
     * 
     * @param player the player to send the packet to
     * @param packet the packet to send
     */
    public void sendPacket(ServerPlayer player, Packet packet) {
        final PacketData<?> data = packetLookup.get(packet.getClass());
        if (data == null)
            return;
        final ArtifactVersion connectionVersion = ConcordNetwork.getFeatureVersion(player.connection.getConnection(), this);
        if (connectionVersion.compareTo(data.version()) >= 0)
            ConcordNetwork.getChannel(this).send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
    
    /**
     * Sends a packet to the server, if the server can handle that packet.
     * 
     * @param packet the packet to send
     */
    public void sendToServer(Packet packet) {
        final PacketData<?> data = packetLookup.get(packet.getClass());
        if (data == null)
            return;
        final ArtifactVersion connectionVersion = ConcordNetwork.getFeatureVersion(Minecraft.getInstance().getConnection().getConnection(), this);
        if (connectionVersion.compareTo(data.version()) >= 0)
            ConcordNetwork.getChannel(this).sendToServer(packet);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "(" + featureName + " feature, current version " + currentVersion + ")";
    }
    
    public record PacketData<T extends Packet>(Class<T> packetClass, Function<FriendlyByteBuf, T> decoder, NetworkDirection direction, ArtifactVersion version) {
        public PacketData(Class<T> packetClass, Function<FriendlyByteBuf, T> decoder, NetworkDirection direction, String version) {
            this(packetClass, decoder, direction, new DefaultArtifactVersion(version));
        }
        public PacketData(Class<T> packetClass, Function<FriendlyByteBuf, T> decoder, String version) {
            this(packetClass, decoder, NetworkDirection.PLAY_TO_CLIENT, new DefaultArtifactVersion(version));
        }
        
        public void build(SimpleChannel channel, int index) {
            channel.messageBuilder(packetClass(), index)
                    .consumer((BiConsumer<T, Supplier<Context>>) Packet::handle)
                    .encoder(Packet::encode)
                    .decoder(decoder)
                    .add();
        }
    }
    
}
