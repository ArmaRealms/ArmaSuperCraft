package net.benwoodworth.fastcraft.bukkit.gui

import io.mockk.mockk
import io.mockk.verify
import io.mockk.every
import net.benwoodworth.fastcraft.bukkit.world.FcItem_Bukkit_Paper
import net.benwoodworth.fastcraft.bukkit.world.FcItemStack_Bukkit_Paper
import net.benwoodworth.fastcraft.bukkit.player.FcPlayer_Bukkit
import net.benwoodworth.fastcraft.platform.player.FcPlayer
import net.benwoodworth.fastcraft.text.miniMessage
import net.kyori.adventure.text.Component
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.PluginManager
import org.bukkit.Material
import org.bukkit.Server
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockbukkit.mockbukkit.MockBukkitExtension
import java.util.Locale

@ExtendWith(MockBukkitExtension::class)
class FcGuiButtonPaperTest {
    private val itemFactory = FcItem_Bukkit_Paper.Factory()
    private val itemOperations = FcItem_Bukkit_Paper.Operations(itemFactory)
    private val stackFactory = FcItemStack_Bukkit_Paper.Factory(itemFactory, mockk<Server>(), itemOperations)
    private val stackOperations = FcItemStack_Bukkit_Paper.Operations(itemFactory, itemOperations, stackFactory)
    private val inventory = mockk<Inventory>(relaxed = true)
    private lateinit var button: FcGuiButton_Bukkit_Paper

    @BeforeEach
    fun createButton() {
        button = FcGuiButton_Bukkit_Paper(
            inventory = inventory,
            slotIndex = 0,
            locale = Locale.US,
            fcItemOperations = itemOperations,
            fcItemStackOperations = stackOperations,
        )
    }

    @Test
    fun preservesMiniMessageButtonTitleAndLore() {
        val title = miniMessage("<gold><bold>Title")
        val lore = miniMessage("<green>Item: <item>", components = mapOf("item" to Component.translatable("item.minecraft.diamond")))
        button.setItem(itemFactory.netherStar)
        button.setText(title)
        button.setDescription(listOf(lore))
        verify {
            inventory.setItem(0, match<ItemStack> {
                it.itemMeta.customName() == title && it.lore() == listOf(lore)
            })
        }
    }

    @Test
    fun passesMiniMessageTitleToNativeInventoryCreation() {
        val server = mockk<Server>()
        val layouts = mockk<FcGuiLayout_Bukkit.Factory>()
        val playerOperations = mockk<FcPlayer_Bukkit.Operations>()
        val player = FcPlayer(Any())
        val title = miniMessage("<gold>Custom title")
        with(playerOperations) { every { player.locale } returns Locale.US }
        every { server.createInventory(any(), 27, title) } returns inventory
        every { layouts.createGridLayout(9, 3, inventory, Locale.US) } returns mockk(relaxed = true)
        val factory = FcGui_Bukkit_Paper.Factory(mockk<Plugin>(), mockk<PluginManager>(relaxed = true), server, layouts, playerOperations)

        factory.createChestGui(player, title, 3)

        verify(exactly = 1) { server.createInventory(any(), 27, title) }
    }

    @Test
    fun createsVisibleButtonFromAnEmptySlot() {
        button.setItem(itemFactory.netherStar)

        verify {
            inventory.setItem(0, match<ItemStack> {
                it.type == Material.NETHER_STAR && it.amount == 1
            })
        }

        button.clear()
        button.setItem(itemFactory.ironSword)
        verify {
            inventory.setItem(0, match<ItemStack> {
                it.type == Material.IRON_SWORD && it.amount == 1
            })
        }
    }

    @Test
    fun settingAirClearsTheSlot() {
        button.setItem(itemFactory.netherStar)
        button.setItem(itemFactory.air)

        verify {
            inventory.setItem(0, match<ItemStack> {
                it.type == Material.AIR && it.amount == 0
            })
        }
    }

    @Test
    fun preservesExistingStackAmountWhenChangingButtonMaterial() {
        button.setItem(itemFactory.netherStar)
        button.setAmount(5)
        button.setItem(itemFactory.ironSword)

        verify {
            inventory.setItem(0, match<ItemStack> {
                it.type == Material.IRON_SWORD && it.amount == 5
            })
        }
    }
}
