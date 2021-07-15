package bot.commands

import dev.kord.common.annotation.KordPreview
import dev.kord.core.entity.interaction.Interaction

interface Command {
    @OptIn(KordPreview::class)
    suspend fun runOn(interaction: Interaction): Unit
}