package tk.sciwhiz12.concord.msg;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.minecraft.Util;
import net.minecraft.network.chat.BaseComponent;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.game.ClientboundChatPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.util.Lazy;
import tk.sciwhiz12.concord.ConcordConfig;
import tk.sciwhiz12.concord.ModPresenceTracker;
import tk.sciwhiz12.concord.util.TranslationUtil;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;

import static net.minecraft.ChatFormatting.DARK_GRAY;
import static net.minecraft.ChatFormatting.WHITE;
import static tk.sciwhiz12.concord.Concord.MODID;

public class Messaging {
    public static final ResourceLocation ICONS_FONT = new ResourceLocation(MODID, "icons");
    public static final TextColor CROWN_COLOR = TextColor.fromRgb(0xfaa61a);

    public static TranslatableComponent createMessage(boolean useIcons, ConcordConfig.CrownVisibility crownVisibility, Member member, String message) {
        final MutableComponent hover = createUserHover(useIcons, crownVisibility, member);

        final List<Role> roles = member.getRoles().stream()
            .filter(((Predicate<Role>) Role::isPublicRole).negate())
            .toList();
        if (!roles.isEmpty()) {
            hover.append("\n").append(new TranslatableComponent("chat.concord.hover.roles"));
            for (int i = 0, rolesSize = roles.size(); i < rolesSize; i++) {
                if (i != 0) hover.append(", "); // add joiner for more than one role
                Role role = roles.get(i);
                hover.append(new TextComponent(role.getName())
                    .withStyle(style -> style.withColor(TextColor.fromRgb(role.getColorRaw())))
                );
            }
        }

        final String name = member.getNickname() != null ? member.getNickname() : member.getUser().getName();

        TranslatableComponent result = new TranslatableComponent("chat.concord.header",
            new TextComponent(name)
                .withStyle(style -> style
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover))
                    .withColor(TextColor.fromRgb(member.getColorRaw()))),
            new TextComponent(message).withStyle(WHITE)
        );
        result.withStyle(DARK_GRAY);
        return result;
    }

    public static MutableComponent createUserHover(boolean useIcons, ConcordConfig.CrownVisibility crownVisibility, Member member) {
        final MemberStatus status = MemberStatus.from(member);

        final boolean showCrown = switch (crownVisibility) {
            case ALWAYS -> member.isOwner(); // Always show for the owner
            case NEVER -> false; // Never show
            case WITHOUT_ADMINISTRATORS -> member.isOwner() // Show if owner and there are no hoisted Admin roles
                && member.getGuild().getRoleCache().streamUnordered()
                .noneMatch(role -> role.isHoisted() && role.hasPermission(Permission.ADMINISTRATOR));
            // TODO: cache the result of the above stream
        };

        final MutableComponent ownerText = new TextComponent(showCrown ? MemberStatus.CROWN_ICON + " " : "")
            .withStyle(style -> style.withColor(CROWN_COLOR));
        final MutableComponent statusText = new TextComponent(String.valueOf(status.getIcon()))
            .withStyle(style -> style.withColor(status.getColor()));

        // Use Concord icon font if configured and told to do so
        if (ConcordConfig.USE_CUSTOM_FONT.get() && useIcons) {
            ownerText.withStyle(style -> style.withFont(ICONS_FONT));
            statusText.withStyle(style -> style.withFont(ICONS_FONT));
        }

        return new TranslatableComponent("chat.concord.hover.header",
            new TextComponent(member.getUser().getName()).withStyle(WHITE),
            new TextComponent(member.getUser().getDiscriminator()).withStyle(WHITE),
            ownerText,
            statusText,
            new TranslatableComponent(status.getTranslationKey())
                .withStyle(style -> style.withColor(status.getColor()))
        ).withStyle(DARK_GRAY);
    }

    public static void sendToAllPlayers(MinecraftServer server, Member member, String message) {
        final ConcordConfig.CrownVisibility crownVisibility = ConcordConfig.HIDE_CROWN.get();

        Lazy<TranslatableComponent> withIcons = Lazy.of(() -> createMessage(true, crownVisibility, member, message));
        TranslatableComponent withoutIcons = createMessage(false, crownVisibility, member, message);

        final boolean lazyTranslate = ConcordConfig.LAZY_TRANSLATIONS.get();
        final boolean useIcons = ConcordConfig.USE_CUSTOM_FONT.get();

        server.sendMessage(withoutIcons, Util.NIL_UUID);

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            BaseComponent sendingText;
            if ((lazyTranslate || useIcons) && ModPresenceTracker.isModPresent(player)) {
                TranslatableComponent translate = useIcons ? withIcons.get() : withoutIcons;
                sendingText = lazyTranslate ? translate : TranslationUtil.eagerTranslate(translate);
            } else {
                sendingText = TranslationUtil.eagerTranslate(withoutIcons);
            }
            player.connection.send(new ClientboundChatPacket(sendingText, ChatType.SYSTEM, Util.NIL_UUID));
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
