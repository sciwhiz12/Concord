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

import dev.sciwhiz12.concord.ChatBot;
import dev.sciwhiz12.concord.Concord;
import dev.sciwhiz12.concord.ConcordConfig;
import dev.sciwhiz12.concord.features.ConcordFeatures;
import dev.sciwhiz12.concord.features.FeatureVersion;
import dev.sciwhiz12.concord.util.IntelligentTranslator;
import dev.sciwhiz12.concord.util.Translation;
import dev.sciwhiz12.concord.util.TranslationUtil;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.ChatVisiblity;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;

import static dev.sciwhiz12.concord.Concord.LOGGER;
import static dev.sciwhiz12.concord.Concord.MODID;

public class Messaging {
    public static final ResourceLocation ICONS_FONT = new ResourceLocation(MODID, "icons");
    public static final TextColor CROWN_COLOR = TextColor.fromRgb(0xfaa61a);

    private final ChatBot bot;
    // Using concurrent queues because messages may added by different threads
    private final Queue<MessageEntry> messageQueue = new ConcurrentLinkedQueue<>();
    private volatile boolean processMessages = false;

    public Messaging(ChatBot bot) {
        this.bot = bot;
    }

    public CompletableFuture<Message> sendToDiscord(String message) {
        final DiscordBound entry = new DiscordBound(message);
        messageQueue.add(entry);
        return entry.future;
    }

    public CompletableFuture<Message> sendToDiscord(Component message) {
        return sendToDiscord(message.getString());
    }

    @SuppressWarnings("UnusedReturnValue")
    public CompletableFuture<Void> sendToMinecraft(Member sender, Message message) {
        final MinecraftBound entry = new MinecraftBound(sender, message);
        messageQueue.add(entry);
        return entry.future;
    }

    @ApiStatus.Internal
    public void allowProcessingMessages(boolean processMessages) {
        this.processMessages = processMessages;
    }

    public void processMessages() {
        this.processMessages(false);
    }

    @ApiStatus.Internal
    public void processMessages(boolean bypass) {
        if (!processMessages && !bypass) return;

        // TODO: rate-limiting
        MessageEntry entry;
        while ((entry = messageQueue.poll()) != null) {
            if (entry instanceof MinecraftBound d2m) {
                this.sendToAllPlayers(d2m.member, d2m.message);
                d2m.future.complete(null);
            } else if (entry instanceof DiscordBound m2d) {
                final CompletableFuture<Message> future = m2d.future;
                this.sendToChannel(m2d.message).whenComplete((message, throwable) -> {
                    if (message != null) {
                        future.complete(message);
                    } else {
                        future.completeExceptionally(throwable);
                    }
                });
            }
        }
    }

    private void sendToAllPlayers(Member member, Message message) {
        final ConcordConfig.CrownVisibility crownVisibility = ConcordConfig.HIDE_CROWN.get();

        final IntelligentTranslator<MessageContext> translator = versionCheckingTranslator(
                ctx -> MessageFormatter.createMessage(ctx.useIcons, crownVisibility, member, bot.getSentMessageMemory(), bot.getServer().getPlayerList(), message));

        final boolean lazyTranslateAll = ConcordConfig.LAZY_TRANSLATIONS.get();
        final boolean useIconsAll = ConcordConfig.USE_CUSTOM_FONT.get();

        final MinecraftServer server = bot.getServer();
        server.sendSystemMessage(translator.resolve(new MessageContext(false, FeatureVersion.TRANSLATIONS.currentVersion())));

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            final ArtifactVersion translationsVersion = lazyTranslateAll
                    ? getFeatureVersionWithDefault(player, FeatureVersion.TRANSLATIONS)
                    : ZERO_VERSION; // Eagerly translating means use the 0.0.0 version, which is never compatible
            final ArtifactVersion iconsVersion = getFeatureVersionWithDefault(player, FeatureVersion.ICONS);

            final boolean useIcons = useIconsAll && isCompatible(FeatureVersion.ICONS.currentVersion(), iconsVersion);
            final MessageContext ctx = new MessageContext(useIcons, translationsVersion);

            final Component sendingText = translator.resolve(ctx);
            if (player.getChatVisibility() == ChatVisiblity.FULL) { // See ServerPlayer#acceptsChatMessages()
                player.sendSystemMessage(sendingText);
            }
        }
    }

    private CompletableFuture<Message> sendToChannel(CharSequence text) {
        final TextChannel channel = bot.getDiscord().getTextChannelById(ConcordConfig.CHAT_CHANNEL_ID.get());
        if (channel != null) {
            return channel.sendMessage(text).setAllowedMentions(getAllowedMentions()).submit();
        } else {
            LOGGER.error("Failed to retrieve chat channel from JDA channel cache; was the channel deleted?");
            Concord.disable(true);
            return CompletableFuture.failedFuture(new RuntimeException("Failed to retrieve chat channel from JDA channel cache"));
        }
    }

    private Set<Message.MentionType> getAllowedMentions() {
        if (ConcordConfig.ALLOW_MENTIONS.get()) {
            final Set<Message.MentionType> allowedMentions = EnumSet.noneOf(Message.MentionType.class);
            if (ConcordConfig.ALLOW_PUBLIC_MENTIONS.get()) {
                allowedMentions.add(Message.MentionType.EVERYONE);
                allowedMentions.add(Message.MentionType.HERE);
            }
            if (ConcordConfig.ALLOW_USER_MENTIONS.get()) {
                allowedMentions.add(Message.MentionType.USER);
            }
            if (ConcordConfig.ALLOW_ROLE_MENTIONS.get()) {
                allowedMentions.add(Message.MentionType.ROLE);
            }
            return allowedMentions;
        }
        return Set.of();
    }

    sealed interface MessageEntry {
    }

    static record MinecraftBound(Member member, Message message,
                                 CompletableFuture<Void> future) implements MessageEntry {
        MinecraftBound(Member member, Message message) {
            this(member, message, new CompletableFuture<>());
        }
    }

    static record DiscordBound(String message, CompletableFuture<Message> future) implements MessageEntry {
        DiscordBound(String message) {
            this(message, new CompletableFuture<>());
        }
    }

    private static final DefaultArtifactVersion ZERO_VERSION = new DefaultArtifactVersion("0.0.0");

    static ArtifactVersion getFeatureVersionWithDefault(ServerPlayer player, FeatureVersion feature) {
        final @Nullable ArtifactVersion version = player.getData(ConcordFeatures.ATTACHMENT).getFeature(feature);
        if (version == null) return ZERO_VERSION;
        return version;
    }

    public static boolean isCompatible(ArtifactVersion first, ArtifactVersion second) {
        return first.getMajorVersion() == second.getMajorVersion()
                && first.getMinorVersion() == second.getMinorVersion();
    }

    private IntelligentTranslator<MessageContext> versionCheckingTranslator(
            final Function<MessageContext, MutableComponent> componentCreator) {
        return new IntelligentTranslator<>(componentCreator, ((originalKey, remoteContext) -> {
            @Nullable final Translation translation = TranslationUtil.findTranslation(originalKey);
            if (translation == null) return originalKey; // Non-Concord translation, so skip

            final ArtifactVersion translationVersion = translation.lastModifiedVersion();
            if (isCompatible(remoteContext.version, translationVersion)) {
                // Major and minor match up, so do not eagerly translate
                return originalKey;
            }

            // Major and/or minor do not match up, so eagerly translate
            return Language.getInstance().getOrDefault(translation.key());
        }));
    }

    private record MessageContext(boolean useIcons, ArtifactVersion version) {
    }
}
