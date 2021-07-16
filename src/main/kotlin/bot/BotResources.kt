package bot

import PronounEntry
import com.charleskorn.kaml.Yaml
import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.io.File
import java.io.FileNotFoundException

@Serializable
class BotResources(
    @Serializable val trackedChannels: MutableMap<Snowflake, Snowflake> = mutableMapOf(),
    @Transient val memberResources: MutableMap<Snowflake, MemberResources> = mutableMapOf()
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

            val memberResourcePairs: MutableMap<Snowflake, MemberResources> = File("./assets/members/").walk().mapNotNull { file ->
                if (file.isDirectory) return@mapNotNull null

                val content = file.readText()

                if (content.isNotBlank()) {
                    Snowflake(file.nameWithoutExtension) to Yaml.default.decodeFromString(MemberResources.serializer(), content)
                } else {
                    Snowflake(file.nameWithoutExtension) to MemberResources()
                }
            }.toMap().toMutableMap()

            return if (deserializedResources != null) {
                BotResources(deserializedResources.trackedChannels, memberResourcePairs)
            } else {
                null
            }
        }
    }
}

@Serializable
data class MemberResources(val options: MutableSet<PronounOption> = mutableSetOf(), val pronouns: MutableSet<PronounEntry> = mutableSetOf())

enum class PronounOption(val description: String) {
    OnlyNickname("Display your pronouns using only your nickname")
}