package net.OOHSHINY.commands

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
import net.OOHSHINY.OOHSHINYManager
import net.OOHSHINY.util.OOHSHINYMessages
import net.OOHSHINY.util.LuckPermsUtil
import java.util.concurrent.ConcurrentHashMap
import net.minecraft.util.Formatting
import net.minecraft.command.CommandSource
import com.mojang.brigadier.arguments.IntegerArgumentType

/**
 * Command handler for /oohshiny and all its subcommands.
 * 
 * Available commands:
 * - /oohshiny create <category> - Create a new category
 * - /oohshiny set [category] - Enter setup mode to create rewards
 * - /oohshiny remove - Enter remove mode to delete rewards
 * - /oohshiny list [category] - View all active reward locations
 * - /oohshiny give - Give chest, pokeball, or player head items
 * - /oohshiny reload - Reload data from disk
 * - /oohshiny reset <player> - Reset a player's claim history
 * - /oohshiny clearall - Delete all rewards (requires confirmation)
 * - /oohshiny completion add <category> <command> - Add a category completion command
 * - /oohshiny completion remove <category> <index> - Remove a category completion command
 * - /oohshiny completion list [category] - List category completion commands
 */
object OOHSHINYCommand {
    
    // Track pending confirmations for destructive commands to prevent accidental data loss
    private val pendingConfirmations: MutableMap<String, Long> = ConcurrentHashMap()
    private const val CONFIRMATION_TIMEOUT = 30000L // Confirmations expire after 30 seconds
    
