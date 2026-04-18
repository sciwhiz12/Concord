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

import com.mojang.authlib.GameProfile;
import dev.sciwhiz12.concord.ConcordConfig;
import dev.sciwhiz12.concord.dto.*;
import dev.sciwhiz12.concord.util.Translations;
import net.minecraft.network.chat.*;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import static dev.sciwhiz12.concord.Concord.MODID;
import static net.minecraft.ChatFormatting.*;

public class MessageFormatter {
    public static final FontDescription.Resource ICONS_FONT = new FontDescription.Resource(Identifier.fromNamespaceAndPath(MODID, "icons"));
    public static final TextColor CROWN_COLOR = TextColor.fromRgb(0xfaa61a);

    static MutableComponent createUserHover(boolean useIcons, ConcordConfig.CrownVisibility crownVisibility, DiscordMember member) {
        final boolean showCrown = switch (crownVisibility) {
            case ALWAYS -> member.owner(); // Always show for the owner
            case NEVER -> false; // Never show
            case WITHOUT_ADMINISTRATORS -> member.owner() // Show if owner and there are no hoisted Admin roles
                    && member.roles().stream().noneMatch(role -> role.hoisted() && role.administrator());
            // TODO: cache the result of the above stream
        };

        final MutableComponent ownerIcon = Component.literal(String.valueOf(MemberStatus.CROWN_ICON))
                .withStyle(style -> style.withColor(CROWN_COLOR));
        final MutableComponent ownerText = showCrown ? Component.empty().append(ownerIcon).append(" ") : Component.empty();
        final MutableComponent statusIcon = Component.literal(String.valueOf(member.status().getIcon()))
                .withStyle(style -> style.withColor(member.status().getColor()));

        // Use Concord icon font if configured and told to do so
        if (ConcordConfig.USE_CUSTOM_FONT.get() && useIcons) {
            ownerIcon.withStyle(style -> style.withFont(ICONS_FONT));
            statusIcon.withStyle(style -> style.withFont(ICONS_FONT));
        }

        return Translations.HOVER_HEADER.component(
                Component.literal(member.name()).withStyle(WHITE),
                ownerText,
                statusIcon,
                member.status().getTranslation().component()
                        .withStyle(style -> style.withColor(member.status().getColor()))
        ).withStyle(DARK_GRAY);
    }

