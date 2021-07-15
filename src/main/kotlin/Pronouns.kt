import bot.PronounBot
import com.charleskorn.kaml.Yaml
import it.skrape.core.htmlDocument
import it.skrape.fetcher.HttpFetcher
import it.skrape.fetcher.extractIt
import it.skrape.fetcher.skrape
import it.skrape.selects.eachText
import it.skrape.selects.html5.div
import it.skrape.selects.html5.h1
import it.skrape.selects.html5.ul
import kotlinx.serialization.Serializable
import java.io.File

data class Webpage(var httpStatusCode: Int = 0,
                   var httpStatusMessage: String = "",
                   var allHeadings: List<String> = listOf(),
)

/**
 * Resolves and finds user pronouns
 */
//class Pronouns(private val bot: PronounBot) {
//
//}

@Serializable
class PronounDictionary {
    // TODO: Replace this with something that makes more sense
    // TODO: Remove hardcoded values
    @Serializable
    private val set: MutableSet<PronounEntry> = mutableSetOf(
        PronounEntry("he", "him", "his", "his", "himself"),
        PronounEntry("she", "her", "hers", "hers", "herself"),
        PronounEntry("they", "them", "they", "their", "themselves")
    )

    fun scrape() {
        println("Scraping...")

        val page = skrape(HttpFetcher) {
            request { url = "https://pronoun-provider.tumblr.com/pronouns" }

            extractIt<Webpage> {
                htmlDocument {
                    findAll {
                        div {
                            withId = "pof"
                            it.allHeadings = h1 { findAll { eachText } }
                            set.addAll(ul {
                                findAll {
                                    eachText.flatMap { text ->
                                        println(text); text.split(" ").map { PronounEntry.from(it) }
                                    }.filterNotNull()
                                }
                            })
                        }
                    }
                }
            }
        }

        println("Found ${page.allHeadings.size} headings")
    }

    fun get(slashSeparatedString: String): List<PronounEntry>? {
        val nouns = slashSeparatedString.split("/")

        return when (nouns.size) {
            5 -> {
                val (subjectPronoun, objectPronoun, possessiveDeterminer, possessivePronoun, reflexivePronoun) = nouns
                val result = get(subjectPronoun, objectPronoun, possessiveDeterminer, possessivePronoun, reflexivePronoun)

                return if (result == null) {
                    null
                } else {
                    listOf(result)
                }
            }
            4 -> {
                val (subjectPronoun, objectPronoun, possessiveDeterminer, reflexivePronoun) = nouns
                get(subjectPronoun, objectPronoun, possessiveDeterminer, reflexivePronoun)
            }
            3 -> {
                val (subjectPronoun, objectPronoun, possessiveDeterminer) = nouns

                return get(subjectPronoun, objectPronoun, possessiveDeterminer)
            }
            2 -> {
                val (subjectPronoun, objectPronoun) = nouns
                get(subjectPronoun, objectPronoun)
            }
            else -> {
                null
            }
        }
    }

    fun get(subjectPronoun: String, objectPronoun: String, possessiveDeterminer: String, possessivePronoun: String, reflexivePronoun: String): PronounEntry? {
        return set.firstOrNull {
            it.subjectPronoun.lowercase() == subjectPronoun.lowercase()
                &&
            it.objectPronoun.lowercase() == objectPronoun.lowercase()
                &&
            it.possessiveDeterminer.lowercase() == possessiveDeterminer.lowercase()
                &&
            it.possessivePronoun.lowercase() == possessivePronoun.lowercase()
                &&
            it.reflexivePronoun.lowercase() == reflexivePronoun.lowercase()
        }
    }

    fun get(subjectPronoun: String, objectPronoun: String, possessiveDeterminer: String, reflexivePronoun: String): List<PronounEntry>? {
        return set.filter {
            it.subjectPronoun.lowercase() == subjectPronoun.lowercase()
                    &&
                    it.objectPronoun.lowercase() == objectPronoun.lowercase()
                    &&
                    it.possessiveDeterminer.lowercase() == possessiveDeterminer.lowercase()
                    &&
                    it.reflexivePronoun.lowercase() == reflexivePronoun.lowercase()
        }.ifEmpty {
            null
        }
    }

    fun get(subjectPronoun: String, objectPronoun: String, possessiveDeterminer: String): List<PronounEntry>? {
        return set.filter {
            it.subjectPronoun.lowercase() == subjectPronoun.lowercase()
                &&
            it.objectPronoun.lowercase() == objectPronoun.lowercase()
                &&
            it.possessiveDeterminer.lowercase() == possessiveDeterminer.lowercase()
        }.ifEmpty {
            null
        }
    }

    fun get(subjectPronoun: String, objectPronoun: String): List<PronounEntry>? {
        return set.filter {
            it.subjectPronoun.lowercase() == subjectPronoun.lowercase()
                &&
            it.objectPronoun.lowercase() == objectPronoun.lowercase()
        }.ifEmpty {
            null
        }
    }

    fun count(): Int {
        return set.size
    }

    companion object {
        fun fetch(): PronounDictionary {
            val file = this::class.java.classLoader.getResource("pronouns.yaml")?.file
            return if (!this::class.java.classLoader.getResource("pronouns.yaml")?.file.isNullOrEmpty()) {
                val text = File(file!!).readText()
                if (text.isEmpty()) {
                    PronounDictionary().apply { scrape() }
                } else {
                    Yaml.default.decodeFromString(serializer(), text)
                }
            } else {
                PronounDictionary()
            }
        }
    }
}

@Serializable
class PronounEntry(
    val subjectPronoun: String,
    val objectPronoun: String,
    val possessiveDeterminer: String,
    val possessivePronoun: String,
    val reflexivePronoun: String)
{
    fun exampleText(): String {
        return "This morning, $subjectPronoun went to the park.\n" +
                "I went with $objectPronoun.\n" +
                "And $possessiveDeterminer bought $possessivePronoun frisbee.\n" +
                "At least I think it was $possessivePronoun.\n" +
                "By the end of the day, $subjectPronoun started throwing the frisbee to $reflexivePronoun."
    }

    override fun toString(): String {
        return "Pronoun(subjectPronoun='$subjectPronoun', objectPronoun='$objectPronoun', possessiveDeterminer='$possessiveDeterminer', possessivePronoun='$possessivePronoun', reflexivePronoun='$reflexivePronoun')"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PronounEntry

        if (subjectPronoun != other.subjectPronoun) return false
        if (objectPronoun != other.objectPronoun) return false
        if (possessiveDeterminer != other.possessiveDeterminer) return false
        if (possessivePronoun != other.possessivePronoun) return false
        if (reflexivePronoun != other.reflexivePronoun) return false

        return true
    }

    override fun hashCode(): Int {
        var result = subjectPronoun.hashCode()
        result = 31 * result + objectPronoun.hashCode()
        result = 31 * result + possessiveDeterminer.hashCode()
        result = 31 * result + possessivePronoun.hashCode()
        result = 31 * result + reflexivePronoun.hashCode()
        return result
    }

    companion object {
        fun from(slashSeparatedString: String): PronounEntry? {
            val items = slashSeparatedString.split("/")

            return if (items.size == 5) {
                PronounEntry(items[0], items[1], items[2], items[3], items[4])
            } else {
                null
            }
        }
    }
}