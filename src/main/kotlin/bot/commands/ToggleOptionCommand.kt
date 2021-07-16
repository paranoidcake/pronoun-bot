package bot.commands

import bot.PronounBot
import bot.PronounOption
import dev.kord.common.annotation.KordPreview
import dev.kord.core.behavior.interaction.followUpEphemeral
import dev.kord.core.entity.interaction.Interaction

class ToggleOptionCommand(private val bot: PronounBot): Command {
    @OptIn(KordPreview::class)
    override suspend fun runOn(interaction: Interaction) {
        val ack = interaction.acknowledgeEphemeral()

        val ordinal = interaction.data.data.options.value?.first()?.values?.value?.first()?.value as String

        val newValue = bot.toggleMemberOption(interaction.user.id, PronounOption.values()[ordinal.toInt()])

        ack.followUpEphemeral { content = "Successfully set option to $newValue" }
    }
}