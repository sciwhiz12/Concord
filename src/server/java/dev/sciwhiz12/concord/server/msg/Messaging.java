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

package dev.sciwhiz12.concord.server.msg;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import dev.sciwhiz12.concord.ConcordConfig;
import dev.sciwhiz12.concord.dto.DiscordMember;
import dev.sciwhiz12.concord.features.ConcordFeatures;
import dev.sciwhiz12.concord.features.FeatureVersion;
import dev.sciwhiz12.concord.msg.MessageFormatter;
import dev.sciwhiz12.concord.server.ChatBot;
import dev.sciwhiz12.concord.server.JdaAdaptor;
import dev.sciwhiz12.concord.util.IntelligentTranslator;
import dev.sciwhiz12.concord.util.Translation;
import dev.sciwhiz12.concord.util.TranslationUtil;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReference;
import net.minecraft.Optionull;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.ChatVisiblity;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;

import static dev.sciwhiz12.concord.Concord.MODID;

public class Messaging {
    public static final FontDescription.Resource ICONS_FONT = new FontDescription.Resource(Identifier.fromNamespaceAndPath(MODID, "icons"));
    public static final TextColor CROWN_COLOR = TextColor.fromRgb(0xfaa61a);

    private final ChatBot bot;
    // Using concurrent queues because messages may added by different threads
    private final Queue<MessageEntry> messageQueue = new ConcurrentLinkedQueue<>();
    private volatile boolean processMessages = false;

    public Messaging(ChatBot bot) {
        this.bot = bot;
    }

    @CanIgnoreReturnValue
    public CompletableFuture<Message> sendSystemMessage(String message) {
        return sendSystemMessage(Component.literal(message));
    }

    @CanIgnoreReturnValue
    public CompletableFuture<Message> sendSystemMessage(Component message) {
        final DiscordBoundSystem entry = new DiscordBoundSystem(message);
        messageQueue.add(entry);
        return entry.future;
    }

    @CanIgnoreReturnValue
    public CompletableFuture<Message> sendPlayerMessage(ServerPlayer player, Component message) {
        final DiscordBoundPlayer entry = new DiscordBoundPlayer(player, message);
        messageQueue.add(entry);
        return entry.future;
    }

    @CanIgnoreReturnValue
    public CompletableFuture<@Nullable Void> sendDiscordMessage(Member sender, Message message) {
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
            switch (entry) {
                case MinecraftBound d2m -> {
                    this.sendToAllPlayers(d2m.member, d2m.message);
                    d2m.future.complete(null);
                }
                case DiscordBoundSystem m2dS -> {
                    final CompletableFuture<Message> future = m2dS.future;
                    this.bot.getChatForwarder().forwardSystemMessage(m2dS.message.getString())
                            .whenComplete((message, throwable) -> {
                                if (message != null) {
                                    this.bot.getSentMessageMemory().rememberSystemMessage(message.getIdLong(), m2dS.message);
                                    future.complete(message);
                                } else {
                                    future.completeExceptionally(throwable);
                                }
                            });
                }
                case DiscordBoundPlayer m2dP -> {
                    final CompletableFuture<Message> future = m2dP.future;
                    this.bot.getChatForwarder().forwardPlayerMessage(m2dP.player, m2dP.message)
                            .whenComplete((message, throwable) -> {
                                if (message != null) {
                                    this.bot.getSentMessageMemory().rememberPlayerMessage(message.getIdLong(), m2dP.player.getGameProfile(), m2dP.message);
                                    future.complete(message);
                                } else {
                                    future.completeExceptionally(throwable);
                                }
                            });
                }
            }
        }
    }

    private void sendToAllPlayers(Member member, Message message) {
        final ConcordConfig.CrownVisibility crownVisibility = ConcordConfig.HIDE_CROWN.get();

        final IntelligentTranslator<MessageContext> translator = versionCheckingTranslator(
                ctx -> {
                    var componentMessage = JdaAdaptor.adapt(message);
                    var replyMessage = Optional.ofNullable(message.getMessageReference()).map(MessageReference::getMessage).map(JdaAdaptor::adapt).orElse(null);

                    if (!ConcordConfig.HIDE_ROLES.get()) {
                        componentMessage = componentMessage.withMember(DiscordMember::withoutRoles);
                        replyMessage = Optionull.map(replyMessage, m -> m.withMember(DiscordMember::withoutRoles));
                    }

                    componentMessage = componentMessage.withMember(m -> m.withOwner(m.owner() && MessageFormatter.shouldShowCrown(crownVisibility, m)));
                    replyMessage = Optionull.map(replyMessage, msg -> msg.withMember(m -> m.withOwner(m.owner() && MessageFormatter.shouldShowCrown(crownVisibility, m))));

                    return MessageFormatter.createMessage(
                            ctx.useIcons,
                            uuid -> {
                                ServerPlayer player = bot.getServer().getPlayerList().getPlayer(uuid);
                                if (player != null) {
                                    return player.getDisplayName().copy();
                                }
                                return null;
                            },
                            componentMessage,
                            replyMessage == null ? null : MessageFormatter.ReplyContext.from(replyMessage, bot.getSentMessageMemory())
                    );
                });

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

    public Set<Message.MentionType> getAllowedMentions() {
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
                                 CompletableFuture<@Nullable Void> future) implements MessageEntry {
        MinecraftBound(Member member, Message message) {
            this(member, message, new CompletableFuture<>());
        }
    }

    static record DiscordBoundSystem(Component message, CompletableFuture<Message> future) implements MessageEntry {
        DiscordBoundSystem(Component message) {
            this(message, new CompletableFuture<>());
        }
    }

    static record DiscordBoundPlayer(ServerPlayer player, Component message,
                                     CompletableFuture<Message> future) implements MessageEntry {
        DiscordBoundPlayer(ServerPlayer player, Component message) {
            this(player, message, new CompletableFuture<>());
        }
    }

    private static final DefaultArtifactVersion ZERO_VERSION = new DefaultArtifactVersion("0.0.0");

    static ArtifactVersion getFeatureVersionWithDefault(ServerPlayer player, FeatureVersion feature) {
        final ArtifactVersion version = ConcordFeatures.getOrEmpty(player).getFeature(feature);
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
            final Translation translation = TranslationUtil.findTranslation(originalKey);
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
