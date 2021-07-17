package bot.commands

import bot.PronounBot
import dev.kord.common.annotation.KordPreview
import dev.kord.core.behavior.interaction.followUpEphemeral
import dev.kord.core.entity.interaction.GuildInteraction

@KordPreview
class TrackCommand(private val bot: PronounBot): Command {
    override suspend fun runOn(interaction: GuildInteraction): Unit = with(bot) {
        trackedChannels[interaction.guildId] = interaction.channelId
        interaction.acknowledgeEphemeral().followUpEphemeral { content = "Now tracking ${interaction.channel.mention}" }
    }
}