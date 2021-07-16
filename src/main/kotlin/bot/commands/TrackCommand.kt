package bot.commands

import bot.PronounBot
import dev.kord.common.annotation.KordPreview
import dev.kord.core.behavior.interaction.followUpEphemeral
import dev.kord.core.entity.interaction.Interaction

class TrackCommand(private val bot: PronounBot): Command {
    @OptIn(KordPreview::class)
    override suspend fun runOn(interaction: Interaction): Unit = with(bot) {
        trackedChannels[interaction.data.guildId.value!!] = interaction.data.channelId
        interaction.acknowledgeEphemeral().followUpEphemeral { content = "Now tracking ${interaction.channel.mention}" }
    }
}