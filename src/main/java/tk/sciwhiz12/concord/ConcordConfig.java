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

package tk.sciwhiz12.concord;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import tk.sciwhiz12.concord.util.Messages;

public class ConcordConfig {
    static final ForgeConfigSpec CONFIG_SPEC;

    public static final ForgeConfigSpec.BooleanValue ENABLE_INTEGRATED;
    public static final ForgeConfigSpec.BooleanValue SAY_COMMAND_HOOK;

    public static final ForgeConfigSpec.ConfigValue<String> TOKEN;
    public static final ForgeConfigSpec.ConfigValue<String> GUILD_ID;
    public static final ForgeConfigSpec.ConfigValue<String> CHAT_CHANNEL_ID;
    public static final ForgeConfigSpec.ConfigValue<String> REPORT_CHANNEL_ID;

    public static final ForgeConfigSpec.BooleanValue USE_CUSTOM_FONT;
    public static final ForgeConfigSpec.BooleanValue LAZY_TRANSLATIONS;
    public static final ForgeConfigSpec.BooleanValue USE_LEGACY_FORMATTING;
    public static final ForgeConfigSpec.BooleanValue USE_CUSTOM_FORMATTING;
    public static final ForgeConfigSpec.EnumValue<CrownVisibility> HIDE_CROWN;

    public static final ForgeConfigSpec.BooleanValue ALLOW_MENTIONS;
    public static final ForgeConfigSpec.BooleanValue ALLOW_PUBLIC_MENTIONS;
    public static final ForgeConfigSpec.BooleanValue ALLOW_USER_MENTIONS;
    public static final ForgeConfigSpec.BooleanValue ALLOW_ROLE_MENTIONS;

    public static final ForgeConfigSpec.BooleanValue SERVER_START;
    public static final ForgeConfigSpec.BooleanValue SERVER_STOP;
    public static final ForgeConfigSpec.BooleanValue BOT_START;
    public static final ForgeConfigSpec.BooleanValue BOT_STOP;

    public static final ForgeConfigSpec.BooleanValue PLAYER_JOIN;
    public static final ForgeConfigSpec.BooleanValue PLAYER_LEAVE;
    public static final ForgeConfigSpec.BooleanValue PLAYER_DEATH;
    public static final ForgeConfigSpec.BooleanValue PLAYER_ADV_GAMERULE;
    public static final ForgeConfigSpec.BooleanValue PLAYER_ADV_TASK;
    public static final ForgeConfigSpec.BooleanValue PLAYER_ADV_CHALLENGE;
    public static final ForgeConfigSpec.BooleanValue PLAYER_ADV_GOAL;

