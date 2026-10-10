package net.benwoodworth.fastcraft.bukkit.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BukkitVersionTest {
    @Test
    fun parsesPaper12111() {
        assertEquals(
            BukkitVersion(1, 21, 11, 0, 1),
            BukkitVersion.parse("1.21.11-R0.1-SNAPSHOT"),
        )
    }

    @Test
    fun comparesSemanticComponentsNumerically() {
        assertTrue(BukkitVersion.parse("1.21.11") > BukkitVersion.parse("1.21.10"))
        assertTrue(BukkitVersion.parse("1.21.11") > BukkitVersion.parse("1.15.2"))
    }

    @Test
    fun rejectsMalformedReleaseComponents() {
        assertNull(BukkitVersion.parseOrNull("1.21.11-R0x1-SNAPSHOT"))
        assertNull(BukkitVersion.parseOrNull("1.21.11xR0.1-SNAPSHOT"))
    }
}
