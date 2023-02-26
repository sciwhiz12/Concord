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

package tk.sciwhiz12.concord.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.RegisterCommandsEvent;
import tk.sciwhiz12.concord.ChatBot;
import tk.sciwhiz12.concord.Concord;
import tk.sciwhiz12.concord.ConcordConfig;
import tk.sciwhiz12.concord.util.Translations;

import java.time.Instant;

import static net.dv8tion.jda.api.utils.MarkdownSanitizer.escape;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

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
                        .then(argument("target", EntityArgument.player())
                                .then(argument("reason", StringArgumentType.greedyString())
                                        .executes(ReportCommand::report))
                        )
        );
    }

    private static int report(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        // If Concord is disabled for whatever reason, tell the player.
        if (!Concord.isEnabled()) {
            ctx.getSource().sendFailure(
                    Translations.COMMAND_REPORT_STATUS.resolvedComponent(ctx.getSource(),
                            Translations.COMMAND_STATUS_DISABLED.resolvedComponent(ctx.getSource())
                    ));
            return 0;
        }

        final ChatBot bot = Concord.getBot();
        final String channelID = ConcordConfig.REPORT_CHANNEL_ID.get();
        final GuildMessageChannel channel = channelID.isBlank() ? null : bot.getDiscord().getChannelById(GuildMessageChannel.class, channelID);

        boolean sendingAllowed = false;
        if (channel != null && (channel.getType() == ChannelType.TEXT || channel.getType() == ChannelType.GUILD_PUBLIC_THREAD)) {
            sendingAllowed = channel.canTalk();
        }

        // If reporting is disabled, also tell the user
        if (!sendingAllowed) {
            ctx.getSource().sendFailure(
                    Translations.COMMAND_REPORT_STATUS.resolvedComponent(ctx.getSource(),
                            Translations.COMMAND_STATUS_DISABLED.resolvedComponent(ctx.getSource())
                    ));
            return 0;
        }

        var reportedPlayer = EntityArgument.getPlayer(ctx, "target");
        var sender = ctx.getSource().getPlayerOrException();
        var reason = StringArgumentType.getString(ctx, "reason");

        var reportedName = reportedPlayer.getName().getString();
        var senderName = sender.getName().getString();
        channel.sendMessageEmbeds(
                new EmbedBuilder()
                        .setColor(0xF5E65C)
                        .setDescription("**%s** has been reported by **%s**".formatted(reportedName, senderName))
                        .addField("Reported",
                                "%s (`%s`)".formatted(escape(reportedName), reportedPlayer.getGameProfile().getId().toString()) + '\n' +
                                        "- _Dimension_ `%s` @ _XYZ_ `%s`".formatted(reportedPlayer.level.dimension().location(), position(reportedPlayer)),
                                false)
                        .addField("Reason", reason, false)
                        .addField("Reporter",
                                "%s (`%s`)".formatted(escape(senderName), sender.getGameProfile().getId().toString()) + '\n' +
                                        "- _Dimension_ `%s` @ _XYZ_ `%s`".formatted(sender.level.dimension().location(), position(sender)),
                                false)
                        .setTimestamp(Instant.now())
                        .setFooter("Game time: " + sender.level.getGameTime())
                        .build()
        ).queue();

        ctx.getSource().sendSuccess(
                Translations.COMMAND_REPORT_SUCCESS.resolvedComponent(ctx.getSource(),
                        reportedPlayer.getName(), reason
                ), true);


        return Command.SINGLE_SUCCESS;
    }

    private static String position(Entity entity) {
        final BlockPos blockPos = entity.blockPosition();
        return "%s %s %s".formatted(blockPos.getX(), blockPos.getY(), blockPos.getZ());
    }
}
