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

package dev.sciwhiz12.concord.network;

import dev.sciwhiz12.concord.Concord;
import dev.sciwhiz12.concord.features.ConcordFeatures;
import dev.sciwhiz12.concord.features.FeatureVersion;
import net.minecraft.Util;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.OnGameConfigurationEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.IPayloadRegistrar;
import org.apache.maven.artifact.versioning.ArtifactVersion;

import java.util.Arrays;
import java.util.Map;

public class ConcordNetwork {
    public static void register(IEventBus modBus) {
        modBus.addListener(ConcordNetwork::onRegisterPayloadHandlers);
        modBus.addListener(ConcordNetwork::onGatherPayloads);
    }

    static void onRegisterPayloadHandlers(RegisterPayloadHandlerEvent event) {
        final IPayloadRegistrar registrar = event.registrar(Concord.MODID)
                .optional();

        registrar.configuration(FeaturesPayload.ID, FeaturesPayload::read,
                handlers -> handlers
                        .client(ConcordNetwork::handleClient)
                        .server(ConcordNetwork::handleServer));
    }

    static void onGatherPayloads(OnGameConfigurationEvent event) {
        event.register(new FeaturesTask(event.getListener()));
    }

    static void handleClient(FeaturesPayload payload, IPayloadContext context) {
        // Received the payload from the server, so we know the server has Concord enabled
        // Send back our own payload, to inform the server that we, the client, have Concord enabled too
        final Map<String, ArtifactVersion> features = Arrays.stream(FeatureVersion.values())
                .map(f -> Map.entry(f.featureName(), f.currentVersion()))
                .collect(Util.toMap());
        context.replyHandler().send(new FeaturesPayload(features));

        // In the future, we can use the info from the payload to decide on what we are going to do
        // For now, the features payload is a 'ping' for server->client
    }

    static void handleServer(FeaturesPayload payload, IPayloadContext context) {
        // The client sent back the features payload, so it has Concord enabled
        // Store the features info it sent
        context.player().ifPresent(player ->
                player.setData(ConcordFeatures.ATTACHMENT, new ConcordFeatures(payload.features())));
    }
}
