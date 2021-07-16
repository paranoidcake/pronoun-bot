package bot.commands

import bot.PronounBot
import dev.kord.common.annotation.KordPreview
import dev.kord.core.behavior.interaction.followUpEphemeral
import dev.kord.core.entity.interaction.Interaction

class ScrapeCommand(private val bot: PronounBot): Command {
    @OptIn(KordPreview::class)
    override suspend fun runOn(interaction: Interaction): Unit = with(bot) {
        val ack = interaction.acknowledgeEphemeral()
        pronounDictionary.scrape()
        ack.followUpEphemeral { content = "Scraping finished!" }
    }
}