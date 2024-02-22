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

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import tk.sciwhiz12.concord.Concord;

import java.util.Map;

public record FeaturesPayload(Map<String, ArtifactVersion> features) implements CustomPacketPayload {
    public static final ResourceLocation ID = new ResourceLocation(Concord.MODID, "features");

    public FeaturesPayload {
        features = Map.copyOf(features);
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeMap(features,
                FriendlyByteBuf::writeUtf,
                (buf, ver) -> buf.writeUtf(ver.toString())
        );
    }

    public static FeaturesPayload read(FriendlyByteBuf buffer) {
        return new FeaturesPayload(buffer.readMap(
                FriendlyByteBuf::readUtf,
                buf -> new DefaultArtifactVersion(buf.readUtf())
        ));
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }
}
