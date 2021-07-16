package bot

import MalformedInputException
import PronounDictionary
import PronounEntry
import com.charleskorn.kaml.Yaml
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.entity.Message
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import java.io.File

class PronounBot(val kord: Kord) {
    val pronounDictionary = PronounDictionary.fetch()
    private val globalResources = BotResources.fetch() ?: BotResources()

    val trackedChannels: MutableMap<Snowflake, Snowflake> = globalResources.trackedChannels

    private val memberResources = globalResources.memberResources

    fun getMemberResources(userId: Snowflake): MemberResources? {
        return memberResources[userId]
    }

    fun addMemberPronoun(userId: Snowflake, pronoun: PronounEntry) {
        memberResources.putIfAbsent(userId, MemberResources())
        memberResources[userId]!!.pronouns.add(pronoun)
    }

    /**
     * @return The new state of the option
     */
    fun toggleMemberOption(userId: Snowflake, option: PronounOption): Boolean {
        memberResources.putIfAbsent(userId, MemberResources())
        val added = memberResources[userId]!!.options.add(option)
        if (!added) {
            memberResources[userId]!!.options.remove(option)
        }

        return added
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
    fun serializeMembers(userId: Snowflake) {
        File("./assets/members").mkdirs()
        val pronounsFile = File("./assets/members/${userId.value}.yaml")
        println("Writing current user pronouns to ${pronounsFile.path}")

        pronounsFile.writeText(Yaml.default.encodeToString(MemberResources.serializer(), memberResources[userId]!!))
    }

    companion object {
        suspend inline operator fun invoke(token: String): PronounBot {
            return PronounBot(Kord(token))
        }
    }
}