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
import net.dv8tion.jda.api.entities.TextChannel;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import tk.sciwhiz12.concord.ChatBot;
import tk.sciwhiz12.concord.Concord;
import tk.sciwhiz12.concord.ConcordConfig;
import tk.sciwhiz12.concord.util.Translations;

import java.time.Instant;

import static net.minecraft.ChatFormatting.GREEN;
import static net.minecraft.ChatFormatting.RED;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static tk.sciwhiz12.concord.command.ConcordCommand.resolve;

/**
 * {@code /report} command for reporting players to a configured Discord channel, usually visible to server staff. This
 * allows players to quickly report another player for some specific reason from within the game, without needing to
 * open Discord and manually communicate with a server operator.
 *
 * <p>The configuration setting for the Discord channel is {@link ConcordConfig#REPORT_CHANNEL_ID}. If this setting is 
 * blank, this command is disabled and not registered.</p>
 *
 * @author Curle
 */
public class ReportCommand {
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        if (ConcordConfig.REPORT_CHANNEL_ID.get().isBlank()) return;

        event.getDispatcher().register(
                literal("report")
                        .then(argument("target", EntityArgument.players())
                                .then(argument("reason", StringArgumentType.greedyString())
                                        .executes(ReportCommand::report))
                        )
        );
    }

    private static int report(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        // If Concord is disabled for whatever reason, tell the player.
        if (!Concord.isEnabled()) {
            ctx.getSource().sendFailure(
                    resolve(ctx.getSource(),
                            Translations.COMMAND_REPORT_STATUS
                                    .component(
                                            resolve(ctx.getSource(),
                                                    Translations.COMMAND_STATUS_DISABLED.component()
                                            )
                                    )
                    ).withStyle(RED));
            return Command.SINGLE_SUCCESS;
        }

        final ChatBot bot = Concord.getBot();
        final String channelID = ConcordConfig.REPORT_CHANNEL_ID.get();
        final TextChannel channel = channelID.isBlank() ? null : bot.getDiscord().getTextChannelById(channelID);

        // If reporting is disabled, also tell the user
        if (channel == null) {
            ctx.getSource().sendFailure(
                    resolve(ctx.getSource(),
                            Translations.COMMAND_REPORT_STATUS
                                    .component(
                                            resolve(ctx.getSource(),
                                                    Translations.COMMAND_STATUS_DISABLED.component()
                                            )
                                    )
                    ).withStyle(RED));
            return Command.SINGLE_SUCCESS;
        }

        var players = EntityArgument.getPlayers(ctx, "target");
        var sender = ctx.getSource().getPlayerOrException();
        var reason = StringArgumentType.getString(ctx, "reason");

        if (!players.isEmpty()) {
            var reportedPlayer = (ServerPlayer) players.toArray()[0];

            var reportedName = reportedPlayer.getName().getString();
            var senderName = sender.getName().getString();
            channel.sendMessageEmbeds(
                    new EmbedBuilder()
                            .setAuthor("Concord Integrations")
                            .setColor(0xFF0000)
                            .setDescription("**" + reportedName + "** has been reported by **" + senderName + "**")
                            .addField("Reason", reason, false)
                            .addField("Reported", "**" + reportedName + "** (`" + reportedPlayer.getGameProfile().getId().toString() + "`)\n" +
                                    "Dimension: `" + reportedPlayer.level.dimension().location() + "`\n" +
                                    "XYZ: `" + (int) reportedPlayer.position().x + " " + (int) reportedPlayer.position().y + " " + (int) reportedPlayer.position().z + "`", false)
                            .addField("Reporter", "**" + senderName + "** (`" + sender.getGameProfile().getId().toString() + "`)\n" +
                                    "Dimension: `" + sender.level.dimension().location() + "`\n" +
                                    "XYZ: `" + (int) sender.position().x + " " + (int) sender.position().y + " " + (int) sender.position().z + "`", false)
                            .setTimestamp(Instant.now())
                            .setFooter("Game time: " + sender.level.getGameTime())
                            .build()
            ).queue();

            ctx.getSource().sendSuccess(
                    resolve(ctx.getSource(),
                            Translations.COMMAND_REPORT_SUCCESS
                                    .component(
                                            reportedPlayer.getName()
                                    )
                    ).withStyle(GREEN), true);
        }


        return Command.SINGLE_SUCCESS;
    }
}
