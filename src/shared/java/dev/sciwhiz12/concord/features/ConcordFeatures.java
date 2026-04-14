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

package dev.sciwhiz12.concord.features;

import dev.sciwhiz12.concord.Concord;
import io.netty.util.AttributeKey;
import net.minecraft.server.level.ServerPlayer;
import org.apache.maven.artifact.versioning.ArtifactVersion;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

public class ConcordFeatures {
    public static final AttributeKey<ConcordFeatures> CHANNEL_ATTRIBUTE_KEY = AttributeKey.newInstance(Concord.MODID +":features");
    public static final ConcordFeatures EMPTY =new ConcordFeatures(Map.of());

    private final Map<String, ArtifactVersion> features;

    public ConcordFeatures(Map<String, ArtifactVersion> features) {
        this.features = Map.copyOf(features);
    }

    public boolean hasFeature(FeatureVersion feature) {
        return this.features.containsKey(feature.featureName());
    }

    @Nullable
    public ArtifactVersion getFeature(FeatureVersion feature) {
        return this.features.get(feature.featureName());
    }

    public ArtifactVersion getFeatureOrThrow(FeatureVersion feature) {
        final @Nullable ArtifactVersion version = getFeature(feature);
        if (version == null) {
            throw new NoSuchElementException(feature.featureName());
        }
        return version;
    }

    public boolean isEmpty() {
        return this.features.isEmpty();
    }

    @Override
    public String toString() {
        return features.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConcordFeatures that = (ConcordFeatures) o;
        return Objects.equals(features, that.features);
    }

    @Override
    public int hashCode() {
        return Objects.hash(features);
    }
    
    @Nullable
    public static ConcordFeatures getOrNull(ServerPlayer player) {
        return player.connection.getConnection().channel().attr(CHANNEL_ATTRIBUTE_KEY).get();
    }
    
    public static ConcordFeatures getOrEmpty(ServerPlayer player) {
        return Optional.ofNullable(getOrNull(player)).orElse(EMPTY);
    }
}
