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

package dev.sciwhiz12.concord.server;

import dev.sciwhiz12.concord.dto.*;
import dev.sciwhiz12.concord.msg.MemberStatus;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.messages.MessageSnapshot;
import net.dv8tion.jda.api.entities.sticker.StickerItem;
import net.minecraft.Optionull;

/// Utilities to adapt from JDA objects to Concord DTOs.
public final class JdaAdaptor {
    private JdaAdaptor() {
    }

    public static DiscordFullMessage adapt(Message message) {
        return new DiscordFullMessage(
                message.getIdLong(),
                Optionull.map(message.getMember(), JdaAdaptor::adapt),
                message.getContentDisplay(),
                message.getStickers().stream().map(JdaAdaptor::adapt).toList(),
                message.getAttachments().stream().map(JdaAdaptor::adapt).toList(),
                message.getMessageSnapshots().stream().map(JdaAdaptor::adapt).toList()
        );
    }

    public static DiscordMessageSnapshot adapt(MessageSnapshot snapshot) {
        return new DiscordMessageSnapshot(
                snapshot.getContentRaw(),
                snapshot.getStickers().stream().map(JdaAdaptor::adapt).toList(),
                snapshot.getAttachments().stream().map(JdaAdaptor::adapt).toList()
        );
    }

    public static DiscordMessage.Sticker adapt(StickerItem sticker) {
        return new DiscordMessage.Sticker(
                sticker.getName(),
                sticker.getIconUrl()
        );
    }

    public static DiscordMessage.Attachment adapt(Message.Attachment attachment) {
        return new DiscordMessage.Attachment(
                attachment.getFileName(),
                attachment.getUrl()
        );
    }

    public static DiscordMember adapt(Member member) {
        return new DiscordMember(
                member.getIdLong(),
                member.getEffectiveName(),
                member.getColors().getPrimaryRaw(),
                member.isOwner(),
                adaptStatus(member),
                member.getRoles().stream().map(JdaAdaptor::adapt).toList()
        );
    }

    public static MemberStatus adaptStatus(Member member) {
        return switch (member.getOnlineStatus()) {
            case OnlineStatus.ONLINE -> MemberStatus.ONLINE;
            case OnlineStatus.IDLE -> MemberStatus.IDLE;
            case OnlineStatus.INVISIBLE, OnlineStatus.OFFLINE -> MemberStatus.OFFLINE;
            case OnlineStatus.DO_NOT_DISTURB ->
                    member.getActivities().stream().anyMatch(act -> act.getType() == Activity.ActivityType.STREAMING)
                            ? MemberStatus.STREAMING : MemberStatus.DO_NOT_DISTURB;
            default -> MemberStatus.UNKNOWN;
        };
    }

    public static DiscordRole adapt(Role role) {
        return new DiscordRole(
                role.getIdLong(),
                role.getName(),
                role.getColors().getPrimaryRaw(),
                role.isHoisted(),
                role.hasPermission(Permission.ADMINISTRATOR),
                role.isPublicRole()
        );
    }
}
