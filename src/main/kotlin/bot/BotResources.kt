package bot

import com.charleskorn.kaml.Yaml
import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
class BotResources {
    @Serializable
    val trackedChannels = hashMapOf<Snowflake, Snowflake>()

    companion object {
        fun fetch(): BotResources {
            return if (this::class.java.classLoader.getResource("settings.yaml")?.file.isNullOrEmpty()) {
                BotResources()
            } else {
                val file = File(this::class.java.classLoader.getResource("settings.yaml")!!.file)
                Yaml.default.decodeFromString(serializer(), file.readText())
            }
        }
    }
}