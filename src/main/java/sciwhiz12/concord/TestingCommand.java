package sciwhiz12.concord;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity.ActivityType;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.ServerPlayNetHandler;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.Color;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.LanguageMap;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.network.ConnectionType;
import net.minecraftforge.fml.network.NetworkHooks;
import net.minecraftforge.server.command.EnumArgument;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

import static net.minecraft.command.Commands.argument;
import static net.minecraft.command.Commands.literal;
import static net.minecraft.util.text.TextFormatting.DARK_GRAY;
import static net.minecraft.util.text.TextFormatting.WHITE;
import static sciwhiz12.concord.Concord.MODID;

@Mod.EventBusSubscriber(modid = MODID, bus = Bus.FORGE)
public class TestingCommand {
    public static final ResourceLocation ICONS_FONT = new ResourceLocation(MODID, "icons");
    public static final int CROWN_COLOR = 0xfaa61a;

    public enum Status {
        ONLINE(OnlineStatus.ONLINE, "chat.concord.status.online", 0x43b581, '\u25cf'),
        IDLE(OnlineStatus.IDLE, "chat.concord.status.idle", 0xfaa61a, '\u263d'),
        DO_NOT_DISTURB(OnlineStatus.DO_NOT_DISTURB, "chat.concord.status.do_not_disturb", 0xf04747, '\u2205'),
        STREAMING(OnlineStatus.DO_NOT_DISTURB, "chat.concord.status.streaming", 0x593695, '\u25b6'),
        OFFLINE(OnlineStatus.OFFLINE, "chat.concord.status.offline", 0x747f8d, '\u25cb'),
        UNKNOWN(OnlineStatus.UNKNOWN, "chat.concord.status.unknown", 0x7c0000, '\u003f');

        private final OnlineStatus discordStatus;
        private final String translationKey;
        private final int colorHex;
        private final char character;

        Status(OnlineStatus discordStatus, String translationKey, int colorHex, char character) {
            this.discordStatus = discordStatus;
            this.translationKey = translationKey;
            this.colorHex = colorHex;
            this.character = character;
        }

        public OnlineStatus getDiscordStatus() {
            return discordStatus;
        }

        public String getTranslationKey() {
            return translationKey;
        }

        public int getHex() {
            return colorHex;
        }

        public Color getColor() {
            return Color.fromInt(colorHex);
        }

        public char getCharacter() {
            return character;
        }

        public static Status from(OnlineStatus status) {
            switch (status) {
                case ONLINE:
                    return ONLINE;
                case IDLE:
                    return IDLE;
                case INVISIBLE:
                case OFFLINE:
                    return OFFLINE;
                case DO_NOT_DISTURB:
                    return DO_NOT_DISTURB;
                case UNKNOWN:
                default:
                    return UNKNOWN;
            }
        }

        public static Status from(Member member) {
            switch (member.getOnlineStatus()) {
                case ONLINE:
                    return ONLINE;
                case IDLE:
                    return IDLE;
                case INVISIBLE:
                case OFFLINE:
                    return OFFLINE;
                case DO_NOT_DISTURB: {
                    if (member.getActivities().stream().anyMatch(act -> act.getType() == ActivityType.STREAMING))
                        return STREAMING;
                    else
                        return DO_NOT_DISTURB;
                }
                case UNKNOWN:
                default:
                    return UNKNOWN;
            }
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(literal("cctest")
                .then(argument("nickname", StringArgumentType.string())
                        .then(argument("username", StringArgumentType.string())
                                .then(argument("discriminator", IntegerArgumentType.integer(0, 9999))
                                        .then(argument("status", EnumArgument.enumArgument(Status.class))
                                                .then(argument("message", StringArgumentType.greedyString())
                                                        .executes(TestingCommand::run)
                                                )
                                        )
                                )
                        )
                )
        );
    }

    private static Member create(String nickname, String username, int discriminator, Status status) {
        final User user = new User(username, String.format("%04d", discriminator));

        final List<Activity> activities = status == Status.STREAMING ?
                ImmutableList.of(new Activity(ActivityType.STREAMING)) :
                ImmutableList.of(new Activity(ActivityType.DEFAULT));

        /* Hardcoded values for testing */
        final List<Role> roles = ImmutableList.of(
                //                new Role("forgegod", 0xe91e63, false),
                //                new Role("Forge Team", 0x3498db, false),
                //                new Role("Contributor", 0x1f8b4c, false),
                //                new Role("Moderator", 0xf1c40f, false),
                new Role("Triage Team", 0x9b59b6, false),
                new Role("superhelper", 0xc27c0e, false),
                //                new Role("$10", 0xff5722, false),
                //                new Role("Patreon Donor", 0xff7043, false),
                new Role("Nitro Booster", 0xb9bbbe, false),
                new Role("@everyone", Role.DEFAULT_COLOR_RAW, true)
        );
        int color = roles.stream().findFirst().map(Role::getColorRaw).orElse(0x3498db);
        boolean owner = false;

        return new Member(roles, activities, color, nickname, owner, status.getDiscordStatus(), user);
    }