    public static final ForgeConfigSpec.BooleanValue COMMAND_SAY;

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, CONFIG_SPEC);
    }

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        ENABLE_INTEGRATED = builder
                .comment("Whether the Discord integration is default enabled for integrated servers (i.e. singleplayer).",
                        "You can use the concord commands to force-enable discord integration for a session, if needed.")
                .define("enable_integrated", false);

        SAY_COMMAND_HOOK = builder
                .comment("Hook into the /say command by overriding the command node, to intercept messages from this.",
                        "Usually does not cause compatibility issues. Takes effect upon a reload (/reload command).")
                .define("say_command_hook", true);

        {
            builder.comment("Discord connection settings").push("discord");

            TOKEN = builder.comment("The token for the bot application.",
                            "If empty, the Discord integration will not be enabled.")
                    .define("token", "");
            GUILD_ID = builder.comment("The snowflake ID of the guild where this bot belongs to.",
                            "If empty, the Discord integration will not be enabled.")
                    .define("guild_id", "");
            CHAT_CHANNEL_ID = builder.comment("The snowflake ID of the channel where this bot will post and receive messages.",
                            "If empty, the Discord integration will not be enabled.")
                    .define("chat_channel_id", "");
            REPORT_CHANNEL_ID = builder.comment("The snowflake ID of the channel where this bot will post reports from in-game users.",
                            "If empty, reports will be disabled.")
                    .define("report_channel_id", "");

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

            USE_CUSTOM_FORMATTING = builder.comment("Allow Discord users to use Concord Message Formatting Codes in a message.",
                            "This will cause in-game messages to have color formatting.",
                            "To use it, send a message with a dollar sign ($) followed by either an English-language color (ie. $red), or a hex code (ie. $#FF0000).",
                            "Names are delimited by a space which will be consumed, so the string \"this is a $red colored text\" will be shown as \"this is a colored text\".",
                            "Please note that Custom Formatting will override Legacy Formatting when enabled. This is intentional.")
                    .define("use_custom_formatting", false);

            USE_LEGACY_FORMATTING = builder.comment("Allow Discord users to put legacy-style chat formatting (&5, etc) in a message.",
                    "This will cause in-game messages to have color, bold, italic, strikethrough and \"obfuscated\" formatting.",
                    "Note however, that this only works with vanilla formatting codes, and is likely to cause weirdness.")
                    .define("use_legacy_formatting", false);


            HIDE_CROWN = builder.comment("Configures when the Server Owner crown is visible to clients.",
                            "ALWAYS means the crown is always visible, NEVER means the crown is never visible.",
                            "WITHOUT_ADMINISTRATORS means it is only visible when there are no hoisted Administrator roles.")
                    .defineEnum("hide_crown", CrownVisibility.WITHOUT_ADMINISTRATORS);

            builder.pop();
        }

        {
            builder.comment("Mention settings",
                            "Settings for when messages from Concord that contain mentions should cause pings.",
                            "These are only bot-side settings; permissions on the bot user may prevent certain mentions from pinging.")
                    .push("mentions");

            ALLOW_MENTIONS = builder.comment("Allow mentions to cause pings.",
                            "Disabling this setting effectively disables all other settings in this category.")
                    .define("allow_mentions", true);

            ALLOW_PUBLIC_MENTIONS = builder.comment("Allow @everyone and @here mentions to cause pings.")
                    .define("allow_public_mentions", false);

            ALLOW_USER_MENTIONS = builder.comment("Allow user mentions to cause pings.")
                    .define("allow_user_mentions", true);

            ALLOW_ROLE_MENTIONS = builder.comment("Allow role mentions to cause pings.")
                    .define("allow_role_mentions", true);

            builder.pop();
        }

        {
            builder.comment("Game notification settings",
                            "Each setting controls a specific game to Discord notification message.")
                    .push("notify");

            SERVER_STOP = builder.comment("Complete startup of server",
                            "Translation key: " + Messages.SERVER_START.key())
                    .define("server.start", true);
            SERVER_START = builder.comment("Stopping of server.",
                            "Translation key: " + Messages.SERVER_STOP.key())
                    .define("server.stop", true);

            BOT_START = builder.comment("Enabling of Discord integration.",
                            "Translation key: " + Messages.BOT_START.key())
                    .define("bot.start", false);
            BOT_STOP = builder.comment("Disabling of Discord integration.",
                            "Translation key: " + Messages.BOT_STOP.key())
                    .define("bot.stop", false);

            PLAYER_JOIN = builder.comment("Player joining the game",
                            "Translation key: " + Messages.PLAYER_JOIN.key())
                    .define("player.join", true);
            PLAYER_LEAVE = builder.comment("Player leaving the game",
                            "Translation key: " + Messages.PLAYER_LEAVE.key())
                    .define("player.leave", true);
            PLAYER_DEATH = builder.comment("Player death message")
                    .define("player.death", true);

            PLAYER_ADV_GAMERULE = builder.comment("Whether to respect the `announceAdvancements` gamerule",
                            "If true, then the other advancement notifications settings only apply if the gamerule is true.",
                            "If false, the advancement notifications settings always apply.")
                    .define("player.adv.respect_gamerule", true);
            PLAYER_ADV_TASK = builder.comment("Player completed an normal advancement",
                            "Translation key: " + Messages.ADVANCEMENT_TASK.key())
                    .define("player.adv.task", true);
            PLAYER_ADV_CHALLENGE = builder.comment("Player completed a challenge advancement",
                            "Translation key: " + Messages.ADVANCEMENT_CHALLENGE.key())
                    .define("player.adv.challenge", true);
            PLAYER_ADV_GOAL = builder.comment("Player completed a goal advancement",
                            "Translation key: " + Messages.ADVANCEMENT_GOAL.key())
                    .define("player.adv.goal", true);

            COMMAND_SAY = builder.comment("Message from /say command",
                            "Translation key: " + Messages.SAY_COMMAND.key())
                    .define("command.say", true);

            builder.pop();
        }

        CONFIG_SPEC = builder.build();
    }

    /**
     * The visibility of the Server Owner's crown in messages to clients.
     */
    public enum CrownVisibility {
        /**
         * The crown is always visible.
         */
        ALWAYS,
        /**
         * The crown is only visible if there are no hoisted roles with {@link net.dv8tion.jda.api.Permission#ADMINISTRATOR}.
         *
         * <p>This follows the same logic that the official Discord client uses to hide the crown.</p>
         */
        WITHOUT_ADMINISTRATORS,
        /**
         * The crown is never visible.
         */
        NEVER
    }
}
