package bot

import dev.kord.core.Kord

class PronounBotBuilder(private val token: String) {
    suspend fun build(): PronounBot {
        return PronounBot(Kord(token))
    }
}