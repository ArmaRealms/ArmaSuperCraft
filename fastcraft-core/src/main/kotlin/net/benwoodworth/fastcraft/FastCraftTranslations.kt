package net.benwoodworth.fastcraft

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import net.benwoodworth.fastcraft.platform.server.FcLogger
import java.nio.charset.StandardCharsets
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale

/** FastCraft strings use the server language selected in config.yml. */
object FastCraftTranslations {
    private val bundledFiles = listOf(
        "cs.json", "de.json", "en.json", "es-ve.json", "es.json",
        "fr.json", "hu.json", "ko.json", "nl.json", "pl.json",
        "pt_BR.json", "ru.json", "tr.json", "vi.json",
        "zh-cn.json", "zh-hk.json", "zh-tw.json",
    )
    private val localeCode = Regex("""[A-Za-z]{2,3}(?:[_-][A-Za-z0-9]{2,8})*""")

    private data class Catalog(val selected: Map<String, String>, val english: Map<String, String>)

    @Volatile private var catalog = Catalog(emptyMap(), emptyMap())

    /** MiniMessage templates are returned untouched. No custom placeholder expansion. */
    @Suppress("UNUSED_PARAMETER")
    fun get(locale: Locale, key: String): String? {
        val active = catalog
        return active.selected[key] ?: active.english[key]
    }

    /** The legacy placeholders and color notations are not accepted in language files. */
    private val obsoleteSyntax = Regex("""\$(?:\{[A-Za-z_][A-Za-z0-9_]*}|[A-Za-z_][A-Za-z0-9_]*)|§[0-9A-FK-ORa-fk-or]|&[0-9A-FK-ORa-fk-or]""")

    /** Export shipped JSONs once; never overwrite server-customized files. */
    fun load(dataFolder: Path, language: String, logger: FcLogger) {
        val folder = dataFolder.resolve("lang")
        Files.createDirectories(folder)

        for (name in bundledFiles) {
            val target = folder.resolve(name)
            if (Files.exists(target)) continue
            val resource = javaClass.classLoader.getResourceAsStream("lang/$name")
            if (resource == null) {
                logger.error("Missing bundled translation: lang/$name")
                continue
            }
            resource.use { source ->
                try {
                    Files.copy(source, target)
                } catch (_: FileAlreadyExistsException) {
                    // A concurrent process created the file.
                }
            }
        }

        val english = readOrNull(folder.resolve("en.json"), logger)
            ?: javaClass.classLoader.getResourceAsStream("lang/en.json")
                ?.bufferedReader(StandardCharsets.UTF_8)
                ?.use { parse(it.readText()) }
            ?: emptyMap()

        val requested = language.trim().removeSuffix(".json")
        val selected = if (localeCode.matches(requested)) {
            // pt_BR and pt-BR (and their casing) refer to the same file.
            Files.list(folder).use { paths ->
                paths.filter { Files.isRegularFile(it) }
                    .filter { it.fileName.toString().endsWith(".json", ignoreCase = true) }
                    .filter {
                        it.fileName.toString().dropLast(5).replace('-', '_')
                            .equals(requested.replace('-', '_'), ignoreCase = true)
                    }
                    .findFirst().orElse(null)
            }
        } else null

        if (selected == null) {
            logger.warn("Unknown or invalid language '$language'. Falling back to en.json")
        }
        val localized = selected?.let { readOrNull(it, logger) } ?: english
        catalog = Catalog(localized, english)
    }

    private fun readOrNull(file: Path, logger: FcLogger): Map<String, String>? =
        try {
            parse(Files.readString(file, StandardCharsets.UTF_8))
        } catch (e: Exception) {
            logger.error("Unable to load translation ${file.fileName}: ${e.message}")
            null
        }

    private fun parse(contents: String): Map<String, String> =
        Json.parseToJsonElement(contents).jsonObject.mapNotNull { (key, value) ->
            when (value) {
                JsonNull -> null
                is JsonPrimitive -> {
                    require(value.isString) { "Translation '$key' must be a JSON string or null" }
                    require(!obsoleteSyntax.containsMatchIn(value.content)) {
                        "Translation '$key' uses old formatting; use MiniMessage tags only"
                    }
                    key to value.content
                }
                else -> error("Translation '$key' must be a JSON string or null")
            }
        }.toMap()
}
