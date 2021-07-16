package bot

import PronounEntry
import com.charleskorn.kaml.Yaml
import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.SetSerializer
import java.io.File
import java.io.FileNotFoundException

@Serializable
class BotResources(
    @Serializable val trackedChannels: MutableMap<Snowflake, Snowflake> = mutableMapOf(),
    @Transient val guildMemberPronouns: MutableMap<Snowflake, MutableMap<Snowflake, MutableSet<PronounEntry>>> = mutableMapOf()
) {
    companion object {
        fun fetch(): BotResources? {
            val deserializedResources = try {
                val content = File("./assets/settings.yaml").readText()

                if (content.isNotBlank()) {
                    Yaml.default.decodeFromString(serializer(), content)
                } else {
                    null
                }
            } catch (e: FileNotFoundException) {
                null
            }

            val guildMapPairs: MutableMap<Snowflake, MutableMap<Snowflake, MutableSet<PronounEntry>>> = File("./assets/guilds/").walk().mapNotNull { file ->
                if (file.isDirectory) return@mapNotNull null

                val content = file.readText()

                if (content.isNotBlank()) {
                    val mapSerializer = MapSerializer(Snowflake.serializer(), SetSerializer(PronounEntry.serializer()))
                    Snowflake(file.nameWithoutExtension) to Yaml.default.decodeFromString(mapSerializer, content).map {
                        it.key to it.value.toMutableSet()
                    }.toMap().toMutableMap()
                } else {
                    Snowflake(file.nameWithoutExtension) to mutableMapOf()
                }
            }.toMap().toMutableMap()

            return if (deserializedResources != null) {
                BotResources(deserializedResources.trackedChannels, guildMapPairs)
            } else {
                null
            }
        }
    }
}