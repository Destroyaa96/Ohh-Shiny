package net.seto.ohhshiny.commands

import com.mojang.authlib.GameProfile
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.minecraft.command.argument.GameProfileArgumentType
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import net.seto.ohhshiny.OhhShinyManager
import net.seto.ohhshiny.util.OhhShinyMessages
import net.seto.ohhshiny.util.LuckPermsUtil
import java.util.concurrent.ConcurrentHashMap

/**
 * Command handler for /ohhshiny and all its subcommands.
 * 
 * Available commands:
 * - /ohhshiny set - Enter setup mode to create rewards
 * - /ohhshiny remove - Enter remove mode to delete rewards
 * - /ohhshiny list - View all active reward locations
 * - /ohhshiny reload - Reload data from disk
 * - /ohhshiny reset <player> - Reset a player's claim history
 * - /ohhshiny clearall - Delete all rewards (requires confirmation)
 */
object OhhShinyCommand {
    
    // Track pending confirmations for destructive commands to prevent accidental data loss
    private val pendingConfirmations: MutableMap<String, Long> = ConcurrentHashMap()
    private const val CONFIRMATION_TIMEOUT = 30000L // Confirmations expire after 30 seconds
    
    fun register(root: LiteralArgumentBuilder<ServerCommandSource>) {
        
        // /ohhshiny set - Enter setup mode
        root.then(
            literal("set")
                .requires { LuckPermsUtil.hasPermission(it, LuckPermsUtil.Permissions.OHHSHINY_SET) }
                .executes { context ->
                    val player = context.source.playerOrThrow
                    OhhShinyManager.enableSetupMode(player)
                    1
                }
        )
        
        // /ohhshiny remove - Enter remove mode
        root.then(
            literal("remove")
                .requires { LuckPermsUtil.hasPermission(it, LuckPermsUtil.Permissions.OHHSHINY_REMOVE) }
                .executes { context ->
                    val player = context.source.playerOrThrow
                    OhhShinyManager.enableRemoveMode(player)
                    1
                }
        )
        
        // /ohhshiny list - List all Ohh Shiny entries
        root.then(
            literal("list")
                .requires { LuckPermsUtil.hasPermission(it, LuckPermsUtil.Permissions.OHHSHINY_LIST) }
                .executes { context ->
                    val server = context.source.server
                    val entries = OhhShinyManager.getAllLootEntries(server)
                    
                    if (entries.isEmpty()) {
                        context.source.sendFeedback({ OhhShinyMessages.noLootEntries() }, false)
                    } else {
                        context.source.sendFeedback({ OhhShinyMessages.lootListHeader(entries.size) }, false)
                        
                        entries.values.forEach { entry ->
                            val itemName = entry.rewardItem.name.string
                            val claimedCount = entry.claimedPlayers.size
                            context.source.sendFeedback({ 
                                OhhShinyMessages.lootListEntry(entry.position, entry.dimension, itemName, claimedCount) 
                            }, false)
                        }
                    }
                    1
                }
        )
        
        // /ohhshiny reload - Reload Ohh Shiny data
        root.then(
            literal("reload")
                .requires { LuckPermsUtil.hasPermission(it, LuckPermsUtil.Permissions.OHHSHINY_RELOAD) }
                .executes { context ->
                    val server = context.source.server
                    val count = OhhShinyManager.reloadData(server)
                    context.source.sendFeedback({ OhhShinyMessages.dataReloaded(count) }, false)
                    1
                }
        )
        
        // /ohhshiny reset <player> - Reset player's claims
        root.then(
            literal("reset")
                .requires { LuckPermsUtil.hasPermission(it, LuckPermsUtil.Permissions.OHHSHINY_RESET) }
                .then(
                    argument("target", GameProfileArgumentType.gameProfile())
                        .executes { context ->
                            val server = context.source.server
                            val targetProfiles = GameProfileArgumentType.getProfileArgument(context, "target")
                            
                            if (targetProfiles.isEmpty()) {
                                context.source.sendFeedback({ Text.literal("No valid player found").formatted(net.minecraft.util.Formatting.RED) }, false)
                                return@executes 0
                            }
                            
                            val targetProfile = targetProfiles.first()
                            val resetCount = OhhShinyManager.resetPlayerClaims(server, targetProfile.id)
                            val playerName = targetProfile.name ?: targetProfile.id.toString()
                            
                            context.source.sendFeedback({ 
                                OhhShinyMessages.playerClaimsReset(playerName, resetCount) 
                            }, false)
                            1
                        }
                )
        )
        
        // /ohhshiny clearall - Clear all Ohh Shiny data (with confirmation)
        root.then(
            literal("clearall")
                .requires { LuckPermsUtil.hasPermission(it, LuckPermsUtil.Permissions.OHHSHINY_CLEARALL) }
                .executes { context ->
                    val playerName = context.source.name
                    val currentTime = System.currentTimeMillis()
                    val confirmationKey = "${playerName}_clearall"
                    
                    // Remove expired confirmations from the cache
                    pendingConfirmations.entries.removeIf { (_, time) -> 
                        currentTime - time > CONFIRMATION_TIMEOUT 
                    }
                    
                    if (pendingConfirmations.containsKey(confirmationKey)) {
                        // Second execution within 30 seconds - proceed with deletion
                        val server = context.source.server
                        val count = OhhShinyManager.clearAllData(server)
                        
                        context.source.sendFeedback({ OhhShinyMessages.allDataCleared(count) }, false)
                        pendingConfirmations.remove(confirmationKey)
                    } else {
                        // First execution - request confirmation by running the command again
                        pendingConfirmations[confirmationKey] = currentTime
                        context.source.sendFeedback({ 
                            OhhShinyMessages.confirmationRequired("/ohhshiny clearall") 
                        }, false)
                    }
                    1
                }
        )
    }
}
