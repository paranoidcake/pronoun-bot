import bot.PronounBot
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.ButtonStyle
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.interaction.followUpEphemeral
import dev.kord.core.event.interaction.InteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import kotlinx.coroutines.runBlocking

class Cli: CliktCommand() {
    private val token: String? by option(help="Discord bot user token")

    @OptIn(KordPreview::class)
    override fun run() = runBlocking {
        val bot = PronounBot(token!!) { }

        bot.kord.on<MessageCreateEvent> {
            if (message.author?.isBot == false && message.content == "button test") {
                println("message in")
                message.channel.createMessage("pong")
                message.channel.createMessage {
                    embed { description = "Test" }
                    actionRow {
                        interactionButton(ButtonStyle.Primary, "testinggg") { label = "Test!" }
                    }
                }
            }
        }

        bot.kord.on<InteractionCreateEvent> {
            println("interaction!!!")
            interaction.acknowledgeEphemeral().followUpEphemeral {

                content = "You pressed a button, with this data: ${interaction.data.data}"
            }
        }

//        bot.kord.on<MessageCreateEvent> {
//            if (message.content == "<@!777216205735198741> talk here" && (message.author?.id?.value == 249962604603113473 || message.author?.id?.value == 539773942873718794)) {
//                message.delete()
//                while (true) {
//                    val res = readLine()
//                    if (res.isNullOrBlank()) {
//                        break
//                    } else {
//                        message.channel.createMessage(res)
//                    }
//                }
//            }
//        }

        println("Bot ready")

        bot.kord.login {
            watching("you")
        }
    }
}

fun main(args: Array<String>) { Cli().main(args) }