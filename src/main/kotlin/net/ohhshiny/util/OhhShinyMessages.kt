package net.ohhshiny.util

import net.minecraft.item.ItemStack
import net.minecraft.registry.RegistryKey
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.ClickEvent
import net.minecraft.text.HoverEvent
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

/**
 * Message formatting utilities for Ohh Shiny system feedback.
 * All messages are loaded from LangManager and support TextPlaceholderAPI placeholders.
 */
object OhhShinyMessages {
    
    /**
     * Helper function to prepend prefix to messages.
     */
    private fun withPrefix(message: String): String {
        val prefix = LangManager.getMessage("prefix")
        return if (prefix.isNotEmpty()) "$prefix $message" else message
    }
    
    /**
     * Creates a Text object from a string, parsing formatting codes via TextPlaceholderAPI.
     * Falls back to literal text if TextPlaceholderAPI is not available.
     */
    private fun createText(message: String, player: ServerPlayerEntity? = null): Text {
        return TextPlaceholderUtil.parseText(withPrefix(message), player)
    }
    
    // Success messages (Green/Aqua)
    fun lootCreated(position: BlockPos, dimension: RegistryKey<World>, itemName: String): Text {
        val message = LangManager.getMessage("loot.created", mapOf(
            "x" to position.x.toString(),
            "y" to position.y.toString(),
            "z" to position.z.toString(),
            "dimension" to dimension.value.toString(),
            "item" to itemName
        ))
        return createText(message)
    }
    
    fun lootClaimed(itemStack: ItemStack, player: ServerPlayerEntity? = null): Text {
        val itemName = itemStack.name.string
        val message = LangManager.getMessage("loot.claimed", mapOf("item" to itemName))
        return createText(message, player)
    }
    
    fun lootRemoved(position: BlockPos, dimension: RegistryKey<World>): Text {
        val message = LangManager.getMessage("loot.removed", mapOf(
            "x" to position.x.toString(),
            "y" to position.y.toString(),
            "z" to position.z.toString(),
            "dimension" to dimension.value.toString()
        ))
        return createText(message)
    }
    
    // Setup mode messages (Yellow)
    fun setupModeEnabled(): Text {
        val message = LangManager.getMessage("setup.enabled")
        return createText(message)
    }
    
    fun removeModeEnabled(): Text {
        val message = LangManager.getMessage("remove.enabled")
        return createText(message)
    }
    
    // Error messages (Red)
    fun alreadyClaimed(player: ServerPlayerEntity? = null): Text {
        val message = LangManager.getMessage("error.already_claimed")
        return createText(message, player)
    }
    
    fun noLootAtLocation(): Text {
        val message = LangManager.getMessage("error.no_loot")
        return createText(message)
    }
    
    fun emptyHandError(): Text {
        val message = LangManager.getMessage("error.empty_hand")
        return createText(message)
    }
    
    fun noPermission(permission: String): Text {
        val message = LangManager.getMessage("error.no_permission", mapOf("permission" to permission))
        return createText(message)
    }
    
    fun permissionServiceMissing(): Text {
        val message = LangManager.getMessage("error.permission_service")
        return createText(message)
    }
    
    // Informational messages
    fun lootListHeader(count: Int): Text {
        val message = LangManager.getMessage("list.header", mapOf("count" to count.toString()))
        return createText(message)
    }
    
    fun lootListEntry(position: BlockPos, dimension: RegistryKey<World>, itemName: String, claimedCount: Int, source: ServerCommandSource): Text {
        val coordText = "[${position.x}, ${position.y}, ${position.z}]"
        val hasTeleportPerm = LuckPermsUtil.hasPermission(source, LuckPermsUtil.Permissions.OHHSHINY_TELEPORT)
        
        val message = Text.literal("• ")
        
        if (hasTeleportPerm) {
            // Make coordinates clickable with teleport command
            val teleportY = position.y + 2
            val teleportCommand = "/execute in ${dimension.value} run tp @s ${position.x} ${teleportY} ${position.z}"
            val hoverMessage = LangManager.getMessage("list.teleport_hover")
            
            val clickableCoords = Text.literal(coordText)
                .formatted(Formatting.AQUA, Formatting.UNDERLINE)
                .styled { style ->
                    style
                        .withClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, teleportCommand))
                        .withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(hoverMessage).formatted(Formatting.YELLOW)))
                }
            
            message.append(clickableCoords)
        } else {
            // No teleport permission - display coordinates normally
            message.append(Text.literal(coordText).formatted(Formatting.WHITE))
        }
        
        message.append(Text.literal(" in ${dimension.value}: ${itemName} (claimed by ${claimedCount} players)").formatted(Formatting.WHITE))
        
        return message
    }
    
    fun noLootEntries(): Text {
        val message = LangManager.getMessage("list.empty")
        return createText(message)
    }
    
    fun dataReloaded(count: Int): Text {
        val message = LangManager.getMessage("admin.reload", mapOf("count" to count.toString()))
        return createText(message)
    }
    
    fun playerClaimsReset(playerName: String, resetCount: Int): Text {
        val message = LangManager.getMessage("admin.reset", mapOf(
            "count" to resetCount.toString(),
            "player" to playerName
        ))
        return createText(message)
    }
    
    fun allDataCleared(count: Int): Text {
        val message = LangManager.getMessage("admin.cleared", mapOf("count" to count.toString()))
        return createText(message)
    }
    
    fun confirmationRequired(command: String): Text {
        val message = LangManager.getMessage("admin.confirm", mapOf("command" to command))
        return createText(message)
    }
    
    // Setup mode exit messages
    fun setupModeDisabled(): Text {
        val message = LangManager.getMessage("setup.disabled")
        return createText(message)
    }
    
    fun removeModeDisabled(): Text {
        val message = LangManager.getMessage("remove.disabled")
        return createText(message)
    }
    
    fun blockProtected(player: ServerPlayerEntity? = null): Text {
        val message = LangManager.getMessage("error.block_protected")
        return createText(message, player)
    }
    
    fun itemGiven(itemName: String): Text {
        val message = LangManager.getMessage("admin.given", mapOf("item" to itemName))
        return createText(message)
    }
    
    fun noPlayerFound(): Text {
        val message = LangManager.getMessage("error.no_player")
        return createText(message)
    }
}
