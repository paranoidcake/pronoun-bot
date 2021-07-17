package bot.commands

import dev.kord.common.annotation.KordExperimental
import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.CommandArgument
import dev.kord.core.cache.data.NotSerializable
import dev.kord.core.entity.interaction.GuildInteraction
import dev.kord.core.entity.interaction.InteractionCommand
import dev.kord.core.entity.interaction.SubCommand
import kotlinx.serialization.Serializable

@KordPreview
interface Command {
    suspend fun runOn(interaction: GuildInteraction)

    @OptIn(KordExperimental::class)
    fun subCommandValues(command: InteractionCommand): List<CommandArgument<@Serializable(with = NotSerializable::class) Any?>> {
        return (command as SubCommand).data.options.value!!.first().values.value!!
    }
}