package bot

import MalformedInputException
import PronounDictionary
import PronounEntry
import bot.commands.AddPronounCommand
import bot.commands.Command
import bot.commands.ScrapeCommand
import bot.commands.TrackCommand
import com.charleskorn.kaml.Yaml
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.createRole
import dev.kord.core.behavior.edit
import dev.kord.core.entity.Guild
import dev.kord.core.entity.Member
import dev.kord.core.entity.Message
import dev.kord.core.entity.Role
import kotlinx.coroutines.flow.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import java.io.File

class PronounBot(val kord: Kord) {
    val pronounDictionary = PronounDictionary.fetch()
    private val globalResources = BotResources.fetch() ?: BotResources()

    val commands = mapOf<String, Command>(
        "scrape" to ScrapeCommand(this),
        "track" to TrackCommand(this),
        "add-pronoun" to AddPronounCommand(this)
    )

    val trackedChannels: MutableMap<Snowflake, Snowflake> = globalResources.trackedChannels

    private val guildMemberPronouns = globalResources.guildMemberPronouns

    /**
     * Add a [PronounEntry] to a [Member] as a [Role].
     *
     * Creates the role if it does not already exist.
     * Adds the [PronounEntry] to the map of user ids to pronoun entries.
     *
     * @return The [Role] added
     */
    suspend fun addRole(member: Member, guild: Guild, pronoun: PronounEntry): Role {
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

        val memberRoles = member.roles.map { it.id }.toSet()

        member.edit {
            roles = memberRoles.plus(
                role.id
            ).toMutableSet()
        }

        guildMemberPronouns.putIfAbsent(guild.id, mutableMapOf())
        guildMemberPronouns[guild.id]!!.putIfAbsent(member.id, mutableSetOf())
        guildMemberPronouns[guild.id]!![member.id]!!.add(pronoun)

        return role
    }

    /*
    Process an introduction template and get the pronouns out

    Currently in the form:

    Name: <name>
    Pronouns: <subject>/<object>, <subject>/<object>
    ...
    */
    fun extractPronounsFrom(message: Message): List<List<PronounEntry>?> {
        val sanityRegex = Regex("[pronounsPRONOUNS]{8}\\b:(?: *\\w+/\\w+,*)+")

        val lines = message.content.split("\n")
        var rawPronouns = lines.first { it.lowercase().contains("pronouns") }
        if (sanityRegex.matches(rawPronouns)) {
            rawPronouns = rawPronouns.split(":").last()
            rawPronouns = rawPronouns.replace(" ", "")
            val subObjPairs = rawPronouns.split(",").map { it.split("/") }

            println("Found pronouns: $subObjPairs")

            return subObjPairs.map { // TODO: Different resolution strategies, better error checking/handling
                pronounDictionary.get(it.first(), it.last()) // TODO: Clarify which variant to choose when ambiguous, should go elsewhere as we do sometimes want to list all variants
            }
        } else {
            throw MalformedInputException("Failed sanity check")
        }
    }

    // TODO: Make this lazy
    fun serializeSettings() {
        if (trackedChannels.isNotEmpty()) {
            File("./assets").mkdir()
            val settingsFile = File("./assets/settings.yaml")
            println("Writing settings to ${settingsFile.path}")
            settingsFile.writeText(Yaml.default.encodeToString(BotResources.serializer(), globalResources))
        }
    }

    // TODO: Make this lazy
    fun serializeDictionary() {
        File("./assets").mkdir()
        val dictionaryFile = File("./assets/pronounDictionary.yaml")
        println("Writing pronouns to ${dictionaryFile.path}")
        dictionaryFile.writeText(Yaml.default.encodeToString(PronounDictionary.serializer(), pronounDictionary))
    }

    // TODO: Make this lazy
    fun serializePronouns(guildId: Snowflake) {
        File("./assets/guilds").mkdirs()
        val pronounsFile = File("./assets/guilds/${guildId.value}.yaml")
        println("Writing current user pronouns to ${pronounsFile.path}")

        val pronounListSerializer: KSerializer<Map<Snowflake, Set<PronounEntry>>> = serializer()
        pronounsFile.writeText(Yaml.default.encodeToString(pronounListSerializer, guildMemberPronouns[guildId] as Map<Snowflake, Set<PronounEntry>>))
    }

    companion object {
        suspend inline operator fun invoke(token: String): PronounBot {
            return PronounBot(Kord(token))
        }
    }
}