    static MutableComponent createUserComponent(boolean useIcons, ConcordConfig.CrownVisibility crownVisibility,
                                                boolean showRoles, DiscordMember member, @Nullable MutableComponent replyMessage) {
        final MutableComponent hover = createUserHover(useIcons, crownVisibility, member);

        if (showRoles) {
            final List<DiscordRole> roles = member.roles().stream()
                    .filter(((Predicate<DiscordRole>) DiscordRole::publicRole).negate())
                    .toList();
            if (!roles.isEmpty()) {
                hover.append("\n").append(Translations.HOVER_ROLES.component());
                for (int i = 0, rolesSize = roles.size(); i < rolesSize; i++) {
                    if (i != 0) hover.append(", "); // add joiner for more than one role
                    DiscordRole role = roles.get(i);
                    hover.append(Component.literal(role.name())
                            .withStyle(style -> style.withColor(TextColor.fromRgb(role.color())))
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

        return Component.literal(member.name())
                .withStyle(style -> style
                        .withHoverEvent(new HoverEvent.ShowText(hover))
                        .withColor(TextColor.fromRgb(member.color())));
    }

    static MutableComponent createFullContentComponent(DiscordFullMessage message) {
        MutableComponent baseComponent = createContentComponent(message);

        boolean addNewline = !baseComponent.getString(1).isEmpty();
        for (DiscordMessageSnapshot snapshot : message.snapshots()) {
            if (addNewline) {
                baseComponent.append("\n");
            }
            MutableComponent snapshotComponent = Translations.CHAT_FORWARDED_FROM.component();
            snapshotComponent = ComponentUtils.wrapInSquareBrackets(snapshotComponent);
            snapshotComponent.withStyle(GREEN);
            snapshotComponent.append(" ");

            baseComponent.append(snapshotComponent);
            baseComponent.append(createContentComponent(snapshot));
        }

        return baseComponent;
    }

    static MutableComponent createContentComponent(DiscordMessage message) {
        final String content = message.content();
        final MutableComponent text;
        if (ConcordConfig.VEILED_LINKS.get()) {
            text = FormattingUtilities.redactLinks(content);
        } else {
            text = FormattingUtilities.processCustomFormatting(content);
        }

        boolean skipSpace = content.length() <= 0 || Character.isWhitespace(content.codePointAt(content.length() - 1));
        for (DiscordMessage.Sticker sticker : message.stickers()) {
            // Ensures a space between stickers, and a space between message and first sticker (whether added by
            // us or from the message)
            if (!skipSpace) {
                text.append(" ");
            }
            skipSpace = false;

            MutableComponent stickerComponent = Translations.CHAT_STICKER.component(sticker.name());
            stickerComponent = ComponentUtils.wrapInSquareBrackets(stickerComponent);
            stickerComponent.withStyle(LIGHT_PURPLE);

            text.append(stickerComponent);
        }

        for (DiscordMessage.Attachment attachment : message.attachments()) {
            // Ensures a space between attachments, and a space between message and first attachment (whether added by
            // us or from the message)
            if (!skipSpace) {
                text.append(" ");
            }
            skipSpace = false;

            final String extension = attachment.fileExtension();
            MutableComponent attachmentComponent;
            if (extension != null) {
                attachmentComponent = Translations.CHAT_ATTACHMENT_WITH_EXTENSION.component(extension);
            } else {
                // TODO: fix bug!
                attachmentComponent = Translations.CHAT_ATTACHMENT_WITH_EXTENSION.component();
            }
            attachmentComponent = ComponentUtils.wrapInSquareBrackets(attachmentComponent);
            attachmentComponent.withStyle(AQUA);

            final MutableComponent attachmentHoverComponent = Component.literal("");
            attachmentHoverComponent.append(
                    Translations.HOVER_ATTACHMENT_FILENAME.component(
                                    Component.literal(attachment.fileName()).withStyle(WHITE))
                            .withStyle(GRAY)
            ).append("\n");
            attachmentHoverComponent.append(Component.literal(attachment.url()).withStyle(DARK_GRAY)).append("\n");
            attachmentHoverComponent.append(Translations.HOVER_ATTACHMENT_CLICK.component());

            attachmentComponent.withStyle(style ->
                    style.withHoverEvent(new HoverEvent.ShowText(attachmentHoverComponent))
                            .withClickEvent(new ClickEvent.OpenUrl(URI.create(attachment.url())))); // TOOD: wrap URI.create in try-catch

            text.append(attachmentComponent);
        }

        return text;
    }

    @SuppressWarnings("SameParameterValue")
    public static MutableComponent createMessage(boolean useIcons, ConcordConfig.CrownVisibility crownVisibility,
                                          SentMessageMemory messageMemory, DisplayNameResolver displayNameResolver,
                                          DiscordFullMessage message, @Nullable DiscordFullMessage repliedMessage) {
        final boolean showRoles = !ConcordConfig.HIDE_ROLES.get();
        final MutableComponent userComponent = createUserComponent(useIcons, crownVisibility, showRoles, message.member(), null);
        MutableComponent text = createFullContentComponent(message);

        if (repliedMessage != null) {
            MutableComponent referencedUserComponent = null;

            if (repliedMessage.member() != null) {
                referencedUserComponent = createUserComponent(useIcons, crownVisibility, showRoles, repliedMessage.member(),
                        createFullContentComponent(repliedMessage));
            }

            switch (messageMemory.findMessage(repliedMessage.id())) {
                case SentMessageMemory.RememberedMessage.Player player -> {
                    final GameProfile playerProfile = player.player();
                    final MutableComponent resolvedName = displayNameResolver.resolve(playerProfile.id());
                    referencedUserComponent = Objects.requireNonNullElseGet(resolvedName, () -> Component.literal(playerProfile.name()).withStyle(ITALIC)).withStyle(WHITE)
                            .withStyle(style -> style.withHoverEvent(new HoverEvent.ShowText(player.message())));
                }
                case SentMessageMemory.RememberedMessage.System system ->
                        referencedUserComponent = Translations.CHAT_REPLY_SYSTEM.component()
                                .withStyle(style -> style.withHoverEvent(new HoverEvent.ShowText(system.message())));
                case null -> { // no-op
                }
            }

            if (referencedUserComponent == null) {
                // Fallback to an unknown user
                referencedUserComponent = Translations.CHAT_REPLY_UNKNOWN.component()
                        .withStyle(style -> style.withHoverEvent(
                                new HoverEvent.ShowText(createFullContentComponent(repliedMessage))));
            }

            text = Translations.CHAT_REPLY_USER.component(referencedUserComponent)
                    .withStyle(GRAY)
                    .append(text);
        }

        MutableComponent result = Translations.CHAT_HEADER.component(userComponent, text);
        result.withStyle(DARK_GRAY);
        return result;
    }
}
