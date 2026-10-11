package net.benwoodworth.fastcraft.bukkit.player

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import net.benwoodworth.fastcraft.bukkit.text.FcText_Bukkit
import net.benwoodworth.fastcraft.bukkit.world.FcItemStack_Bukkit
import net.benwoodworth.fastcraft.platform.player.FcPlayer
import net.benwoodworth.fastcraft.platform.text.FcTextConverter
import org.bukkit.entity.Player
import org.junit.jupiter.api.Test

class FcPlayerMessageTest {
    @Test
    fun sendsComponentDirectlyWithoutDispatchingTellraw() {
        val player = mockk<Player>(relaxed = true)
        val converter = mockk<FcTextConverter>()
        val message = FcText_Bukkit.Component.Text("Command usage: /fastcraft")
        val formattedMessage = "§cCommand usage: /fastcraft"

        // Keep the test independent of static LocaleApi instrumentation and
        // the player's locale string, which varies across Bukkit API versions.
        every { converter.toLegacy(message, any()) } returns formattedMessage
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

        verify(exactly = 1) { converter.toLegacy(message, any()) }
        verify(exactly = 1) { player.sendMessage(formattedMessage) }
    }
}
