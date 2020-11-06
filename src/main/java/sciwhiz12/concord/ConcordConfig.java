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

                LAZY_TRANSLATIONS = builder.comment("Lazily translate the messages wheb possible.",
                    "This requires the clients have a resource pack with the messages, else they will render weirdly.",
                    "If false, all translation keys will be translated on the server.",
                    "If true, translation keys will translated on the server only if the client does not have the mod " +
                        "installed.",
                    "Set to false if you cannot ensure that all clients will have the mod installed.")
                    .define("lazy_translate", true);

                builder.pop();
            }

            CONFIG_SPEC = builder.build();
        }
    }
}
