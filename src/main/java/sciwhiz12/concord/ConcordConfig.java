package sciwhiz12.concord;

import net.minecraftforge.common.ForgeConfigSpec;

public class ConcordConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.ConfigValue<String> TOKEN;
    public static final ForgeConfigSpec.ConfigValue<String> GUILD_ID;
    public static final ForgeConfigSpec.ConfigValue<String> CHANNEL_ID;

    public static final ForgeConfigSpec.BooleanValue USE_CUSTOM_FONT;
    public static final ForgeConfigSpec.BooleanValue LAZY_TRANSLATIONS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

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

            USE_CUSTOM_FONT = builder.comment("Use the custom font `concord:icons` for the icons (status, owner crown).",
                    "This requires the clients have a resource pack with the custom font, else the icons will render weirdly.",
                    "(this mod, if installed on the client, will provide the custom icons; another option is a server pack)",
                    "Set to false if you cannot ensure that clients will have the mod nor a custom resource pack.")
                    .define("use_custom_font", true);

            LAZY_TRANSLATIONS = builder.comment("Lazily translate the messages (rely on the client to translate the messages).",
                    "This requires the clients have a resource pack with the messages, else they will render weirdly.",
                    "(this mod, if installed on the client, will provide the custom icons; another option is a server pack)",
                    "If set to false, all translation keys will be translated on the server.",
                    "If set to true, translation keys will only be translated on the server if the client is a vanilla client.",
                    "Useful for servers that serve vanilla clients.")
                    .define("lazy_translate", true);

            builder.pop();
        }
        // TODO: Create a class to hold baked config values
        SPEC = builder.build();
    }
}
