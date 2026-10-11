package net.benwoodworth.fastcraft.paper

import io.mockk.mockk
import io.mockk.verify
import net.benwoodworth.fastcraft.FastCraftTranslations
import net.benwoodworth.fastcraft.Strings
import net.benwoodworth.fastcraft.platform.server.FcLogger
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FastCraftTranslationsTest {
    @TempDir
    lateinit var dataFolder: Path

    private val logger = mockk<FcLogger>(relaxed = true)

    @Test
    fun extractsAllBundledFilesAndDefaultsToBrazilianPortuguese() {
        Strings.load(dataFolder, "pt_BR", logger)

        val directory = dataFolder.resolve("lang")
        assertTrue(Files.exists(directory.resolve("pt_BR.json")))
        assertTrue(Files.exists(directory.resolve("en.json")))
        assertTrue(Files.exists(directory.resolve("es.json")))
        assertEquals(17L, Files.list(directory).use { it.count() })
        // Server-wide language, regardless of the player's Minecraft locale.
        assertEquals("§aFastCraft ativado", Strings.commandSetEnabledTrue(Locale.US))
        assertEquals("§aFastCraft ativado", Strings.commandSetEnabledTrue(Locale.forLanguageTag("fr-FR")))
    }

    @Test
    fun preservesCustomFilesAndFallsBackToEnglishForMissingKeys() {
        Strings.load(dataFolder, "pt_BR", logger)
        val custom = dataFolder.resolve("lang/pt_BR.json")
        Files.writeString(
            custom,
            """{"command.error.usage":"${red}Utilize ${usage}","gui.title":null}""",
        )
        Strings.load(dataFolder, "pt-BR.json", logger)

        assertEquals(
            "§cUtilize /fastcraft",
            Strings.commandErrorUsage(Locale.US, "/fastcraft"),
        )
        assertEquals("FastCraft", Strings.guiTitle(Locale.US))
        assertEquals(
            """{"command.error.usage":"${red}Utilize ${usage}","gui.title":null}""",
            Files.readString(custom),
        )
    }

    @Test
    fun reloadsEditedTranslationsWithoutRestarting() {
        Strings.load(dataFolder, "pt_BR", logger)
        val selected = dataFolder.resolve("lang/pt_BR.json")
        Files.writeString(selected, """{"command.set.enabled.true":"Primeira versão"}""")
        Strings.load(dataFolder, "pt_BR", logger)
        assertEquals("Primeira versão", Strings.commandSetEnabledTrue(Locale.US))

        Files.writeString(selected, """{"command.set.enabled.true":"Segunda versão"}""")
        Strings.load(dataFolder, "pt_BR", logger)
        assertEquals("Segunda versão", Strings.commandSetEnabledTrue(Locale.US))
    }

    @Test
    fun rejectsUnsafeFileNamesAndKeepsEnglishFallback() {
        Strings.load(dataFolder, "../secrets", logger)

        assertFalse(Files.exists(dataFolder.resolve("secrets.json")))
        assertEquals("§aFastCraft enabled", Strings.commandSetEnabledTrue(Locale.US))
        verify { logger.warn(match { it.contains("invalid language") }) }
    }

    @Test
    fun invalidUserJsonIsReportedAndFallsBackToEnglish() {
        Strings.load(dataFolder, "pt_BR", logger)
        Files.writeString(dataFolder.resolve("lang/pt_BR.json"), "{broken")
        Strings.load(dataFolder, "pt_BR", logger)

        assertEquals("§aFastCraft enabled", Strings.commandSetEnabledTrue(Locale.US))
        verify { logger.error(match { it.contains("pt_BR.json") }) }
    }
}
