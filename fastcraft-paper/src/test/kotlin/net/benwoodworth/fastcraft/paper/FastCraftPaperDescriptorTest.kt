package net.benwoodworth.fastcraft.paper

import org.junit.jupiter.api.Test
import kotlin.test.assertContains
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class FastCraftPaperDescriptorTest {
    @Test
    fun descriptorTargetsPaper12111Only() {
        val descriptor = assertNotNull(
            javaClass.classLoader.getResourceAsStream("paper-plugin.yml"),
            "FastCraft's paper-plugin.yml must be packaged"
        ).bufferedReader().use { it.readText() }

        assertNull(javaClass.classLoader.getResource("plugin.yml"))
        assertContains(descriptor, "api-version: \"1.21.11\"")
        assertContains(descriptor, "main: \"net.benwoodworth.fastcraft.paper.FastCraftPaper\"")
    }
}
