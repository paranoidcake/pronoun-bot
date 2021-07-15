package bot.commands

import bot.PronounBot
import dev.kord.common.annotation.KordPreview
import dev.kord.core.behavior.createRole
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.interaction.followUpEphemeral
import dev.kord.core.entity.interaction.Interaction
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList

class AddPronounCommand(private val bot: PronounBot): Command {
    @OptIn(KordPreview::class)
    override suspend fun runOn(interaction: Interaction) {
        val ack = interaction.acknowledgeEphemeral()

        val target =
            interaction.data.data.options.value?.first { it.name == "add" }?.values?.value?.first()?.value

        val pronoun = bot.pronouns.get(target as String)!!.first() // TODO: Error checking

        // TODO: Some rewritten code from the pronoun extraction section, both could be put into one function
        val guild = bot.kord.getGuild(interaction.data.guildId.value!!)!!
        val member = interaction.user.asMember(guild.id)
        val existingRoles = member.roles.toList() // Looks like the guild doesn't update member roles, we have to get them from the member cache instead
        member.edit {
            // TODO: Generate this, as we want to have variable length role names to avoid ambiguity
            val roleName = "${pronoun.subjectPronoun}/${pronoun.objectPronoun}"

            val newRole = guild.roles.filter { role ->
                role.name.lowercase() == roleName
            }.firstOrNull() ?: guild.createRole {
                name = roleName
            }

            println("Current roles are: $existingRoles")

            roles = if (existingRoles.isNullOrEmpty()) {
                mutableSetOf(newRole.id)
            } else {
                existingRoles.map { it.id }.plus(newRole.id).toMutableSet()
            }

            ack.followUpEphemeral { content = "`$roleName` was successfully added to your pronouns!" }
        }
    }
}