import bot.PronounBot
import bot.PronounOption
import bot.commands.AddPronounCommand
import bot.commands.ScrapeCommand
import bot.commands.ToggleOptionCommand
import bot.commands.TrackCommand
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.Choice
import dev.kord.common.entity.ComponentType
import dev.kord.core.behavior.*
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.interaction.followUp
import dev.kord.core.behavior.interaction.followUpEphemeral
import dev.kord.core.event.interaction.InteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class Cli: CliktCommand() {
    private val token: String? by option(help="Discord bot user token")

    @OptIn(KordPreview::class)
    override fun run(): Unit = runBlocking {
//        val result = PronounEntry.from("he/him")?.countMatchingSegments(PronounEntry.from("he/him/his/her/herself")!!)
//        println(result)

//        val dict = PronounDictionary(setOf(PronounEntry.from("he/him/one")!!, PronounEntry.from("he/him/two")!!))
//        val pronoun = PronounEntry.from("he/him/her")!!
//        val existing = dict.toSet().filter { it.toString().contains(pronoun.toString()) || pronoun.toString().contains(it.toString()) }
//        println(existing)
//        return@runBlocking

        val commandPrefix = "pr"

        PronounBot(token!!).apply {
            /**
             * TODO: Restructure commands to be less scoped
             *
             * TODO: Replace with a global application command
             *
             * TODO: Automatically generate these with reflection or something, where we define the response behaviour
             *  in a definition class?
             *  The snowflakes of each command could allow an automatically generated list, where each definition class
             *  holds its snowflake to be matched on an interaction event
             */
            kord.guilds.onEach { guild ->
                println("Guild: $guild")
                guild.createApplicationCommands {
                    command("owner", "Test") {
                        subCommand("track-introductions", "Track introductions in the current channel") { }
                        subCommand("scrape-pronouns", "Scrape pronouns from https://pronoun-provider.tumblr.com/pronouns") { }
                    }

                    command(commandPrefix, "Test") {
                        subCommand("count", "Get a count of all known pronouns") { }
                        subCommand("example", "Give examples of your pronouns in sentences") { }
                        subCommand("add", "Add pronouns") {
                            string("pronouns", "A slash separated list of your pronouns") {
                                required = true
                            }
                        }
                        subCommand("toggle-option", "Toggles an option on / off") {
                            string("option", "The option to toggle") {
                                required = true
                                choices = PronounOption.values().map { Choice.StringChoice("${it.name}: ${it.description}", it.ordinal.toString()) }.toMutableList()
                            }
                        }
                    }
                }
            }.collect()

            kord.on<MessageCreateEvent> {
                if (message.channelId == trackedChannels[guildId] && message.author?.isBot == false) {
                    try {
                        val variants = extractPronounsFrom(message).first()!! // TODO: Error checking

                        val guild = message.getGuild()
                        val author = message.getAuthorAsMember()

                        val roleList = variants.map { pronoun ->
                            val roleName = "${pronoun.subjectPronoun}/${pronoun.objectPronoun}"

                            guild.roles.filter { role ->
                                role.name.lowercase() == roleName
                            }.firstOrNull() ?: guild.createRole {
                                name = roleName
                            }
                        }.map { it.id }

                        author?.edit {
                            if (roles.isNullOrEmpty()) {
                                roles = roleList.toMutableSet()
                            } else {
                                roles?.addAll(roleList)
                            }
                        }

                        /**
                         *  TODO: Allow users to opt into alternate naming style
                         *
                         *  TODO: Fix nicknames appending onto themselves
                        */
//                        val author = message.author?.asMember(guildId!!)!!
//                        val pronoun = variants.first()
//                        author.edit {
//                            nickname = "[${pronoun.subjectPronoun}/${pronoun.objectPronoun}] " + author.displayName
//                        }
                    } catch (e: MalformedInputException) {
                        message.author?.getDmChannel()?.createMessage {
                            embed {
                                title = "Whoops! I didn't catch that"
                                description = "Thank you for your introduction, but sadly I wasn't able to read your pronouns.\n\n" +
                                        "It would be great if you could help me out by replying with a full list for my future reference!\n\n" +
                                        "Feel free to list however many you want, like in the examples.\n"

                                field {
                                    name = "To fix this:"
                                    value = "Please reply in the form:\n\n" +
                                            "`/$commandPrefix set <subject>/<object>`\n" +
                                            "or\n" +
                                            "`/$commandPrefix set  <subject>/<object>/<pos. det.>/<pos. pronoun>/<reflexive pronoun>`\n\n" +
                                            "For example:\n" +
                                            "`/$commandPrefix set he/him, she/her`\n" +
                                            "`/$commandPrefix set he/him/his/his/himself, she/her/hers/hers/herself`"
                                }
                            }
                        }
                    }
                }
            }

            kord.on<InteractionCreateEvent> {
                val data = interaction.data.data
                when (data.componentType.value) {
                    ComponentType.ActionRow -> {}
                    ComponentType.Button -> {}
                    ComponentType.SelectMenu -> {}
                    else -> {
                        println("Interaction is not a known component, probably a command")
                        println(interaction.data.data)

                        // TODO: Replace this with the generated system
                        when (data.name.value) {
                            "owner" -> {
                                when(data.options.value?.first()?.name) {
                                    "track-introductions" -> TrackCommand(this@apply).runOn(interaction).also { serializeSettings() }
                                    "scrape-pronouns" -> ScrapeCommand(this@apply).runOn(interaction).also { serializeDictionary() }
                                }
                            }
                            "pr" -> {
                                when(data.options.value?.first()?.name) {
                                    "count" -> interaction.acknowledgeEphemeral().followUpEphemeral { content = "${pronounDictionary.count()} known pronouns!" }
                                    "example" -> {
                                        interaction.acknowledgePublic().followUp {
                                            // TODO: Figure out how to make roles less ambiguous without being too clunky. User configured opt-ins could work
                                            content = try {
                                                val pronoun = pronounDictionary.get(
                                                    interaction.user.asMember(interaction.data.guildId.value!!).roles.first().name
                                                )!!.first()

                                                pronoun.exampleText()
                                            } catch (e: NoSuchElementException) {
                                                "No pronouns set!"
                                            } catch (e: NullPointerException) {
                                                e.toString() // TODO: Ask the user to report the error? This shouldn't ever occur
                                            }
                                        }
                                    }
                                    "add" -> AddPronounCommand(this@apply).runOn(interaction).also { serializeMembers(interaction.user.id) }
                                    "toggle-option" -> ToggleOptionCommand(this@apply).runOn(interaction).also { serializeMembers(interaction.user.id) }
                                }
                            }
                        }
                    }
                }
            }

            println("Bot ready")

            // TODO: Graceful shutdowns from the server side?
            kord.login {
                watching("you")
            }
        }
    }
}

fun main(args: Array<String>) = Cli().main(args)