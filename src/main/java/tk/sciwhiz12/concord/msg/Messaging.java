package tk.sciwhiz12.concord.msg;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
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
import net.minecraftforge.common.util.Lazy;
import tk.sciwhiz12.concord.ConcordConfig;
import tk.sciwhiz12.concord.ModPresenceTracker;
import tk.sciwhiz12.concord.util.TranslationUtil;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static net.minecraft.util.text.TextFormatting.DARK_GRAY;
import static net.minecraft.util.text.TextFormatting.WHITE;
import static tk.sciwhiz12.concord.Concord.MODID;

public class Messaging {
    public static final ResourceLocation ICONS_FONT = new ResourceLocation(MODID, "icons");
    public static final Color CROWN_COLOR = Color.fromRgb(0xfaa61a);

    public static TranslationTextComponent createMessage(boolean useIcons, Member member, String message) {
        final MemberStatus status = MemberStatus.from(member);

        final IFormattableTextComponent ownerText = new StringTextComponent(
            member.isOwner() ? MemberStatus.CROWN_ICON + " " : "")
            .withStyle(style -> style.withColor(CROWN_COLOR));

        final IFormattableTextComponent statusText = new StringTextComponent("" + status.getIcon())
            .withStyle(style -> style.withColor(status.getColor()));

        if (ConcordConfig.USE_CUSTOM_FONT.get() && useIcons) {
            ownerText.withStyle(style -> style.withFont(ICONS_FONT));
            statusText.withStyle(style -> style.withFont(ICONS_FONT));
        }

        final IFormattableTextComponent hover = new TranslationTextComponent("chat.concord.hover.header",
            new StringTextComponent(member.getUser().getName()).withStyle(WHITE),
            new StringTextComponent(member.getUser().getDiscriminator()).withStyle(WHITE),
            ownerText,
            statusText,
            new TranslationTextComponent(status.getTranslationKey())
                .withStyle(style -> style.withColor(status.getColor()))
        ).withStyle(DARK_GRAY);

        final List<Role> roles = member.getRoles().stream()
            .filter(((Predicate<Role>) Role::isPublicRole).negate())
            .collect(Collectors.toList());
        if (!roles.isEmpty()) {
            hover.append("\n").append(new TranslationTextComponent("chat.concord.hover.roles"));
            for (int i = 0, rolesSize = roles.size(); i < rolesSize; i++) {
                if (i != 0) hover.append(", "); // add joiner for more than one role
                Role role = roles.get(i);
                hover.append(new StringTextComponent(role.getName())
                    .withStyle(style -> style.withColor(Color.fromRgb(role.getColorRaw())))
                );
            }
        }

        final String name = member.getNickname() != null ? member.getNickname() : member.getUser().getName();

        TranslationTextComponent result = new TranslationTextComponent("chat.concord.header",
            new StringTextComponent(name)
                .withStyle(style -> style
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover))
                    .withColor(Color.fromRgb(member.getColorRaw()))),
            new StringTextComponent(message).withStyle(WHITE)
        );
        result.withStyle(DARK_GRAY);
        return result;
    }

    public static void sendToAllPlayers(MinecraftServer server, Member member, String message) {
        Lazy<TranslationTextComponent> withIcons = Lazy.of(() -> createMessage(true, member, message));
        TranslationTextComponent withoutIcons = createMessage(false, member, message);

        final boolean lazyTranslate = ConcordConfig.LAZY_TRANSLATIONS.get();
        final boolean useIcons = ConcordConfig.USE_CUSTOM_FONT.get();

        server.sendMessage(withoutIcons, Util.NIL_UUID);

        for (ServerPlayerEntity player : server.getPlayerList().getPlayers()) {
            TextComponent sendingText;
            if ((lazyTranslate || useIcons) && ModPresenceTracker.isModPresent(player)) {
                TranslationTextComponent translate = useIcons ? withIcons.get() : withoutIcons;
                sendingText = lazyTranslate ? translate : TranslationUtil.eagerTranslate(translate);
            } else {
                sendingText = TranslationUtil.eagerTranslate(withoutIcons);
            }
            player.connection.send(new SChatPacket(sendingText, ChatType.SYSTEM, Util.NIL_UUID));
        }
    }

    public static void sendToChannel(JDA discord, CharSequence text) {
        final TextChannel channel = discord.getTextChannelById(ConcordConfig.CHANNEL_ID.get());
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
            channel.sendMessage(text).allowedMentions(allowedMentions).queue();
        }
    }
}
