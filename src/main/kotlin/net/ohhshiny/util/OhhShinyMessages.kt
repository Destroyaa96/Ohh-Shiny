package net.seto.ohhshiny.util

import net.minecraft.item.ItemStack
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.ClickEvent
import net.minecraft.text.HoverEvent
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.math.BlockPos
import net.minecraft.registry.RegistryKey
import net.minecraft.world.World

/**
 * Message constants and formatting utilities for Ohh Shiny system feedback
 * Following the ResortCore pattern of using Text.literal() with consistent formatting
 */
object OhhShinyMessages {
    
    // Success messages (Green/Aqua)
    fun lootCreated(position: BlockPos, dimension: RegistryKey<World>, itemName: String): Text {
        return Text.literal("Ohh Shiny set at [${position.x}, ${position.y}, ${position.z}] in ${dimension.value} with ${itemName}")
            .formatted(Formatting.GREEN)
    }
    
    fun lootClaimed(itemStack: ItemStack): Text {
        return Text.literal("You found something shiny: ")
            .append(itemStack.name)
            .append(Text.literal("!"))
            .formatted(Formatting.AQUA)
    }
    
    fun lootRemoved(position: BlockPos, dimension: RegistryKey<World>): Text {
        return Text.literal("Removed Ohh Shiny at [${position.x}, ${position.y}, ${position.z}] in ${dimension.value}")
            .formatted(Formatting.GREEN)
    }
    
    // Setup mode messages (Yellow)
    fun setupModeEnabled(): Text {
        return Text.literal("Right-click a block to bind your held item as Ohh Shiny")
            .formatted(Formatting.YELLOW)
    }
    
    fun removeModeEnabled(): Text {
        return Text.literal("Right-click an Ohh Shiny block to remove it")
            .formatted(Formatting.YELLOW)
    }
    
    // Error messages (Red)
    fun alreadyClaimed(): Text {
        return Text.literal("You've already claimed this Ohh Shiny!")
            .formatted(Formatting.RED)
    }
    
    fun noLootAtLocation(): Text {
        return Text.literal("No Ohh Shiny at this location")
            .formatted(Formatting.RED)
    }
    
    fun emptyHandError(): Text {
        return Text.literal("You must hold an item in your main hand to create Ohh Shiny")
            .formatted(Formatting.RED)
    }
    
    fun noPermission(permission: String): Text {
        return Text.literal("You don't have permission: ${permission}")
            .formatted(Formatting.RED)
    }
    
    fun permissionServiceMissing(): Text {
        return Text.literal("Permission service unavailable; contact an administrator")
            .formatted(Formatting.RED)
    }
    
    // Informational messages
    fun lootListHeader(count: Int): Text {
        return Text.literal("Active Ohh Shiny entries (${count} total):")
            .formatted(Formatting.GOLD)
    }
    
    fun lootListEntry(position: BlockPos, dimension: RegistryKey<World>, itemName: String, claimedCount: Int, source: ServerCommandSource): Text {
        val coordText = "[${position.x}, ${position.y}, ${position.z}]"
        val hasTeleportPerm = LuckPermsUtil.hasPermission(source, LuckPermsUtil.Permissions.OHHSHINY_TELEPORT)
        
        val message = Text.literal("• ")
        
        if (hasTeleportPerm) {
            // Make coordinates clickable with teleport command
            val teleportY = position.y + 2
            val teleportCommand = "/execute in ${dimension.value} run tp @s ${position.x} ${teleportY} ${position.z}"
            
            val clickableCoords = Text.literal(coordText)
                .formatted(Formatting.AQUA, Formatting.UNDERLINE)
                .styled { style ->
                    style
                        .withClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, teleportCommand))
                        .withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Click to teleport").formatted(Formatting.YELLOW)))
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
        return Text.literal("No Ohh Shiny entries found")
            .formatted(Formatting.GRAY)
    }
    
    fun dataReloaded(count: Int): Text {
        return Text.literal("Ohh Shiny data reloaded (${count} entries)")
            .formatted(Formatting.GREEN)
    }
    
    fun playerClaimsReset(playerName: String, resetCount: Int): Text {
        return Text.literal("Reset ${resetCount} Ohh Shiny claims for player ${playerName}")
            .formatted(Formatting.GREEN)
    }
    
    fun allDataCleared(count: Int): Text {
        return Text.literal("Cleared all Ohh Shiny data (${count} entries removed)")
            .formatted(Formatting.GREEN)
    }
    
    fun confirmationRequired(command: String): Text {
        return Text.literal("This action cannot be undone. Run '${command}' again to confirm")
            .formatted(Formatting.YELLOW)
    }
    
    // Setup mode exit messages
    fun setupModeDisabled(): Text {
        return Text.literal("Setup mode disabled")
            .formatted(Formatting.GRAY)
    }
    
    fun removeModeDisabled(): Text {
        return Text.literal("Remove mode disabled")
            .formatted(Formatting.GRAY)
    }
    
    fun blockProtected(): Text {
        return Text.literal("This block is protected and cannot be broken!")
            .formatted(Formatting.RED)
    }
}
