package bot

import MalformedInputException
import PronounEntry
import PronounDictionary
import bot.commands.AddPronounCommand
import bot.commands.Command
import bot.commands.ScrapeCommand
import bot.commands.TrackCommand
import com.charleskorn.kaml.Yaml
import dev.kord.common.annotation.KordPreview
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.followUpEphemeral
import dev.kord.core.entity.Message
import dev.kord.core.entity.interaction.Interaction
import java.io.File

class PronounBot(val kord: Kord) {
    val pronouns = PronounDictionary.fetch()
    private val resources = BotResources.fetch()

    // Generate this list
    val commands = mapOf<String, Command>(
        "scrape" to ScrapeCommand(this),
        "track" to TrackCommand(this),
        "add-pronoun" to AddPronounCommand(this)
    )

    val trackedChannels = resources.trackedChannels

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
                pronouns.get(it.first(), it.last()) // TODO: Clarify which variant to choose when ambiguous, should go elsewhere as we do sometimes want to list all variants
            }
        } else {
            throw MalformedInputException("Failed sanity check")
        }
    }

    // TODO: Since these serialize funcs are getting called relatively often, it should probably be optimised at some point
    fun serializeSettings() {
        if (trackedChannels.isNotEmpty()) {
            val resDir = this::class.java.classLoader.getResource("./")!!.toURI()

            val settingsFile = File(resDir.path + "settings.yaml")
            println("Writing settings to ${settingsFile.path}")
            settingsFile.writeText(Yaml.default.encodeToString(BotResources.serializer(), resources))
        }
    }

    fun serializePronouns() {
        val resDir = this::class.java.classLoader.getResource("./")!!.toURI()

        val pronounsFile = File(resDir.path + "pronouns.yaml")
        println("Writing pronouns to ${pronounsFile.path}")
        pronounsFile.writeText(Yaml.default.encodeToString(PronounDictionary.serializer(), pronouns))
    }

    companion object {
        suspend inline operator fun invoke(token: String): PronounBot {
            return PronounBot(Kord(token))
        }
    }
}