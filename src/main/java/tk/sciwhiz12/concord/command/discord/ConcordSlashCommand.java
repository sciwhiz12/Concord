package tk.sciwhiz12.concord.command.discord;

import com.jagrosh.jdautilities.command.SlashCommand;
import com.jagrosh.jdautilities.command.SlashCommandEvent;

import net.dv8tion.jda.api.utils.MiscUtil;
import tk.sciwhiz12.concord.ChatBot;
import tk.sciwhiz12.concord.ConcordConfig;

public abstract class ConcordSlashCommand extends SlashCommand {

    protected final ChatBot bot;
    
    protected ConcordSlashCommand(final ChatBot bot) {
        this.bot = bot;
        this.guildOnly = true;
    }
    
    @Override
    protected final void execute(SlashCommandEvent event) {
        if (!event.isFromGuild()
                || event.getGuild().getIdLong() != MiscUtil.parseSnowflake(ConcordConfig.GUILD_ID.get())
                || event.getChannel().getIdLong() != MiscUtil.parseSnowflake(ConcordConfig.CHANNEL_ID.get())) {
            event.deferReply(true).setContent("This command cannot be used in this channel.").queue();
            return;
        }
        execute0(event);
    }
    
    protected abstract void execute0(SlashCommandEvent event);
    
}