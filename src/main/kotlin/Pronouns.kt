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
import java.io.FileNotFoundException

internal data class Webpage(var httpStatusCode: Int = 0,
                   var httpStatusMessage: String = "",
                   var allHeadings: List<String> = listOf(),
)

@Serializable
class PronounDictionary() {
    // TODO: Remove hardcoded values
    @Serializable
    private val set: MutableSet<PronounEntry> = mutableSetOf(
        PronounEntry("he", "him", "his", "his", "himself"),
        PronounEntry("she", "her", "hers", "hers", "herself"),
        PronounEntry("they", "them", "their", "theirs", "theirself")
    )

    constructor(collection: Collection<PronounEntry>) : this() {
        this.set.clear()
        this.set.addAll(collection)
    }

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

    fun toSet(): MutableSet<PronounEntry> = set

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
            it.possessiveDeterminer?.lowercase() == possessiveDeterminer.lowercase()
                &&
            it.possessivePronoun?.lowercase() == possessivePronoun.lowercase()
                &&
            it.reflexivePronoun?.lowercase() == reflexivePronoun.lowercase()
        }
    }

    fun get(subjectPronoun: String, objectPronoun: String, possessiveDeterminer: String, reflexivePronoun: String): List<PronounEntry>? {
        return set.filter {
            it.subjectPronoun.lowercase() == subjectPronoun.lowercase()
                    &&
                    it.objectPronoun.lowercase() == objectPronoun.lowercase()
                    &&
                    it.possessiveDeterminer?.lowercase() == possessiveDeterminer.lowercase()
                    &&
                    it.reflexivePronoun?.lowercase() == reflexivePronoun.lowercase()
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
            it.possessiveDeterminer?.lowercase() == possessiveDeterminer.lowercase()
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
            return try {
                val text = File("./assets/pronounDictionary.yaml").readText()

                if (text.isEmpty()) {
                    PronounDictionary().apply { scrape() }
                } else {
                    Yaml.default.decodeFromString(serializer(), text)
                }
            } catch (e: FileNotFoundException) {
                PronounDictionary().apply { scrape() }
            }
        }
    }
}

@Serializable
class PronounEntry(
    val subjectPronoun: String,
    val objectPronoun: String,
    val possessiveDeterminer: String?,
    val possessivePronoun: String?,
    val reflexivePronoun: String?)
{
    fun exampleText(): String {
        require(possessiveDeterminer != null && possessivePronoun != null && reflexivePronoun != null) {
            "Not enough pronouns are stored to generate an example conjugation"
        }

        return "This morning, $subjectPronoun went to the park.\n" +
                "I went with $objectPronoun.\n" +
                "And $subjectPronoun bought $possessiveDeterminer frisbee.\n" +
                "At least I think it was $possessivePronoun.\n" +
                "By the end of the day, $subjectPronoun started throwing the frisbee to $reflexivePronoun."
    }

    fun toList(): List<String> {
        return listOfNotNull(subjectPronoun, objectPronoun, possessiveDeterminer, possessivePronoun, reflexivePronoun)
    }

    fun countMatchingSegments(other: PronounEntry): Int {
        require(other.reflexivePronoun != null && other.possessivePronoun != null && other.possessiveDeterminer != null) {
            "The pronoun matched against must be fully defined"
        }

        return toList().zip(other.toList()).count { (segment, otherSegment) -> println("Comparing $segment to $otherSegment"); segment == otherSegment }
    }

    override fun toString(): String {
        return "$subjectPronoun/$objectPronoun" +
                if (possessiveDeterminer != null) "/$possessiveDeterminer" else "" +
                if (possessivePronoun != null) "/$possessivePronoun" else "" +
                if (reflexivePronoun != null) "/$reflexivePronoun" else ""
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

            return when (items.size) {
                5 -> PronounEntry(items[0], items[1], items[2], items[3], items[4])
                4 -> PronounEntry(items[0], items[1], items[2], null, items[3])
                3 -> PronounEntry(items[0], items[1], items[2], null, null)
                2 -> PronounEntry(items[0], items[1], null, null, null)
                else -> null
            }
        }
    }
}