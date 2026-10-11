package net.benwoodworth.fastcraft.paper

import org.junit.jupiter.api.Test
import kotlin.test.assertContains
import kotlin.test.assertNotNull

class FastCorePaperDescriptorTest {
    @Test
    fun descriptorTargetsPaper12111Only() {
        val descriptor = assertNotNull(
            javaClass.classLoader.getResourceAsStream("plugin.yml"),
            "FastCraft's Paper plugin.yml must be packaged"
        ).bufferedReader().use { it.readText() }

        assertContains(descriptor, "api-version: \"1.21.11\"")
        assertContains(descriptor, "main: \"net.benwoodworth.fastcraft.paper.FastCorePaper\"")
    }
}
