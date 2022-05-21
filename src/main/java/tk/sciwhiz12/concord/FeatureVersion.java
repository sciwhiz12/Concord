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
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

/**
 * Concord feature versions. Each constant is linked to a specific feature of Concord, and holds the current version
 * for that feature. By having each feature be versioned separately and communicated between client and server, Concord
 * can dynamically adapt to clients having different versions of Concord installed.
 */
public enum FeatureVersion {
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

    FeatureVersion(String featureName, String currentVersion) {
        this.featureName = featureName;
        this.channelName = new ResourceLocation(Concord.MODID, featureName);
        this.currentVersion = new DefaultArtifactVersion(currentVersion);
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
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "(" + featureName + " feature, current version " + currentVersion + ")";
    }
}
