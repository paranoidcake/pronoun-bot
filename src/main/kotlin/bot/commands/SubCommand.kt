package bot.commands

import bot.PronounBot
import dev.kord.common.annotation.KordExperimental
import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.CommandArgument
import dev.kord.core.cache.data.NotSerializable
import dev.kord.core.entity.interaction.GuildInteraction
import dev.kord.core.entity.interaction.InteractionCommand
import dev.kord.core.entity.interaction.SubCommand
import kotlinx.serialization.Serializable

@KordPreview
abstract class SubCommand(val rootName: String, val name: String) {
    abstract suspend fun runOn(bot: PronounBot, interaction: GuildInteraction)

    @OptIn(KordExperimental::class)
    fun subCommandValues(command: InteractionCommand): List<CommandArgument<@Serializable(with = NotSerializable::class) Any?>> {
        return (command as SubCommand).data.options.value!!.first().values.value!!
    }
}