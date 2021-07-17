package bot.commands

import bot.PronounBot
import dev.kord.common.annotation.KordPreview
import dev.kord.core.behavior.interaction.followUpEphemeral
import dev.kord.core.entity.interaction.GuildInteraction

@KordPreview
class ScrapeCommand(private val bot: PronounBot): Command {
    override suspend fun runOn(interaction: GuildInteraction): Unit = with(bot) {
        val ack = interaction.acknowledgeEphemeral()
        pronounDictionary.scrape()
        ack.followUpEphemeral { content = "Scraping finished!" }
    }
}