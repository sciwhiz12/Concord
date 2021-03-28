package tk.sciwhiz12.concord.msg;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.server.SChatPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.Color;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.util.text.event.HoverEvent;
import tk.sciwhiz12.concord.ConcordConfig;
import tk.sciwhiz12.concord.util.MessageUtil;
import tk.sciwhiz12.concord.ModPresenceTracker;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static net.minecraft.util.text.TextFormatting.DARK_GRAY;
import static net.minecraft.util.text.TextFormatting.WHITE;
import static tk.sciwhiz12.concord.Concord.MODID;

public class Messaging {
    public static final ResourceLocation ICONS_FONT = new ResourceLocation(MODID, "icons");
    public static final Color CROWN_COLOR = Color.fromInt(0xfaa61a);

    public static TranslationTextComponent createMessage(boolean useIcons, Member member, String message) {
        final MemberStatus status = MemberStatus.from(member);

        final IFormattableTextComponent ownerText = new StringTextComponent(
            member.isOwner() ? MemberStatus.CROWN_ICON + " " : "")
            .modifyStyle(style -> style.setColor(CROWN_COLOR));

        final IFormattableTextComponent statusText = new StringTextComponent("" + status.getIcon())
            .modifyStyle(style -> style.setColor(status.getColor()));

        if (ConcordConfig.USE_CUSTOM_FONT && useIcons) {
            ownerText.modifyStyle(style -> style.setFontId(ICONS_FONT));
            statusText.modifyStyle(style -> style.setFontId(ICONS_FONT));
        }

        final IFormattableTextComponent hover = new TranslationTextComponent("chat.concord.hover.header",
            new StringTextComponent(member.getUser().getName()).mergeStyle(WHITE),
            new StringTextComponent(member.getUser().getDiscriminator()).mergeStyle(WHITE),
            ownerText,
            statusText,
            new TranslationTextComponent(status.getTranslationKey())
                .modifyStyle(style -> style.setColor(status.getColor()))
        ).mergeStyle(DARK_GRAY);

        final List<Role> roles = member.getRoles().stream()
            .filter(((Predicate<Role>) Role::isPublicRole).negate())
            .collect(Collectors.toList());
        if (!roles.isEmpty()) {
            hover.appendString("\n").appendSibling(new TranslationTextComponent("chat.concord.hover.roles"));
            for (int i = 0, rolesSize = roles.size(); i < rolesSize; i++) {
                if (i != 0) hover.appendString(", "); // add joiner for more than one role
                Role role = roles.get(i);
                hover.appendSibling(new StringTextComponent(role.getName())
                    .modifyStyle(style -> style.setColor(Color.fromInt(role.getColorRaw())))
                );
            }
        }

        final String name = member.getNickname() != null ? member.getNickname() : member.getUser().getName();

        TranslationTextComponent result = new TranslationTextComponent("chat.concord.header",
            new StringTextComponent(name)
                .modifyStyle(style -> style
                    .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover))
                    .setColor(Color.fromInt(member.getColorRaw()))),
            new StringTextComponent(message).mergeStyle(WHITE)
        );
        result.mergeStyle(DARK_GRAY);
        return result;
    }

    public static void sendToAllPlayers(MinecraftServer server, Member member, String message) {
        TranslationTextComponent withIcons = null;
        TranslationTextComponent withoutIcons = createMessage(false, member, message);

        final boolean lazyTranslate = ConcordConfig.LAZY_TRANSLATIONS;
        final boolean useIcons = ConcordConfig.USE_CUSTOM_FONT;

        server.sendMessage(withoutIcons, Util.DUMMY_UUID);

        for (ServerPlayerEntity player : server.getPlayerList().getPlayers()) {
            TextComponent sendingText;
            if ((lazyTranslate || useIcons) && ModPresenceTracker.isModPresent(player)) {
                TranslationTextComponent translate;
                if (useIcons) {
                    if (withIcons == null) withIcons = createMessage(true, member, message);
                    translate = withIcons;
                } else {
                    translate = withoutIcons;
                }
                sendingText = lazyTranslate ? translate : MessageUtil.eagerTranslate(translate);
            } else {
                sendingText = MessageUtil.eagerTranslate(withoutIcons);
            }
            player.connection.sendPacket(new SChatPacket(sendingText, ChatType.SYSTEM, Util.DUMMY_UUID));
        }
    }

    public static void sendToChannel(JDA discord, CharSequence text) {
        final TextChannel channel = discord.getTextChannelById(ConcordConfig.CHANNEL_ID);
        if (channel != null) {
            channel.sendMessage(text).queue();
        }
    }
}