    private static int run(CommandContext<CommandSource> ctx) {
        final CommandSource source = ctx.getSource();
        final Member member = create(
                StringArgumentType.getString(ctx, "nickname"),
                StringArgumentType.getString(ctx, "username"),
                IntegerArgumentType.getInteger(ctx, "discriminator"),
                ctx.getArgument("status", Status.class));
        final String message = StringArgumentType.getString(ctx, "message");

        final Status status = Status.from(member);

        final IFormattableTextComponent ownerText = new StringTextComponent(member.isOwner() ? "\u2606 " : "")
                .modifyStyle(style -> style.setColor(Color.fromInt(CROWN_COLOR)));
        final IFormattableTextComponent statusText = new StringTextComponent("" + status.getCharacter())
                .modifyStyle(style -> style.setColor(status.getColor()));
        if (ConcordConfig.USE_CUSTOM_FONT.get()) {
            ownerText.modifyStyle(style -> style.setFontId(ICONS_FONT));
            statusText.modifyStyle(style -> style.setFontId(ICONS_FONT));
        }

        final IFormattableTextComponent hover = createTranslation(source, "chat.concord.hover.header",
                new StringTextComponent(member.getUser().getName()).mergeStyle(WHITE),
                new StringTextComponent(member.getUser().getDiscriminator()).mergeStyle(WHITE),
                ownerText, statusText,
                createTranslation(source, status.getTranslationKey()).modifyStyle(style -> style.setColor(status.getColor()))
        ).mergeStyle(DARK_GRAY);

        // Roles text component
        final List<Role> roles = member.getRoles().stream().filter(((Predicate<Role>) Role::isPublicRole).negate())
                .collect(Collectors.toList());
        if (!roles.isEmpty()) {
            hover.appendString("\n").append(createTranslation(source, "chat.concord.hover.roles"));
            for (int i = 0, rolesSize = roles.size(); i < rolesSize; i++) {
                if (i != 0) hover.appendString(", "); // add joiner for more than one role
                Role role = roles.get(i);
                hover.append(new StringTextComponent(role.getName())
                        .modifyStyle(style -> style.setColor(Color.fromInt(role.getColorRaw())))
                );
            }
        }

        final String name = member.getNickname() != null ? member.getNickname() : member.getUser().getName();
        final IFormattableTextComponent msg =
                createTranslation(source, "chat.concord.header",
                        new StringTextComponent(name)
                                .modifyStyle(style -> style
                                        .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover))
                                        .setColor(Color.fromInt(member.getColorRaw()))),
                        new StringTextComponent(message).mergeStyle(WHITE)
                ).mergeStyle(DARK_GRAY);

        ctx.getSource().sendFeedback(msg, true);
        return 1;
    }

    /* The following classes are stand-ins for JDA's interfaces, for stub/testing purposes. */

    public static class User {
        final String name;
        final String discriminator;

        public User(String name, String discriminator) {
            this.name = name;
            this.discriminator = discriminator;
        }

        String getName() {
            return name;
        }

        String getDiscriminator() {
            return discriminator;
        }
    }

    public static class Member {
        final List<Role> roles;
        final List<Activity> activities;
        final int color;
        @Nullable final String nickname;
        final boolean isOwner;
        final OnlineStatus onlineStatus;
        final User user;

        public Member(List<Role> roles, List<Activity> activities, int color, @Nullable String nickname, boolean isOwner,
                OnlineStatus onlineStatus,
                User user) {
            this.roles = roles;
            this.activities = activities;
            this.color = color;
            this.nickname = nickname;
            this.isOwner = isOwner;
            this.onlineStatus = onlineStatus;
            this.user = user;
        }

        List<Role> getRoles() {
            return roles;
        }

        List<Activity> getActivities() {
            return activities;
        }

        int getColorRaw() {
            return color;
        }

        @Nullable
        String getNickname() {
            return nickname;
        }

        boolean isOwner() {
            return isOwner;
        }

        OnlineStatus getOnlineStatus() {
            return onlineStatus;
        }

        User getUser() {
            return user;
        }
    }

    public static class Role {
        public static final int DEFAULT_COLOR_RAW = 0x1FFFFFF;

        final String name;
        final int color;
        final boolean publicRole;

        public Role(String name, int color, boolean publicRole) {
            this.name = name;
            this.color = color;
            this.publicRole = publicRole;
        }

        String getName() {
            return name;
        }

        int getColorRaw() {
            return color;
        }

        boolean isPublicRole() {
            return publicRole;
        }
    }

    public static class Activity {
        final ActivityType type;

        public Activity(ActivityType type) {
            this.type = type;
        }

        ActivityType getType() {
            return type;
        }
    }

    /* Copied from net.minecraftforge.server.command.TextComponentHelper, and modified to suit our purpose */

    public static TextComponent createTranslation(CommandSource source, final String translation,
            final Object... args) {
        if (ConcordConfig.LAZY_TRANSLATIONS.get() && isVanillaClient(source)) {
            return new StringTextComponent(String.format(LanguageMap.getInstance().func_230503_a_(translation), args));
        }
        return new TranslationTextComponent(translation, args);
    }

    private static boolean isVanillaClient(CommandSource sender) {
        if (sender.getEntity() instanceof ServerPlayerEntity) {
            ServerPlayNetHandler channel = ((ServerPlayerEntity) sender.getEntity()).connection;
            return NetworkHooks.getConnectionType(() -> channel.netManager) == ConnectionType.VANILLA;
        }
        return false;
    }
}
