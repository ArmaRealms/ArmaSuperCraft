package net.benwoodworth.fastcraft.bukkit.gui

import io.mockk.mockk
import io.mockk.verify
import net.benwoodworth.fastcraft.bukkit.text.FcText_Bukkit_Paper
import net.benwoodworth.fastcraft.bukkit.world.FcItem_Bukkit_Paper
import net.benwoodworth.fastcraft.bukkit.world.FcItemStack_Bukkit_Paper
import net.benwoodworth.fastcraft.bukkit.text.FcTextConverter_Bukkit_Paper
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
    private val textFactory = FcText_Bukkit_Paper.Factory()
    private val itemFactory = FcItem_Bukkit_Paper.Factory()
    private val itemOperations = FcItem_Bukkit_Paper.Operations(textFactory, itemFactory)
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
            fcTextFactory = textFactory,
            fcTextConverter = mockk<FcTextConverter_Bukkit_Paper>(),
            fcItemOperations = itemOperations,
            fcItemStackOperations = stackOperations,
        )
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
