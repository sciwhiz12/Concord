package sciwhiz12.concord;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public class ConcordConfig {
    public static boolean ENABLE_INTEGRATED = false;

    public static String TOKEN = "";
    public static String GUILD_ID = "";
    public static String CHANNEL_ID = "";

    public static boolean USE_CUSTOM_FONT = true;
    public static boolean LAZY_TRANSLATIONS = true;

    public static boolean SERVER_START = true;
    public static boolean SERVER_STOP = true;
    public static boolean BOT_START = true;
    public static boolean BOT_STOP = false;

    public static boolean PLAYER_JOIN = true;
    public static boolean PLAYER_LEAVE = true;
    public static boolean PLAYER_DEATH = true;
    public static boolean PLAYER_ADV_TASK = true;
    public static boolean PLAYER_ADV_CHALLENGE = true;
    public static boolean PLAYER_ADV_GOAL = true;

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, Spec.CONFIG_SPEC);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(ConcordConfig::onConfigEvent);
    }

    static void bakeConfigValues() {
        ENABLE_INTEGRATED = Spec.ENABLE_INTEGRATED.get();

        TOKEN = Spec.TOKEN.get();
        GUILD_ID = Spec.GUILD_ID.get();
        CHANNEL_ID = Spec.CHANNEL_ID.get();

        USE_CUSTOM_FONT = Spec.USE_CUSTOM_FONT.get();
        LAZY_TRANSLATIONS = Spec.LAZY_TRANSLATIONS.get();

        SERVER_START = Spec.SERVER_START.get();
        SERVER_STOP = Spec.SERVER_STOP.get();
        BOT_START = Spec.BOT_START.get();
        BOT_STOP = Spec.BOT_STOP.get();

        PLAYER_JOIN = Spec.PLAYER_JOIN.get();
        PLAYER_LEAVE = Spec.PLAYER_LEAVE.get();
        PLAYER_DEATH = Spec.PLAYER_DEATH.get();
        PLAYER_ADV_TASK = Spec.PLAYER_ADV_TASK.get();
        PLAYER_ADV_CHALLENGE = Spec.PLAYER_ADV_CHALLENGE.get();
        PLAYER_ADV_GOAL = Spec.PLAYER_ADV_GOAL.get();
    }

    public static void onConfigEvent(ModConfig.ModConfigEvent event) {
        if (event.getConfig().getSpec() == Spec.CONFIG_SPEC) {
            bakeConfigValues();
        }
    }

    static class Spec {
        static final ForgeConfigSpec CONFIG_SPEC;

        public static final ForgeConfigSpec.BooleanValue ENABLE_INTEGRATED;

        public static final ForgeConfigSpec.ConfigValue<String> TOKEN;
        public static final ForgeConfigSpec.ConfigValue<String> GUILD_ID;
        public static final ForgeConfigSpec.ConfigValue<String> CHANNEL_ID;

        public static final ForgeConfigSpec.BooleanValue USE_CUSTOM_FONT;
        public static final ForgeConfigSpec.BooleanValue LAZY_TRANSLATIONS;

        public static final ForgeConfigSpec.BooleanValue SERVER_START;
        public static final ForgeConfigSpec.BooleanValue SERVER_STOP;
        public static final ForgeConfigSpec.BooleanValue BOT_START;
        public static final ForgeConfigSpec.BooleanValue BOT_STOP;

        public static final ForgeConfigSpec.BooleanValue PLAYER_JOIN;
        public static final ForgeConfigSpec.BooleanValue PLAYER_LEAVE;
        public static final ForgeConfigSpec.BooleanValue PLAYER_DEATH;
        public static final ForgeConfigSpec.BooleanValue PLAYER_ADV_TASK;
        public static final ForgeConfigSpec.BooleanValue PLAYER_ADV_CHALLENGE;
        public static final ForgeConfigSpec.BooleanValue PLAYER_ADV_GOAL;


        static {
            ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

            ENABLE_INTEGRATED = builder
                .comment("Whether the Discord integration is default enabled for integrated servers (i.e. singleplayer).",
                    "You can use the concord commands to force-enable discord integration for a session, if needed.")
                .define("enable_integrated", false);
            {
                builder.comment("Discord connection settings").push("discord");

                TOKEN = builder.comment("The token for the bot application.",
                    "If empty, the Discord integration will not be enabled.")
                    .define("token", "");
                GUILD_ID = builder.comment("The snowflake ID of the guild where this bot belongs to.",
                    "If empty, the Discord integration will not be enabled.")
                    .define("guild_id", "");
                CHANNEL_ID = builder.comment("The snowflake ID of the channel where this bot will post and receive messages.",
                    "If empty, the Discord integration will not be enabled.")
                    .define("channel_id", "");

                builder.pop();
            }
            {
                builder.comment("Message settings").push("messages");

                USE_CUSTOM_FONT = builder.comment("Use the custom `concord:icons` icons font (e.g owner crown) when possible.",
                    "If true, clients with the mod will use the custom icons font.",
                    "Set to false if you cannot ensure that all clients will have the mod installed.")
                    .define("use_custom_font", true);

                LAZY_TRANSLATIONS = builder.comment("Lazily translate the messages when possible.",
                    "This requires the clients have a resource pack with the messages, else they will render weirdly.",
                    "If false, all translation keys will be translated on the server.",
                    "If true, translation keys will translated on the server only if the client does not have the mod " +
                        "installed.",
                    "Set to false if you cannot ensure that all clients will have the mod installed.")
                    .define("lazy_translate", true);

                builder.pop();
            }
            {
                builder.comment("Game notification settings",
                    "Each setting controls a specific game to Discord notification message.")
                    .push("notify");

                SERVER_STOP = builder.comment("Complete startup of server",
                    "Translation key: message.concord.server.start")
                    .define("server.start", true);
                SERVER_START = builder.comment("Stopping of server.",
                    "Translation key: message.concord.server.stop")
                    .define("server.stop", true);

                BOT_START = builder.comment("Enabling of Discord integration.",
                    "Translation key: message.concord.bot.start")
                    .define("bot.start", false);
                BOT_STOP = builder.comment("Disabling of Discord integration.",
                    "Translation key: message.concord.bot.stop")
                    .define("bot.stop", false);

                PLAYER_JOIN = builder.comment("Player joining the game",
                    "Translation key: message.concord.player.join")
                    .define("player.join", true);
                PLAYER_LEAVE =builder.comment("Player leaving the game",
                    "Translation key: message.concord.player.leave")
                    .define("player.leave", true);
                PLAYER_DEATH = builder.comment("Player death message",
                    "Translation key: message.concord.player.death")
                    .define("player.death", true);

                PLAYER_ADV_TASK = builder.comment("Player completed an normal advancement",
                    "Translation key: message.concord.player.advancement.task")
                    .define("player.adv.task", true);
                PLAYER_ADV_CHALLENGE = builder.comment("Player completed a challenge advancement",
                    "Translation key: message.concord.player.advancement.challenge")
                    .define("player.adv.challenge", true);
                PLAYER_ADV_GOAL = builder.comment("Player completed a goal advancement",
                    "Translation key: message.concord.player.advancement.goal")
                    .define("player.adv.goal", true);

                builder.pop();
            }

            CONFIG_SPEC = builder.build();
        }
    }
}
