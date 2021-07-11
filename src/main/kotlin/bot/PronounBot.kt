package bot

import dev.kord.core.Kord

class PronounBot(val kord: Kord) {
    companion object {
        suspend inline operator fun invoke(token: String, builder: PronounBotBuilder.() -> Unit): PronounBot {
            return PronounBotBuilder(token).apply { builder() }.build()
        }
    }
}