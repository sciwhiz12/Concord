/*
 * Concord - Copyright (c) 2020-2022 SciWhiz12
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

package tk.sciwhiz12.concord.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.dv8tion.jda.api.EmbedBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import tk.sciwhiz12.concord.Concord;
import tk.sciwhiz12.concord.ConcordConfig;
import tk.sciwhiz12.concord.ModPresenceTracker;
import tk.sciwhiz12.concord.util.TranslationUtil;
import tk.sciwhiz12.concord.util.Translations;

import static net.minecraft.ChatFormatting.GREEN;
import static net.minecraft.ChatFormatting.RED;
import static net.minecraft.commands.Commands.literal;

public class ConcordCommand {
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        // Concord bot control commands.
        event.getDispatcher().register(
            literal("concord")
                .then(literal("reload")
                    .requires(source -> source.hasPermission(Commands.LEVEL_ADMINS))
                    .executes(ConcordCommand::reload)
                )
                .then(literal("enable")
                    .requires(source -> source.hasPermission(Commands.LEVEL_ADMINS))
                    .executes(ConcordCommand::enable)
                )
                .then(literal("disable")
                    .requires(source -> source.hasPermission(Commands.LEVEL_ADMINS))
                    .executes(ConcordCommand::disable)
                )
                .then(literal("status")
                    .executes(ConcordCommand::status)
                )
        );

        // Discord integration commands.
        // Reports a user to the staff upon request of a user.
        event.getDispatcher().register(
                literal("report")
                        .then(Commands.argument("target", EntityArgument.players())
                            .then(Commands.argument("reason", StringArgumentType.greedyString())
                                    .executes(ConcordCommand::report))
                        )
        );
    }

    public static MutableComponent resolve(CommandSourceStack source, final TranslatableComponent text){
        return !ConcordConfig.LAZY_TRANSLATIONS.get() 
                || (source.getEntity() instanceof ServerPlayer player && ModPresenceTracker.isModPresent(player)) 
                ? text 
                : TranslationUtil.eagerTranslate(text);
    }

    private static int reload(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ctx.getSource().sendSuccess(resolve(source, Translations.COMMAND_ENABLING.component()), true);
        if (Concord.isEnabled()) {
            Concord.disable();
        }
        Concord.enable(source.getServer());
        return Command.SINGLE_SUCCESS;
    }

    private static int enable(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        if (Concord.isEnabled()) {
            ctx.getSource().sendFailure(resolve(source, Translations.COMMAND_ALREADY_ENABLED.component()));
            return Command.SINGLE_SUCCESS;
        }
        ctx.getSource().sendSuccess(resolve(source, Translations.COMMAND_ENABLING.component()), true);
        Concord.enable(source.getServer());
        return Command.SINGLE_SUCCESS;
    }

    private static int disable(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        if (!Concord.isEnabled()) {
            ctx.getSource().sendFailure(resolve(source, Translations.COMMAND_ALREADY_DISABLED.component()));
            return Command.SINGLE_SUCCESS;
        }
        ctx.getSource().sendSuccess(resolve(source, Translations.COMMAND_DISABLING.component()), true);
        Concord.disable();
        return Command.SINGLE_SUCCESS;
    }

    private static int status(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        Component result;
        if (Concord.isEnabled()) {
            result = resolve(source, Translations.COMMAND_STATUS_ENABLED.component()).withStyle(GREEN);
        } else {
            result = resolve(source, Translations.COMMAND_STATUS_DISABLED.component()).withStyle(RED);
        }
        ctx.getSource().sendSuccess(resolve(source, Translations.COMMAND_STATUS_PREFIX.component(result)), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int report(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        // If Concord is disabled for whatever reason, tell the player.
        if (!Concord.isEnabled()) {
            ctx.getSource().sendFailure(
                    createMessage(ctx.getSource(), "command.concord.status",
                            createMessage(ctx.getSource(), "command.concord.status.disabled")
                    ).withStyle(RED));
            return Command.SINGLE_SUCCESS;
        }

        // If reporting is disabled, also tell the user
        if (ConcordConfig.REPORT_CHANNEL_ID.get().isEmpty()) {
            ctx.getSource().sendFailure(
                    createMessage(ctx.getSource(), "command.concord.report.status",
                            createMessage(ctx.getSource(), "command.concord.status.disabled")
                    ).withStyle(RED));
            return Command.SINGLE_SUCCESS;
        }

        var players = EntityArgument.getPlayers(ctx, "target");
        var reason = StringArgumentType.getString(ctx, "reason");
        var bot = Concord.getBot();
        var channel = bot.getDiscord().getTextChannelById(ConcordConfig.REPORT_CHANNEL_ID.get());

        for (ServerPlayer player : players) {
            channel.sendMessageEmbeds(
                    new EmbedBuilder()
                            .setDescription("A user has been reported!")
                            .addField("",
                                    "**" + player.getName().getString() + "** has been reported for " + reason, false)
                            .build()
            ).queue();
        }

        return Command.SINGLE_SUCCESS;
    }
}
