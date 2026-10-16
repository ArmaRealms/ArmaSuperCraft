package net.benwoodworth.fastcraft.paper

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.tree.LiteralCommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import net.benwoodworth.fastcraft.FastCraftCommand
import net.benwoodworth.fastcraft.bukkit.command.FcCommandSource_Bukkit
import net.benwoodworth.fastcraft.platform.command.FcCommandSource
import org.bukkit.Server
import org.bukkit.command.CommandSender
import javax.inject.Inject

/** Native Paper commands. The core service still performs permission checks before actions. */
class CraftCommand @Inject constructor(
    private val fastCraftCommand: FastCraftCommand,
    private val commandSourceFactory: FcCommandSource_Bukkit.Factory,
    private val server: Server,
) {
    fun register(plugin: FastCraftPaper) {
        plugin.lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
            event.registrar().register(createTree(), "Abrir e configurar o Super Craft")
            event.registrar().register(createAdminTree(), "Administrar o Super Craft", listOf("fc"))
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

    internal fun createAdminTree(): LiteralCommandNode<CommandSourceStack> =
        Commands.literal("fastcraft")
            .requires {
                it.sender.hasPermission(ADMIN_SET_ENABLED) ||
                    it.sender.hasPermission(ADMIN_CRAFT) ||
                    it.sender.hasPermission(ADMIN_RELOAD)
            }
            .then(
                Commands.literal("set")
                    .requires { it.sender.hasPermission(ADMIN_SET_ENABLED) }
                    .then(
                        Commands.literal("enabled")
                            .then(adminOption("true") { source, player ->
                                fastCraftCommand.fcSetEnabledAdmin(source, player, enabled = true)
                            })
                            .then(adminOption("false") { source, player ->
                                fastCraftCommand.fcSetEnabledAdmin(source, player, enabled = false)
                            })
                            .then(adminOption("toggle") { source, player ->
                                fastCraftCommand.fcSetEnabledAdmin(source, player, toggle = true)
                            })
                    )
            )
            .then(
                Commands.literal("craft")
                    .requires { it.sender.hasPermission(ADMIN_CRAFT) }
                    .then(adminOption("fastcraft") { source, player ->
                        fastCraftCommand.fcCraftAdmin(source, "fastcraft", player)
                    })
                    .then(adminOption("grid") { source, player ->
                        fastCraftCommand.fcCraftAdmin(source, "grid", player)
                    })
                    .then(adminOption("default") { source, player ->
                        fastCraftCommand.fcCraftAdmin(source, "default", player)
                    })
            )
            .then(
                Commands.literal("reload")
                    .requires { it.sender.hasPermission(ADMIN_RELOAD) }
                    .executes { context ->
                        result(fastCraftCommand.fcReload(commandSourceFactory.create(context.source.sender)))
                    }
            )
            .build()

    private fun adminOption(
        name: String,
        action: (FcCommandSource, String) -> Boolean,
    ): LiteralArgumentBuilder<CommandSourceStack> =
        Commands.literal(name).then(
            Commands.argument("player", StringArgumentType.word())
                .suggests { _, builder ->
                    server.onlinePlayers
                        .map { it.name }
                        .filter { it.startsWith(builder.remaining, ignoreCase = true) }
                        .forEach { builder.suggest(it) }
                    builder.buildFuture()
                }
                .executes { context ->
                    result(action(
                        commandSourceFactory.create(context.source.sender),
                        StringArgumentType.getString(context, "player"),
                    ))
                }
        )

    private fun open(sender: CommandSender): Int =
        result(fastCraftCommand.fcCraft(commandSourceFactory.create(sender), "default"))

    private fun setEnabled(sender: CommandSender, enabled: Boolean = false, toggle: Boolean = false): Int =
        result(fastCraftCommand.fcSetEnabled(commandSourceFactory.create(sender), enabled, toggle))

    private fun result(success: Boolean): Int = if (success) Command.SINGLE_SUCCESS else 0

    private companion object {
        const val SET_ENABLED = "fastcraft.command.set.enabled"

        const val ADMIN_CRAFT = "fastcraft.admin.command.craft"
        const val ADMIN_SET_ENABLED = "fastcraft.admin.command.set.enabled"
        const val ADMIN_RELOAD = "fastcraft.admin.command.reload"
    }
}