    fun register(root: LiteralArgumentBuilder<ServerCommandSource>) {
        
        // /oohshiny create <category> - Create a new category
        root.then(
            literal("create")
                .requires { LuckPermsUtil.hasPermission(it, LuckPermsUtil.Permissions.OOHSHINY_COMMAND_CREATE) }
                .then(
                    argument("category", StringArgumentType.word())
                        .executes { context ->
                            val server = context.source.server
                            val category = StringArgumentType.getString(context, "category")
                            
                            OOHSHINYManager.createCategory(server, category)
                            context.source.sendFeedback({ 
                                Text.literal("Created category: $category").formatted(net.minecraft.util.Formatting.GREEN)
                            }, false)
                            1
                        }
                )
        )
        
        // /oohshiny set [category] - Enter setup mode with optional category
        root.then(
            literal("set")
                .requires { LuckPermsUtil.hasPermission(it, LuckPermsUtil.Permissions.OOHSHINY_COMMAND_SET) }
                .executes { context ->
                    val player = context.source.playerOrThrow
                    OOHSHINYManager.enableSetupMode(player, "default")
                    1
                }
                .then(
                    argument("category", StringArgumentType.word())
                        .suggests { context, builder ->
                            val server = context.source.server
                            val categories = OOHSHINYManager.getCategories(server)
                            CommandSource.suggestMatching(categories, builder)
                        }
                        .executes { context ->
                            val player = context.source.playerOrThrow
                            val category = StringArgumentType.getString(context, "category")
                            val server = context.source.server
                            val categories = OOHSHINYManager.getCategories(server)
                            
                            // Validate category exists
                            if (!categories.contains(category)) {
                                context.source.sendError(
                                    Text.literal("Unknown category: $category. Use /oohshiny create <category> to create it first.")
                                        .formatted(Formatting.RED)
                                )
                                return@executes 0
                            }
                            
                            OOHSHINYManager.enableSetupMode(player, category)
                            context.source.sendFeedback({
                                Text.literal("Setup mode enabled for category: $category").formatted(Formatting.GREEN)
                            }, false)
                            1
                        }
                )
        )
        
        // /oohshiny remove - Enter remove mode
        root.then(
            literal("remove")
                .requires { LuckPermsUtil.hasPermission(it, LuckPermsUtil.Permissions.OOHSHINY_COMMAND_REMOVE) }
                .executes { context ->
                    val player = context.source.playerOrThrow
                    OOHSHINYManager.enableRemoveMode(player)
                    1
                }
        )
        
        // /oohshiny give - Give special items
        root.then(
            literal("give")
                .requires { LuckPermsUtil.hasPermission(it, LuckPermsUtil.Permissions.OOHSHINY_COMMAND_GIVE) }
                .then(
                    // /oohshiny give chest <type>
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
                    // /oohshiny give pokeball <type>
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
        
        // /oohshiny list [category] - List all Ooh Shiny entries (optionally by category)
        root.then(
            literal("list")
                .requires { LuckPermsUtil.hasPermission(it, LuckPermsUtil.Permissions.OOHSHINY_COMMAND_LIST) }
                .executes { context ->
                    displayLootList(context.source, null)
                    1
                }
                .then(
                    argument("category", StringArgumentType.word())
                        .suggests { context, builder ->
                            val server = context.source.server
                            val categories = OOHSHINYManager.getCategories(server)
                            CommandSource.suggestMatching(categories, builder)
                        }
                        .executes { context ->
                            val category = StringArgumentType.getString(context, "category")
                            displayLootList(context.source, category)
                            1
                        }
                )
        )
        
        // /oohshiny reload - Reload Ooh Shiny data and language files
        root.then(
            literal("reload")
                .requires { LuckPermsUtil.hasPermission(it, LuckPermsUtil.Permissions.OOHSHINY_COMMAND_RELOAD) }
                .executes { context ->
                    val server = context.source.server
                    val count = OOHSHINYManager.reloadData(server)
                    net.OOHSHINY.util.LangManager.reload()
                    context.source.sendFeedback({ OOHSHINYMessages.dataReloaded(count) }, false)
                    context.source.sendFeedback({ 
                        Text.literal("Language file reloaded").formatted(net.minecraft.util.Formatting.GREEN) 
                    }, false)
                    1
                }
        )
        
        // /oohshiny reset <player> - Reset player's claims
        root.then(
            literal("reset")
                .requires { LuckPermsUtil.hasPermission(it, LuckPermsUtil.Permissions.OOHSHINY_COMMAND_RESET) }
                .then(
                    argument("target", GameProfileArgumentType.gameProfile())
                        .executes { context ->
                            val server = context.source.server
                            val targetProfiles = GameProfileArgumentType.getProfileArgument(context, "target")
                            
                            if (targetProfiles.isEmpty()) {
                                context.source.sendFeedback({ OOHSHINYMessages.noPlayerFound() }, false)
                                return@executes 0
                            }
                            
                            val targetProfile = targetProfiles.first()
                            val resetCount = OOHSHINYManager.resetPlayerClaims(server, targetProfile.id)
                            val playerName = targetProfile.name ?: targetProfile.id.toString()
                            
                            context.source.sendFeedback({ 
                                OOHSHINYMessages.playerClaimsReset(playerName, resetCount) 
                            }, false)
                            1
                        }
                )
        )
        
        // /oohshiny clearall - Clear all Ooh Shiny data (with confirmation)
        root.then(
            literal("clearall")
                .requires { LuckPermsUtil.hasPermission(it, LuckPermsUtil.Permissions.OOHSHINY_COMMAND_CLEARALL) }
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
                        val count = OOHSHINYManager.clearAllData(server)
                        
                        context.source.sendFeedback({ OOHSHINYMessages.allDataCleared(count) }, false)
                        pendingConfirmations.remove(confirmationKey)
                    } else {
                        // First execution - request confirmation by running the command again
                        pendingConfirmations[confirmationKey] = currentTime
                        context.source.sendFeedback({ 
                            OOHSHINYMessages.confirmationRequired("/oohshiny clearall")
                        }, false)
                    }
                    1
                }
        )
        
        // /oohshiny completion - Manage category completion commands
        root.then(
            literal("completion")
                .requires { LuckPermsUtil.hasPermission(it, LuckPermsUtil.Permissions.OOHSHINY_COMMAND_COMPLETION) }
                .then(
                    // /oohshiny completion add <category> <command>
                    literal("add")
                        .then(
                            argument("category", StringArgumentType.word())
                                .suggests { context, builder ->
                                    val server = context.source.server
                                    val categories = OOHSHINYManager.getCategories(server)
                                    CommandSource.suggestMatching(categories, builder)
                                }
                                .then(
                                    argument("command", StringArgumentType.greedyString())
                                        .executes { context ->
                                            val category = StringArgumentType.getString(context, "category")
                                            val command = StringArgumentType.getString(context, "command")
                                            val server = context.source.server
                                            val categories = OOHSHINYManager.getCategories(server)
                                            
                                            // Validate category exists
                                            if (!categories.contains(category)) {
                                                context.source.sendError(
                                                    Text.literal("Unknown category: $category. Use /oohshiny create <category> to create it first.")
                                                        .formatted(Formatting.RED)
                                                )
                                                return@executes 0
                                            }
                                            
                                            OOHSHINYManager.addCategoryCompletionCommand(category, command)
                                            context.source.sendFeedback({
                                                Text.literal("Added completion command for category '$category': $command")
                                                    .formatted(Formatting.GREEN)
                                            }, false)
                                            context.source.sendFeedback({
                                                Text.literal("Tip: Use {player} in the command to reference the player's name")
                                                    .formatted(Formatting.GRAY)
                                            }, false)
                                            1
                                        }
                                )
                        )
                )
                .then(
                    // /oohshiny completion remove <category> <index>
                    literal("remove")
                        .then(
                            argument("category", StringArgumentType.word())
                                .suggests { context, builder ->
                                    val server = context.source.server
                                    val categories = OOHSHINYManager.getCategories(server)
                                    CommandSource.suggestMatching(categories, builder)
                                }
                                .then(
                                    argument("index", IntegerArgumentType.integer(0))
                                        .executes { context ->
                                            val category = StringArgumentType.getString(context, "category")
                                            val index = IntegerArgumentType.getInteger(context, "index")
                                            
                                            val success = OOHSHINYManager.removeCategoryCompletionCommand(category, index)
                                            if (success) {
                                                context.source.sendFeedback({
                                                    Text.literal("Removed completion command #$index from category '$category'")
                                                        .formatted(Formatting.GREEN)
                                                }, false)
                                            } else {
                                                context.source.sendError(
                                                    Text.literal("Invalid index or no command at index $index for category '$category'")
                                                        .formatted(Formatting.RED)
                                                )
                                            }
                                            if (success) 1 else 0
                                        }
                                )
                        )
                )
                .then(
                    // /oohshiny completion list [category]
                    literal("list")
                        .executes { context ->
                            displayCompletionCommands(context.source, null)
                            1
                        }
                        .then(
                            argument("category", StringArgumentType.word())
                                .suggests { context, builder ->
                                    val server = context.source.server
                                    val categories = OOHSHINYManager.getCategories(server)
                                    CommandSource.suggestMatching(categories, builder)
                                }
                                .executes { context ->
                                    val category = StringArgumentType.getString(context, "category")
                                    displayCompletionCommands(context.source, category)
                                    1
                                }
                        )
                )
        )
    }
    
