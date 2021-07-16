package bot.commands

import bot.PronounBot
import dev.kord.common.annotation.KordPreview
import dev.kord.core.behavior.interaction.followUpEphemeral
import dev.kord.core.entity.interaction.Interaction

class AddPronounCommand(private val bot: PronounBot): Command {
    @OptIn(KordPreview::class)
    override suspend fun runOn(interaction: Interaction): Unit = with(bot) {
        val ack = interaction.acknowledgeEphemeral()

        try {
            val target =
                interaction.data.data.options.value?.first { it.name == "add" }?.values?.value?.first()?.value

            val variants = bot.pronounDictionary.get(target as String)

            if (variants == null || variants.isEmpty()) {
                TODO("Implement flow for adding new pronouns to the dictionary")
            } else if (variants.size == 1) {
                val pronoun = variants.first()

                val guild = bot.kord.getGuild(interaction.data.guildId.value!!)!!
                val member = interaction.user.asMember(guild.id)

                addRole(member, guild, pronoun)

                ack.followUpEphemeral { content = "Added pronouns `${bot.pronounDictionary.get(pronoun.toString())}` successfully" }
            } else {
                TODO("Implement flow for picking a variant from known pronouns")
            }

        } catch (e: NotImplementedError) {
            ack.followUpEphemeral { content = "Failed to add your pronouns! Reason:\n`${e.message}`" }
        }

    }
}