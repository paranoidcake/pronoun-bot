package bot.commands

import bot.PronounBot
import dev.kord.common.annotation.KordPreview
import dev.kord.core.behavior.interaction.followUpEphemeral
import dev.kord.core.entity.interaction.GuildInteraction

@KordPreview
class TrackCommand(rootName: String, name: String): SubCommand(rootName, name) {
    override suspend fun runOn(bot: PronounBot, interaction: GuildInteraction) {
        bot.trackedChannels[interaction.guildId] = interaction.channelId
        interaction.acknowledgeEphemeral().followUpEphemeral { content = "Now tracking ${interaction.channel.mention}" }
        bot.serializeSettings()
    }
}