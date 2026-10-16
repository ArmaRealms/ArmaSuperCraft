package net.benwoodworth.fastcraft.paper

import io.mockk.mockk
import io.mockk.verify
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
        assertEquals("<green>Super Craft ativado", Strings.commandSetEnabledTrue(Locale.US))
        assertEquals("<green> <amount>x <item>", Strings.guiRecipeIngredientsItem(Locale.US))
        assertEquals("<green>Super Craft ativado", Strings.commandSetEnabledTrue(Locale.forLanguageTag("fr-FR")))
    }

    @Test
    fun reloadErrorUsesTheSelectedServerLanguage() {
        Strings.load(dataFolder, "pt_BR", logger)
        assertEquals(
            "<red>Não foi possível recarregar a configuração do Super Craft. Consulte o console para mais detalhes.",
            Strings.commandReloadFailed(Locale.US),
        )

        Strings.load(dataFolder, "en", logger)
        assertEquals(
            "<red>Unable to reload Super Craft configuration. Check the server log.",
            Strings.commandReloadFailed(Locale.US),
        )
    }

    @Test
    fun reloadErrorStillWorksWithOlderOnDiskLanguageFiles() {
        Strings.load(dataFolder, "en", logger)
        Files.writeString(dataFolder.resolve("lang/en.json"), """{"gui.title":"Super Craft"}""")
        Files.writeString(dataFolder.resolve("lang/pt_BR.json"), """{"gui.title":"Super Craft"}""")

        Strings.load(dataFolder, "pt_BR", logger)

        // Existing user-maintained language files are never overwritten.
        assertEquals(
            "<red>Unable to reload Super Craft configuration. Check the server log.",
            Strings.commandReloadFailed(Locale.US),
        )
        assertEquals("""{"gui.title":"Super Craft"}""", Files.readString(dataFolder.resolve("lang/pt_BR.json")))
    }

    @Test
    fun preservesCustomFilesAndFallsBackToEnglishForMissingKeys() {
        Strings.load(dataFolder, "pt_BR", logger)
        val custom = dataFolder.resolve("lang/pt_BR.json")
        Files.writeString(
            custom,
            """{"command.error.usage":"<red>Utilize <usage>","gui.title":null}""",
        )
        Strings.load(dataFolder, "pt-BR.json", logger)

        assertEquals(
            "<red>Utilize <usage>",
            Strings.commandErrorUsage(Locale.US),
        )
        assertEquals("Super Craft", Strings.guiTitle(Locale.US))
        assertEquals(
            """{"command.error.usage":"<red>Utilize <usage>","gui.title":null}""",
            Files.readString(custom),
        )
    }

    @Test
    fun rejectsLegacyFormattingInsteadOfMigratingOldFiles() {
        Strings.load(dataFolder, "pt_BR", logger)
        val selected = dataFolder.resolve("lang/pt_BR.json")
        Files.writeString(selected, """{"gui.title":"§aAntigo"}""")
        Strings.load(dataFolder, "pt_BR", logger)
        assertEquals("Super Craft", Strings.guiTitle(Locale.US))
        verify { logger.error(match { it.contains("old formatting") }) }

        Files.writeString(selected, """{"gui.title":"&aAntigo"}""")
        Strings.load(dataFolder, "pt_BR", logger)
        assertEquals("Super Craft", Strings.guiTitle(Locale.US))
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
        assertEquals("<green>Super Craft enabled", Strings.commandSetEnabledTrue(Locale.US))
        verify { logger.warn(match { it.contains("invalid language") }) }
    }

    @Test
    fun invalidUserJsonIsReportedAndFallsBackToEnglish() {
        Strings.load(dataFolder, "pt_BR", logger)
        Files.writeString(dataFolder.resolve("lang/pt_BR.json"), "{broken")
        Strings.load(dataFolder, "pt_BR", logger)

        assertEquals("<green>Super Craft enabled", Strings.commandSetEnabledTrue(Locale.US))
        verify { logger.error(match { it.contains("pt_BR.json") }) }
    }
}
