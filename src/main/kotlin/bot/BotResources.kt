package bot

import com.charleskorn.kaml.Yaml
import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable
import java.io.File
import java.io.FileNotFoundException

@Serializable
class BotResources {
    @Serializable
    val trackedChannels = hashMapOf<Snowflake, Snowflake>()

    companion object {
        fun fetch(): BotResources? {
            return try {
                val content = File("./assets/settings.yaml").readText()

                if (content.isNotBlank()) {
                    Yaml.default.decodeFromString(serializer(), content)
                } else {
                    null
                }
            } catch (e: FileNotFoundException) {
                null
            }
        }
    }
}