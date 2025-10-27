package net.seto.ohhshiny.events

import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.ActionResult
import net.seto.ohhshiny.OhhShinyManager
import net.seto.ohhshiny.util.OhhShinyMessages
import net.seto.ohhshiny.util.LuckPermsUtil
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents

/**
 * Event handler for Ohh Shiny interactions
 * Handles right-click events for setup, removal, and claiming
 */
object OhhShinyEventHandler {
    
    /**
     * Registers all Ohh Shiny event handlers
     */
    fun register() {
        // Register UseBlock event for right-click handling
        UseBlockCallback.EVENT.register { player, world, hand, hitResult ->
            // Only process main hand interactions and server-side only
            if (hand.name == "MAIN_HAND" && !world.isClient && player is ServerPlayerEntity) {
                handleBlockUse(player, hitResult)
            } else {
                ActionResult.PASS
            }
        }
        
        // Register player disconnect event to clean up mode tracking
        ServerPlayConnectionEvents.DISCONNECT.register { handler, _ ->
            OhhShinyManager.onPlayerDisconnect(handler.player.uuid)
        }
    }
    
    /**
     * Handles block use (right-click) events for Ohh Shiny functionality
     */
    private fun handleBlockUse(
        player: ServerPlayerEntity, 
        hitResult: net.minecraft.util.hit.BlockHitResult
    ): ActionResult {
        val world = player.serverWorld
        val position = hitResult.blockPos
        val dimension = world.registryKey
        
        // Check if player is in setup mode
        if (OhhShinyManager.isInSetupMode(player)) {
            // Verify permission (double-check)
            if (!LuckPermsUtil.hasPermission(player.commandSource, LuckPermsUtil.Permissions.OHHSHINY_SET)) {
                player.sendMessage(OhhShinyMessages.noPermission(LuckPermsUtil.Permissions.OHHSHINY_SET), false)
                return ActionResult.FAIL
            }
            
            val heldItem = player.mainHandStack
            val success = OhhShinyManager.createLootEntry(player, dimension, position, heldItem)
            
            return if (success) ActionResult.SUCCESS else ActionResult.FAIL
        }
        
        // Check if player is in remove mode
        if (OhhShinyManager.isInRemoveMode(player)) {
            // Verify permission (double-check)
            if (!LuckPermsUtil.hasPermission(player.commandSource, LuckPermsUtil.Permissions.OHHSHINY_REMOVE)) {
                player.sendMessage(OhhShinyMessages.noPermission(LuckPermsUtil.Permissions.OHHSHINY_REMOVE), false)
                return ActionResult.FAIL
            }
            
            val success = OhhShinyManager.removeLootEntry(player, dimension, position)
            
            return if (success) ActionResult.SUCCESS else ActionResult.FAIL
        }
        
        // Check if this location has Ohh Shiny and player can claim it
        if (LuckPermsUtil.hasPermission(player.commandSource, LuckPermsUtil.Permissions.OHHSHINY_CLAIM)) {
            val success = OhhShinyManager.claimLoot(player, dimension, position)
            
            // If we successfully claimed loot, return SUCCESS to prevent other block interactions
            // If there was no loot here, return PASS to allow normal block interactions
            return if (success) ActionResult.SUCCESS else ActionResult.PASS
        }
        
        // Not in any mode and no claim permission, allow normal block interactions
        return ActionResult.PASS
    }
}
