package bot.commands

import PronounEntry
import bot.PronounBot
import dev.kord.common.annotation.KordPreview
import dev.kord.core.behavior.interaction.followUpEphemeral
import dev.kord.core.entity.interaction.Interaction

class AddPronounCommand(private val bot: PronounBot): Command {
    @OptIn(KordPreview::class)
    override suspend fun runOn(interaction: Interaction): Unit = with(bot) {
        val ack = interaction.acknowledgeEphemeral()

        val target =
            interaction.data.data.options.value?.first { it.name == "add" }?.values?.value?.first()?.value

        val pronoun = PronounEntry.from(target as String) // TODO: Error checking

        val guild = bot.kord.getGuild(interaction.data.guildId.value!!)!!
        val member = interaction.user.asMember(guild.id)

        addRole(member, guild, pronoun!!)

        ack.followUpEphemeral { content = "Your pronouns (`$pronoun`) were successfully added!" }
    }
}