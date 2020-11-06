package sciwhiz12.concord.msg;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.ServerPlayNetHandler;
import net.minecraft.network.play.server.SChatPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.Color;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraftforge.fml.network.ConnectionType;
import net.minecraftforge.fml.network.NetworkHooks;
import sciwhiz12.concord.ConcordConfig;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static net.minecraft.util.text.TextFormatting.DARK_GRAY;
import static net.minecraft.util.text.TextFormatting.WHITE;
import static sciwhiz12.concord.Concord.MODID;
import static sciwhiz12.concord.MessageUtil.createTranslation;

public class Messaging {
    public static final ResourceLocation ICONS_FONT = new ResourceLocation(MODID, "icons");
    public static final int CROWN_COLOR = 0xfaa61a;

    public static ITextComponent createMessage(boolean lazyTranslate, Member member, String message) {
        final MemberStatus status = MemberStatus.from(member);

        final IFormattableTextComponent ownerText = new StringTextComponent(member.isOwner() ? "\u2606 " : "")
            .modifyStyle(style -> style.setColor(Color.fromInt(CROWN_COLOR)));
        final IFormattableTextComponent statusText = new StringTextComponent("" + status.getIcon())
            .modifyStyle(style -> style.setColor(status.getColor()));

        if (ConcordConfig.USE_CUSTOM_FONT.get()) {
            ownerText.modifyStyle(style -> style.setFontId(ICONS_FONT));
            statusText.modifyStyle(style -> style.setFontId(ICONS_FONT));
        }

        final IFormattableTextComponent hover = createTranslation(lazyTranslate, "chat.concord.hover.header",
            new StringTextComponent(member.getUser().getName()).mergeStyle(WHITE),
            new StringTextComponent(member.getUser().getDiscriminator()).mergeStyle(WHITE),
            ownerText, statusText,
            createTranslation(lazyTranslate, status.getTranslationKey())
                .modifyStyle(style -> style.setColor(status.getColor()))
        ).mergeStyle(DARK_GRAY);

        final List<Role> roles = member.getRoles().stream()
            .filter(((Predicate<Role>) Role::isPublicRole).negate())
            .collect(Collectors.toList());
        if (!roles.isEmpty()) {
            hover.appendString("\n").append(createTranslation(lazyTranslate, "chat.concord.hover.roles"));
            for (int i = 0, rolesSize = roles.size(); i < rolesSize; i++) {
                if (i != 0) hover.appendString(", "); // add joiner for more than one role
                Role role = roles.get(i);
                hover.append(new StringTextComponent(role.getName())
                    .modifyStyle(style -> style.setColor(Color.fromInt(role.getColorRaw())))
                );
            }
        }

        final String name = member.getNickname() != null ? member.getNickname() : member.getUser().getName();

        return createTranslation(lazyTranslate, "chat.concord.header",
            new StringTextComponent(name)
                .modifyStyle(style -> style
                    .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover))
                    .setColor(Color.fromInt(member.getColorRaw()))),
            new StringTextComponent(message).mergeStyle(WHITE)
        ).mergeStyle(DARK_GRAY);
    }

    public static void sendToAllPlayers(MinecraftServer server, Member member, String message) {
        if (!ConcordConfig.LAZY_TRANSLATIONS.get()) {
            server.getPlayerList().func_232641_a_(createMessage(false, member, message), ChatType.CHAT, Util.DUMMY_UUID);
            return;
        }
        final ITextComponent translation = createMessage(true, member, message);
        server.sendMessage(translation, Util.DUMMY_UUID);// TODO: use a custom, hardcoded UUID

        ITextComponent vanilla = null;
        for (ServerPlayerEntity player : server.getPlayerList().getPlayers()) {
            ServerPlayNetHandler connection = player.connection;
            SChatPacket packet;
            if (NetworkHooks.getConnectionType(() -> connection.netManager) == ConnectionType.VANILLA) {
                if (vanilla == null) vanilla = createMessage(false, member, message);
                packet = new SChatPacket(vanilla, ChatType.SYSTEM, Util.DUMMY_UUID);
            } else {
                packet = new SChatPacket(translation, ChatType.SYSTEM, Util.DUMMY_UUID);
            }
            connection.sendPacket(packet);
        }
    }

    public static void sendToChannel(JDA discord, String username, ITextComponent message) {
        final TextChannel channel = discord.getTextChannelById(ConcordConfig.CHANNEL_ID.get());
        if (channel != null) {
            channel.sendMessage(message.getString()).queue();
        }
    }
}
