package bot.commands

import bot.PronounBot
import dev.kord.common.annotation.KordPreview
import dev.kord.core.behavior.interaction.followUpEphemeral
import dev.kord.core.entity.interaction.GuildInteraction

@KordPreview
class ScrapeCommand(rootName: String, name: String): SubCommand(rootName, name) {
    override suspend fun runOn(bot: PronounBot, interaction: GuildInteraction) {
        val ack = interaction.acknowledgeEphemeral()
        bot.pronounDictionary.scrape()
        ack.followUpEphemeral { content = "Scraping finished!" }

        bot.serializeDictionary()
    }
}