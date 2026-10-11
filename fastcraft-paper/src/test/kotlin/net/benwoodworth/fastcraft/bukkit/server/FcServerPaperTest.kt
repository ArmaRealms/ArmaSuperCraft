package net.benwoodworth.fastcraft.bukkit.server

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.bukkit.Server
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class FcServerPaperTest {
    @Test
    fun usesMinecraftVersionWithoutInspectingServerBrandOrVersionBanner() {
        val server = mockk<Server>()
        every { server.minecraftVersion } returns "1.21.11"

        val adapter = FcServer_Bukkit_Paper(server)

        assertEquals("1.21.11", adapter.minecraftVersion)
        verify(exactly = 0) { server.name }
        verify(exactly = 0) { server.version }
        verify(exactly = 0) { server.bukkitVersion }
    }

    @Test
    fun doesNotRejectAlternativeMinecraftVersions() {
        val server = mockk<Server>()
        every { server.minecraftVersion } returns "1.22"

        val adapter = FcServer_Bukkit_Paper(server)

        // The version only identifies Minecraft assets; it does not gate startup.
        assertEquals("1.22", adapter.minecraftVersion)
    }
}
