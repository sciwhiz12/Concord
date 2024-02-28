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
import dev.sciwhiz12.concord.util.Translations;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReference;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.sticker.StickerItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Predicate;

import static net.minecraft.ChatFormatting.*;

class MessageFormatter {
    static MutableComponent createUserHover(boolean useIcons, ConcordConfig.CrownVisibility crownVisibility, Member member) {
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
                .withStyle(style -> style.withColor(Messaging.CROWN_COLOR));
        final MutableComponent ownerText = showCrown ? Component.empty().append(ownerIcon).append(" ") : Component.empty();
        final MutableComponent statusIcon = Component.literal(String.valueOf(status.getIcon()))
                .withStyle(style -> style.withColor(status.getColor()));

        // Use Concord icon font if configured and told to do so
        if (ConcordConfig.USE_CUSTOM_FONT.get() && useIcons) {
            ownerIcon.withStyle(style -> style.withFont(Messaging.ICONS_FONT));
            statusIcon.withStyle(style -> style.withFont(Messaging.ICONS_FONT));
        }

        return Translations.HOVER_HEADER.component(
                Component.literal(member.getUser().getName()).withStyle(WHITE),
                ownerText,
                statusIcon,
                status.getTranslation().component()
                        .withStyle(style -> style.withColor(status.getColor()))
        ).withStyle(DARK_GRAY);
    }

    static MutableComponent createUserComponent(boolean useIcons, ConcordConfig.CrownVisibility crownVisibility,
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

    static MutableComponent createContentComponent(Message message) {
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

    static MutableComponent createMessage(boolean useIcons, ConcordConfig.CrownVisibility crownVisibility,
                                          Member member, SentMessageMemory messageMemory, PlayerList playerList, Message message) {
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

                final SentMessageMemory.RememberedMessage memory = messageMemory.findMessage(referencedMessage.getIdLong());
                if (memory != null) {
                    final GameProfile playerProfile = memory.player();
                    final ServerPlayer player = playerList.getPlayer(playerProfile.getId());
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
}
