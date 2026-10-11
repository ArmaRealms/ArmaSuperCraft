package net.benwoodworth.fastcraft.bukkit.player

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import net.benwoodworth.fastcraft.bukkit.text.FcText_Bukkit
import net.benwoodworth.fastcraft.bukkit.world.FcItemStack_Bukkit
import net.benwoodworth.fastcraft.platform.player.FcPlayer
import net.benwoodworth.fastcraft.platform.text.FcTextConverter
import net.benwoodworth.localeconfig.api.LocaleApi
import org.bukkit.entity.Player
import org.junit.jupiter.api.Test
import java.util.Locale

class FcPlayerMessageTest {
    @Test
    fun sendsComponentDirectlyWithoutDispatchingTellraw() {
        val player = mockk<Player>()
        val converter = mockk<FcTextConverter>()
        val message = FcText_Bukkit.Component.Text("Command usage: /fastcraft")
        val formattedMessage = "§cCommand usage: /fastcraft"

        mockkStatic(LocaleApi::class)
        try {
            every { LocaleApi.getLocale(player) } returns Locale.ENGLISH
            every { converter.toLegacy(message, Locale.ENGLISH) } returns formattedMessage
            every { player.sendMessage(formattedMessage) } just Runs

            val operations = FcPlayer_Bukkit_1_7.Operations(
                converter,
                mockk(),
                mockk<FcSound_Bukkit.Operations>(),
                mockk<FcItemStack_Bukkit.Operations>(),
            )

            with(operations) {
                FcPlayer(player).sendMessage(message)
            }

            verify(exactly = 1) { converter.toLegacy(message, Locale.ENGLISH) }
            verify(exactly = 1) { player.sendMessage(formattedMessage) }
        } finally {
            unmockkStatic(LocaleApi::class)
        }
    }
}
