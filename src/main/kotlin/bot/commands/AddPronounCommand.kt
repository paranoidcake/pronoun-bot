package bot.commands

import PronounEntry
import bot.PronounBot
import bot.PronounOption
import dev.kord.common.annotation.KordPreview
import dev.kord.core.behavior.createRole
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.interaction.followUpEphemeral
import dev.kord.core.entity.Member
import dev.kord.core.entity.Role
import dev.kord.core.entity.interaction.GuildInteraction
import dev.kord.rest.request.KtorRequestException
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toSet

@KordPreview
class AddPronounCommand(rootName: String, name: String) : SubCommand(rootName, name) {
    /**
     * Add a [PronounEntry] to a [Member] as a [Role].
     *
     * Creates the role if it does not already exist.
     * Adds the [PronounEntry] to the map of user ids to pronoun entries.
     *
     * @return The [Role] added
     */
    private suspend fun Member.addRole(pronoun: PronounEntry): Role {
        require(pronoun.isFullEntry()) {
            TODO("Add roles for partial pronoun listings")
        }

        // TODO: Check member roles and update our pronouns in case of an edit made without the bot

        val role: Role = guild.roles
            .filter { PronounEntry.from(it.name) != null }
            .toSet()
            .maxByOrNull { PronounEntry.from(it.name)!!.countMatchingSegments(pronoun) }
            ?:
            guild.createRole { name = "${pronoun.subjectPronoun}/${pronoun.objectPronoun}" }

        val memberRoles = roles.map { id }.toSet()

        edit {
            roles = memberRoles.plus(
                role.id
            ).toMutableSet()
        }

        return role
    }

    private suspend fun Member.changeNick(pronoun: PronounEntry) {
        require(pronoun.isFullEntry()) {
            TODO("Nicknames for partial pronoun listings")
        }

        val prefix = "[${pronoun.subjectPronoun}/${pronoun.objectPronoun}] "

        val oldNickname = if (nickname?.startsWith(prefix) == true) {
            asMember().displayName.substring(prefix.length - 1)
        } else {
            asMember().displayName
        }

        edit {
            nickname = prefix + oldNickname
        }
    }

    override suspend fun runOn(bot: PronounBot, interaction: GuildInteraction) {
        val ack = interaction.acknowledgeEphemeral()

        try {
            val values = subCommandValues(interaction.command)
            val target = values.first().value as String

            val variants = bot.pronounDictionary.get(target)

            if (variants == null || variants.isEmpty()) {
                TODO("Implement flow for adding new pronouns to the dictionary")
            } else if (variants.size == 1) {
                val pronoun = variants.first()

                val member = interaction.member

                if (bot.getMemberResources(interaction.user.id)?.options?.contains(PronounOption.OnlyUseNicknames) == true) {
                    member.changeNick(pronoun)
                    bot.addMemberPronoun(interaction.user.id, pronoun)
                } else {
                    member.addRole(pronoun)
                    bot.addMemberPronoun(interaction.user.id, pronoun)
                }

                ack.followUpEphemeral { content = "Added pronouns `${bot.pronounDictionary.get(pronoun.toString())}` successfully" }
            } else {
                TODO("Implement flow for picking a variant from known pronouns")
            }

        } catch (e: NotImplementedError) {
            ack.followUpEphemeral { content = "Failed to add your pronouns! Reason:\n`${e.message}`" }
        } catch (e: KtorRequestException) {
            ack.followUpEphemeral { content = "Failed to add your pronouns! Reason:\n`${e.message}`" }
        }

        bot.serializeMembers(interaction.user.id)
    }
}