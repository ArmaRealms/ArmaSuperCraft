package net.benwoodworth.fastcraft.bukkit.player

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.benwoodworth.fastcraft.bukkit.world.FcItemStack_Bukkit
import net.benwoodworth.fastcraft.platform.player.FcPlayer
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.junit.jupiter.api.Test

class FcPlayerMessageTest {
    @Test
    fun readsClientLocaleFromNativePaperApi() {
        val player = mockk<Player>()
        val clientLocale = java.util.Locale.forLanguageTag("pt-BR")
        every { player.locale() } returns clientLocale
        val operations = FcPlayer_Bukkit_Paper.Operations(
            mockk(),
            mockk<FcSound_Bukkit.Operations>(),
            mockk<FcItemStack_Bukkit.Operations>(),
        )

        with(operations) {
            kotlin.test.assertEquals(clientLocale, FcPlayer(player).locale)
        }
    }

    @Test
    fun sendsAdventureComponentWithoutLegacySerialization() {
        val player = mockk<Player>(relaxed = true)
        val component = Component.translatable("block.minecraft.stone")

        val operations = FcPlayer_Bukkit_Paper.Operations(
            mockk(),
            mockk<FcSound_Bukkit.Operations>(),
            mockk<FcItemStack_Bukkit.Operations>(),
        )
        with(operations) { FcPlayer(player).sendMessage(component) }

        verify(exactly = 1) { player.sendMessage(component) }
    }
}
