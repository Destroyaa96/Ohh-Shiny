package net.ohhshiny.commands

import com.mojang.authlib.GameProfile
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.authlib.properties.Property
import net.minecraft.command.argument.GameProfileArgumentType
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.ProfileComponent
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import java.util.*
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import net.ohhshiny.OhhShinyManager
import net.ohhshiny.util.OhhShinyMessages
import net.ohhshiny.util.LuckPermsUtil
import java.util.concurrent.ConcurrentHashMap

/**
 * Command handler for /ohhshiny and all its subcommands.
 * 
 * Available commands:
 * - /ohhshiny set - Enter setup mode to create rewards
 * - /ohhshiny remove - Enter remove mode to delete rewards
 * - /ohhshiny list - View all active reward locations
 * - /ohhshiny give - Give chest, pokeball, or player head items
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
        
        // /ohhshiny give - Give special items
        root.then(
            literal("give")
                .requires { LuckPermsUtil.hasPermission(it, LuckPermsUtil.Permissions.OHHSHINY_GIVE) }
                .then(
                    // /ohhshiny give chest <type>
                    literal("chest")
                        .then(
                            literal("copper")
                                .executes { context ->
                                    giveChest(context.source, "copper")
                                    1
                                }
                        )
                        .then(
                            literal("iron")
                                .executes { context ->
                                    giveChest(context.source, "iron")
                                    1
                                }
                        )
                        .then(
                            literal("gold")
                                .executes { context ->
                                    giveChest(context.source, "gold")
                                    1
                                }
                        )
                )
                .then(
                    // /ohhshiny give pokeball <type>
                    literal("pokeball")
                        .then(
                            literal("poke")
                                .executes { context ->
                                    givePokeball(context.source, "poke")
                                    1
                                }
                        )
                        .then(
                            literal("ultra")
                                .executes { context ->
                                    givePokeball(context.source, "ultra")
                                    1
                                }
                        )
                        .then(
                            literal("master")
                                .executes { context ->
                                    givePokeball(context.source, "master")
                                    1
                                }
                        )
                )
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
                                OhhShinyMessages.lootListEntry(entry.position, entry.dimension, itemName, claimedCount, context.source) 
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
    
    /**
     * Gives a chest item (custom texture player head) to the player based on type.
     */
    private fun giveChest(source: ServerCommandSource, type: String) {
        val player = source.playerOrThrow
        
        val (texture, name) = when (type.lowercase()) {
            "copper" -> Pair(
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOWM2OTgyN2FlZjQ4N2E1MmU2NGZkYmE5NmVhOTZkOWY0ZTM2ZGM1NDRmMDMzMjI3N2E2ZTY3ZjQ5YWNmYjc0ZCJ9fX0=",
                "Copper Chest"
            )
            "iron" -> Pair(
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmIxNDFmNTZjYzUxNDdmZTQxMDM0OGU5NDM0NWQxNDhlN2M2NzliMzIxMjMzNWNiM2U4OGZkOWQ2Zjg0MDgwNiJ9fX0=",
                "Iron Chest"
            )
            "gold" -> Pair(
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYWNmMjY2NTNmYjU3M2Q3MDM0ZmZhMzZiNTE0Nzk1MDY0ZTc3Njc5YzBkOGI1YTkwZjc0ODUwYTExODhiNzBiNCJ9fX0=",
                "Gold Chest"
            )
            else -> Pair("", "Chest")
        }
        
        val chestItem = createCustomTextureHead(texture, name)
        
        if (!player.giveItemStack(chestItem)) {
            player.dropItem(chestItem, false)
        }
        
        source.sendFeedback({ 
            Text.literal("Given $name").formatted(net.minecraft.util.Formatting.GREEN) 
        }, false)
    }
    
    /**
     * Gives a pokeball item (custom texture player head) to the player based on type.
     */
    private fun givePokeball(source: ServerCommandSource, type: String) {
        val player = source.playerOrThrow
        
        val (texture, name) = when (type.lowercase()) {
            "poke" -> Pair(
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOGRlYjQ3ZDQ3YzI4YTZhNDNmYzViOGQwZmE0NmIyZGVmYmFjOWNiZDVhYjM0NDgwMzI1YTI0NTliZTExMGY0In19fQ==",
                "Poke Ball"
            )
            "ultra" -> Pair(
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzcxODU3ZmFlM2MyMThkMDYwOThmYTY2MWVkOWQ0NDRmOWE5MjI0YTZlMmQ4M2I5NmIwODQzYmQxYmNmMWQyIn19fQ==",
                "Ultra Ball"
            )
            "master" -> Pair(
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNmZiOTk5ZTNhYmQ1ZTNjNWI4M2I4ZTRkNmJkMmQxMjQ4OGY1YmRmNzQxMWUxZGQ5YWZhZTI1NmFlZjQ3In19fQ==",
                "Master Ball"
            )
            else -> Pair("", "Ball")
        }
        
        val pokeballItem = createCustomTextureHead(texture, name)
        
        if (!player.giveItemStack(pokeballItem)) {
            player.dropItem(pokeballItem, false)
        }
        
        source.sendFeedback({ 
            Text.literal("Given $name").formatted(net.minecraft.util.Formatting.GREEN) 
        }, false)
    }
    
    
    /**
     * Creates a player head with a custom texture from a base64-encoded texture value.
     */
    private fun createCustomTextureHead(textureBase64: String, displayName: String): ItemStack {
        val profile = GameProfile(UUID.randomUUID(), "")
        profile.properties.put("textures", Property("textures", textureBase64))
        
        return ItemStack(Items.PLAYER_HEAD).apply {
            set(DataComponentTypes.PROFILE, ProfileComponent(profile))
            set(DataComponentTypes.CUSTOM_NAME, Text.literal(displayName))
        }
    }
}