    /**
     * Displays completion commands, optionally filtered by category.
     */
    private fun displayCompletionCommands(source: ServerCommandSource, filterCategory: String?) {
        val server = source.server
        val categories = OOHSHINYManager.getCategories(server)
        
        // Validate category if specified
        if (filterCategory != null && !categories.contains(filterCategory)) {
            source.sendError(
                Text.literal("Unknown category: $filterCategory")
                    .formatted(Formatting.RED)
            )
            return
        }
        
        val allCommands = OOHSHINYManager.getAllCompletionCommands()
        
        if (allCommands.isEmpty()) {
            source.sendFeedback({ 
                Text.literal("No completion commands configured")
                    .formatted(Formatting.YELLOW)
            }, false)
            return
        }
        
        if (filterCategory == null) {
            // Show all completion commands grouped by category
            source.sendFeedback({ 
                Text.literal("Category Completion Commands:")
                    .formatted(Formatting.GREEN, Formatting.BOLD)
            }, false)
            
            allCommands.forEach { (category, commands) ->
                if (commands.isNotEmpty()) {
                    source.sendFeedback({
                        Text.literal("\n[Category: $category]")
                            .formatted(Formatting.AQUA, Formatting.BOLD)
                    }, false)
                    
                    commands.forEachIndexed { index, command ->
                        source.sendFeedback({
                            Text.literal("  [$index] $command")
                                .formatted(Formatting.WHITE)
                        }, false)
                    }
                }
            }
        } else {
            // Show only commands for specified category
            val commands = allCommands[filterCategory]
            
            if (commands == null || commands.isEmpty()) {
                source.sendFeedback({ 
                    Text.literal("No completion commands for category: $filterCategory")
                        .formatted(Formatting.YELLOW)
                }, false)
            } else {
                source.sendFeedback({ 
                    Text.literal("Completion commands for category '$filterCategory':")
                        .formatted(Formatting.GREEN)
                }, false)
                
                commands.forEachIndexed { index, command ->
                    source.sendFeedback({
                        Text.literal("  [$index] $command")
                            .formatted(Formatting.WHITE)
                    }, false)
                }
            }
        }
    }
    
