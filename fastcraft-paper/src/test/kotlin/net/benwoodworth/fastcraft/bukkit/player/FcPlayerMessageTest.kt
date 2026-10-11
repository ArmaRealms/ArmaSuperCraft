package net.benwoodworth.fastcraft.bukkit.player

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.benwoodworth.fastcraft.bukkit.text.FcText_Bukkit
import net.benwoodworth.fastcraft.bukkit.text.FcTextConverter_Bukkit
import net.benwoodworth.fastcraft.bukkit.world.FcItemStack_Bukkit
import net.benwoodworth.fastcraft.platform.player.FcPlayer
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.junit.jupiter.api.Test

class FcPlayerMessageTest {
    @Test
    fun sendsAdventureComponentWithoutLegacySerialization() {
        val player = mockk<Player>(relaxed = true)
        val converter = mockk<FcTextConverter_Bukkit>()
        val message = FcText_Bukkit.Component.Translate("block.minecraft.stone")
        val component = Component.translatable("block.minecraft.stone")
        every { converter.toComponent(message) } returns component

        val operations = FcPlayer_Bukkit_Paper.Operations(
            converter,
            mockk(),
            mockk<FcSound_Bukkit.Operations>(),
            mockk<FcItemStack_Bukkit.Operations>(),
        )
        with(operations) { FcPlayer(player).sendMessage(message) }

        verify(exactly = 1) { converter.toComponent(message) }
        verify(exactly = 1) { player.sendMessage(component) }
        verify(exactly = 0) { converter.toLegacy(any(), any()) }
    }
}
