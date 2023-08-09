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

package tk.sciwhiz12.concord.msg;

import com.mojang.authlib.GameProfile;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReference;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.sticker.StickerItem;
import net.minecraft.ChatFormatting;
import net.minecraft.locale.Language;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.ChatVisiblity;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import tk.sciwhiz12.concord.*;
import tk.sciwhiz12.concord.util.IntelligentTranslator;
import tk.sciwhiz12.concord.util.Translation;
import tk.sciwhiz12.concord.util.TranslationUtil;
import tk.sciwhiz12.concord.util.Translations;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;

import static net.minecraft.ChatFormatting.*;
import static tk.sciwhiz12.concord.Concord.*;

public class Messaging {
    public static final ResourceLocation ICONS_FONT = new ResourceLocation(MODID, "icons");
    public static final TextColor CROWN_COLOR = TextColor.fromRgb(0xfaa61a);

    private final ChatBot bot;

    public Messaging(ChatBot bot) {
        this.bot = bot;
    }

    private MutableComponent createUserComponent(boolean useIcons, ConcordConfig.CrownVisibility crownVisibility,
                                                 boolean showRoles, Member member, @Nullable MutableComponent replyMessage) {
        final MutableComponent hover = createUserHover(useIcons, crownVisibility, member);

        if (showRoles) {
            final List<Role> roles = member.getRoles().stream()
                    .filter(((Predicate<Role>) Role::isPublicRole).negate())
                    .toList();
            if (!roles.isEmpty()) {
                hover.append("\n").append(Translations.HOVER_ROLES.component());
                for (int i = 0, rolesSize = roles.size(); i < rolesSize; i++) {
                    if (i != 0) hover.append(", "); // add joiner for more than one role
                    Role role = roles.get(i);
                    hover.append(Component.literal(role.getName())
                            .withStyle(style -> style.withColor(TextColor.fromRgb(role.getColorRaw())))
                    );
                }
            }
        }

        if (replyMessage != null) {
            hover.append("\n")
                    .append(Translations.HOVER_REPLY.component(
                                    replyMessage.withStyle(WHITE))
                            .withStyle(GRAY)
                    );
        }

        return Component.literal(member.getEffectiveName())
                .withStyle(style -> style
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover))
                        .withColor(TextColor.fromRgb(member.getColorRaw())));
    }

    private MutableComponent createMessage(boolean useIcons, ConcordConfig.CrownVisibility crownVisibility,
                                           Member member, Message message) {
        final MessageReference reference = message.getMessageReference();
        final boolean showRoles = !ConcordConfig.HIDE_ROLES.get();
        final MutableComponent userComponent = createUserComponent(useIcons, crownVisibility, showRoles, member, null);
        MutableComponent text = createContentComponent(message);

        if (reference != null) {
            final Message referencedMessage = reference.getMessage();
            if (referencedMessage != null) {
                MutableComponent referencedUserComponent = null;

                final Member referencedMember = referencedMessage.getMember();
                if (referencedMember != null) {
                    referencedUserComponent = createUserComponent(useIcons, crownVisibility, showRoles, referencedMember,
                            createContentComponent(referencedMessage));
                }

                final SentMessageMemory.RememberedMessage memory = bot.getSentMessageMemory().findMessage(referencedMessage.getIdLong());
                if (memory != null) {
                    final GameProfile playerProfile = memory.player();
                    final ServerPlayer player = bot.getServer().getPlayerList().getPlayer(playerProfile.getId());
                    if (player != null) {
                        referencedUserComponent = player.getDisplayName().copy();
                    } else {
                        referencedUserComponent = Component.literal(playerProfile.getName()).withStyle(ITALIC);
                    }
                    referencedUserComponent = referencedUserComponent
                            .withStyle(style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, memory.message())));
                }

                if (referencedUserComponent == null) {
                    // Fallback to an unknown user
                    referencedUserComponent = Translations.CHAT_REPLY_UNKNOWN.component()
                            .withStyle(style -> style.withHoverEvent(
                                    new HoverEvent(HoverEvent.Action.SHOW_TEXT, createContentComponent(referencedMessage))));
                }

                text = Translations.CHAT_REPLY_USER.component(referencedUserComponent)
                        .withStyle(ChatFormatting.GRAY)
                        .append(text);
            }
        }

        MutableComponent result = Translations.CHAT_HEADER.component(userComponent, text);
        result.withStyle(DARK_GRAY);
        return result;
    }

    private MutableComponent createContentComponent(Message message) {
        final String content = message.getContentDisplay();
        final MutableComponent text = FormattingUtilities.processCustomFormatting(content);

        boolean skipSpace = content.length() <= 0 || Character.isWhitespace(content.codePointAt(content.length() - 1));
        for (StickerItem sticker : message.getStickers()) {
            // Ensures a space between stickers, and a space between message and first sticker (whether added by
            // us or from the message)
            if (!skipSpace) {
                text.append(" ");
            }
            skipSpace = false;

            MutableComponent stickerComponent = Translations.CHAT_STICKER.component(sticker.getName());
            stickerComponent = ComponentUtils.wrapInSquareBrackets(stickerComponent);
            stickerComponent.withStyle(ChatFormatting.LIGHT_PURPLE);

            text.append(stickerComponent);
        }

        for (Message.Attachment attachment : message.getAttachments()) {
            // Ensures a space between attachments, and a space between message and first attachment (whether added by
            // us or from the message)
            if (!skipSpace) {
                text.append(" ");
            }
            skipSpace = false;

            final String extension = attachment.getFileExtension();
            MutableComponent attachmentComponent;
            if (extension != null) {
                attachmentComponent = Translations.CHAT_ATTACHMENT_WITH_EXTENSION.component(extension);
            } else {
                attachmentComponent = Translations.CHAT_ATTACHMENT_WITH_EXTENSION.component();
            }
            attachmentComponent = ComponentUtils.wrapInSquareBrackets(attachmentComponent);
            attachmentComponent.withStyle(AQUA);

            final MutableComponent attachmentHoverComponent = Component.literal("");
            attachmentHoverComponent.append(
                    Translations.HOVER_ATTACHMENT_FILENAME.component(
                                    Component.literal(attachment.getFileName()).withStyle(WHITE))
                            .withStyle(GRAY)
            ).append("\n");
            attachmentHoverComponent.append(Component.literal(attachment.getUrl()).withStyle(DARK_GRAY)).append("\n");
            attachmentHoverComponent.append(Translations.HOVER_ATTACHMENT_CLICK.component());

            attachmentComponent.withStyle(style ->
                    style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, attachmentHoverComponent))
                            .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, attachment.getUrl())));

            text.append(attachmentComponent);
        }

        return text;
    }

    private MutableComponent createUserHover(boolean useIcons, ConcordConfig.CrownVisibility crownVisibility, Member member) {
        final MemberStatus status = MemberStatus.from(member);

        final boolean showCrown = switch (crownVisibility) {
            case ALWAYS -> member.isOwner(); // Always show for the owner
            case NEVER -> false; // Never show
            case WITHOUT_ADMINISTRATORS -> member.isOwner() // Show if owner and there are no hoisted Admin roles
                    && member.getGuild().getRoleCache().streamUnordered()
                    .noneMatch(role -> role.isHoisted() && role.hasPermission(Permission.ADMINISTRATOR));
            // TODO: cache the result of the above stream
        };

        final MutableComponent ownerIcon = Component.literal(String.valueOf(MemberStatus.CROWN_ICON))
                .withStyle(style -> style.withColor(CROWN_COLOR));
        final MutableComponent ownerText = showCrown ? Component.empty().append(ownerIcon).append(" ") : Component.empty();
        final MutableComponent statusIcon = Component.literal(String.valueOf(status.getIcon()))
                .withStyle(style -> style.withColor(status.getColor()));

        // Use Concord icon font if configured and told to do so
        if (ConcordConfig.USE_CUSTOM_FONT.get() && useIcons) {
            ownerIcon.withStyle(style -> style.withFont(ICONS_FONT));
            statusIcon.withStyle(style -> style.withFont(ICONS_FONT));
        }

        return Translations.HOVER_HEADER.component(
                Component.literal(member.getUser().getName()).withStyle(WHITE),
                ownerText,
                statusIcon,
                status.getTranslation().component()
                        .withStyle(style -> style.withColor(status.getColor()))
        ).withStyle(DARK_GRAY);
    }

    public void sendToAllPlayers(Member member, Message message) {
        final ConcordConfig.CrownVisibility crownVisibility = ConcordConfig.HIDE_CROWN.get();

        final IntelligentTranslator<MessageContext> translator = versionCheckingTranslator(
                ctx -> createMessage(ctx.useIcons, crownVisibility, member, message));

        final boolean lazyTranslateAll = ConcordConfig.LAZY_TRANSLATIONS.get();
        final boolean useIconsAll = ConcordConfig.USE_CUSTOM_FONT.get();

        final MinecraftServer server = bot.getServer();
        server.sendSystemMessage(translator.resolve(new MessageContext(false, FeatureVersion.TRANSLATIONS.currentVersion())));

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            final Connection connection = player.connection.connection;

            final ArtifactVersion translationsVersion = lazyTranslateAll
                    ? getFeatureVersionWithDefault(connection, FeatureVersion.TRANSLATIONS)
                    : ZERO_VERSION; // Eagerly translating means use the 0.0.0 version, which is never compatible
            final ArtifactVersion iconsVersion = getFeatureVersionWithDefault(connection, FeatureVersion.ICONS);

            final boolean useIcons = useIconsAll && isCompatible(FeatureVersion.ICONS.currentVersion(), iconsVersion);
            final MessageContext ctx = new MessageContext(useIcons, translationsVersion);

            final Component sendingText = translator.resolve(ctx);
            if (player.getChatVisibility() == ChatVisiblity.FULL) { // See ServerPlayer#acceptsChatMessages()
                player.sendSystemMessage(sendingText);
            }
        }
    }

    public static CompletableFuture<Message> sendToChannel(JDA discord, CharSequence text) {
        final TextChannel channel = discord.getTextChannelById(ConcordConfig.CHAT_CHANNEL_ID.get());
        if (channel != null) {
            Collection<Message.MentionType> allowedMentions = Collections.emptySet();
            if (ConcordConfig.ALLOW_MENTIONS.get()) {
                allowedMentions = EnumSet.noneOf(Message.MentionType.class);
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
            }
            return channel.sendMessage(text).setAllowedMentions(allowedMentions).submit();
        } else {
            LOGGER.error("Failed to retrieve chat channel from JDA channel cache; was the channel deleted?");
            Concord.disable(true);
            return CompletableFuture.failedFuture(new RuntimeException("Failed to retrieve chat channel from JDA channel cache"));
        }
    }

    private static final DefaultArtifactVersion ZERO_VERSION = new DefaultArtifactVersion("0.0.0");
    private static final DefaultArtifactVersion ONE_VERSION = new DefaultArtifactVersion("1.0.0");

    static ArtifactVersion getFeatureVersionWithDefault(Connection connection, FeatureVersion feature) {
        @Nullable ArtifactVersion featureVersion = ConcordNetwork.getFeatureVersionIfExists(connection, feature);
        // If no remote version, but mod exists on remote, then default to 1.0.0
        // This should be removed when we switch to 2.0.0
        if (featureVersion != null) return featureVersion;
        return ConcordNetwork.isModPresent(connection) ? ONE_VERSION : ZERO_VERSION;
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
