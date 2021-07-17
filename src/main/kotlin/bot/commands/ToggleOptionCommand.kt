package bot.commands

import bot.PronounBot
import bot.PronounOption
import dev.kord.common.annotation.KordPreview
import dev.kord.core.behavior.interaction.followUpEphemeral
import dev.kord.core.entity.interaction.GuildInteraction

@KordPreview
class ToggleOptionCommand(private val bot: PronounBot): Command {
    override suspend fun runOn(interaction: GuildInteraction) {
        val ack = interaction.acknowledgeEphemeral()

        val values = subCommandValues(interaction.command)
        val ordinal = values.first().value as String

        val option = PronounOption.values()[ordinal.toInt()]

        val newValue = bot.toggleMemberOption(interaction.user.id, option)

        ack.followUpEphemeral { content = "Successfully set ${option.name} to $newValue" }
    }
}