    /**
     * Displays loot list, optionally filtered by category.
     */
    private fun displayLootList(source: ServerCommandSource, filterCategory: String?) {
        val server = source.server
        val categories = OOHSHINYManager.getCategories(server)
        
        // Validate category if specified
        if (filterCategory != null && !categories.contains(filterCategory)) {
            source.sendError(
                Text.literal("Unknown category: $filterCategory")
                    .formatted(Formatting.RED)
            )
            return
        }
        
        val allEntries = OOHSHINYManager.getAllLootEntries(server)
        
        if (allEntries.isEmpty()) {
            source.sendFeedback({ OOHSHINYMessages.noLootEntries() }, false)
            return
        }
        
        if (filterCategory == null) {
            // Show all entries grouped by category
            source.sendFeedback({ OOHSHINYMessages.lootListHeader(allEntries.size) }, false)
            
            categories.forEach { category ->
                val categoryEntries = allEntries.values.filter { it.category == category }
                if (categoryEntries.isNotEmpty()) {
                    source.sendFeedback({
                        Text.literal("\n[Category: $category]").formatted(Formatting.AQUA, Formatting.BOLD)
                    }, false)
                    
                    categoryEntries.forEach { entry ->
                        val itemNames = entry.rewardItems.joinToString(", ") { it.name.string }
                        val claimedCount = entry.claimedPlayers.size
                        source.sendFeedback({ 
                            OOHSHINYMessages.lootListEntry(entry.position, entry.dimension, itemNames, claimedCount, source) 
                        }, false)
                    }
                }
            }
        } else {
            // Show only entries from specified category
            val categoryEntries = allEntries.values.filter { it.category == filterCategory }
            
            if (categoryEntries.isEmpty()) {
                source.sendFeedback({ 
                    Text.literal("No loot entries found in category: $filterCategory").formatted(Formatting.YELLOW)
                }, false)
            } else {
                source.sendFeedback({ 
                    Text.literal("Loot entries in category '$filterCategory': ${categoryEntries.size}").formatted(Formatting.GREEN)
                }, false)
                
                categoryEntries.forEach { entry ->
                    val itemNames = entry.rewardItems.joinToString(", ") { it.name.string }
                    val claimedCount = entry.claimedPlayers.size
                    source.sendFeedback({ 
                        OOHSHINYMessages.lootListEntry(entry.position, entry.dimension, itemNames, claimedCount, source) 
                    }, false)
                }
            }
        }
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
        
        source.sendFeedback({ OOHSHINYMessages.itemGiven(name) }, false)
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
        
        source.sendFeedback({ OOHSHINYMessages.itemGiven(name) }, false)
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
