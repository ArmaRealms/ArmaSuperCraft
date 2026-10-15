package net.benwoodworth.fastcraft.paper

import com.mojang.brigadier.Command
import com.mojang.brigadier.tree.LiteralCommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import net.benwoodworth.fastcraft.FastCraftCommand
import net.benwoodworth.fastcraft.bukkit.command.FcCommandSource_Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import javax.inject.Inject

/**
 * Native Paper command. Reuses the existing preference and GUI services while the
 * original /fastcraft administrative command remains available for compatibility.
 */
class CraftCommand @Inject constructor(
    private val fastCraftCommand: FastCraftCommand,
    private val commandSourceFactory: FcCommandSource_Bukkit.Factory,
) {
    fun register(plugin: FastCraftPaper) {
        plugin.lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
            event.registrar().register(createTree(), "Abrir e configurar o Super Craft")
        }
    }

    internal fun createTree(): LiteralCommandNode<CommandSourceStack> =
        Commands.literal("craft")
            .executes { context -> open(context.source.sender) }
            .then(
                Commands.literal("alternar")
                    .requires { it.sender.hasPermission(SET_ENABLED) }
                    .executes { context -> setEnabled(context.source.sender, toggle = true) }
            )
            .then(
                Commands.literal("on")
                    .requires { it.sender.hasPermission(SET_ENABLED) }
                    .executes { context -> setEnabled(context.source.sender, enabled = true) }
            )
            .then(
                Commands.literal("off")
                    .requires { it.sender.hasPermission(SET_ENABLED) }
                    .executes { context -> setEnabled(context.source.sender, enabled = false) }
            )
            .build()

    private fun open(sender: CommandSender): Int {
        fastCraftCommand.fcCraft(commandSourceFactory.create(sender), "default")
        return if (sender is Player && (sender.hasPermission(OPEN_CRAFT) || sender.hasPermission(OPEN_GRID))) {
            Command.SINGLE_SUCCESS
        } else 0
    }

    private fun setEnabled(sender: CommandSender, enabled: Boolean = false, toggle: Boolean = false): Int {
        fastCraftCommand.fcSetEnabled(commandSourceFactory.create(sender), enabled, toggle)
        return if (sender is Player && sender.hasPermission(SET_ENABLED)) Command.SINGLE_SUCCESS else 0
    }

    private companion object {
        const val OPEN_CRAFT = "fastcraft.command.craft.fastcraft"
        const val OPEN_GRID = "fastcraft.command.craft.grid"
        const val SET_ENABLED = "fastcraft.command.set.enabled"
    }
}
