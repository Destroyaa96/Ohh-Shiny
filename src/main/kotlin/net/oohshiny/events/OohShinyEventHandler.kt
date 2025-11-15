package net.OOHSHINY.events

import net.fabricmc.fabric.api.event.player.AttackBlockCallback
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.ActionResult
import net.OOHSHINY.OOHSHINYManager
import net.OOHSHINY.util.OOHSHINYMessages
import net.OOHSHINY.util.LuckPermsUtil
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import org.slf4j.LoggerFactory
import net.OOHSHINY.util.OOHSHINYParticles

/**
 * Handles all player interactions with blocks for Ooh Shiny functionality.
 * 
 * Intercepts right-click events to:
 * - Allow admins in setup mode to create rewards
 * - Allow admins in remove mode to delete rewards  
 * - Allow players to claim rewards at marked locations
 * 
 * Also protects Ooh Shiny blocks from being broken.
 */
object OOHSHINYEventHandler {
    private val logger = LoggerFactory.getLogger("oohshiny")
    
    /**
     * Registers all event listeners needed for Ooh Shiny functionality.
     */
    fun register() {
        // Listen for block right-clicks to handle reward creation and claiming
        UseBlockCallback.EVENT.register { player, world, hand, hitResult ->
            // Only process main hand interactions, and only on the server (not client)
            if (hand.name == "MAIN_HAND" && !world.isClient && player is ServerPlayerEntity) {
                handleBlockUse(player, hitResult)
            } else {
                ActionResult.PASS
            }
        }
        
        // Clean up mode tracking when players disconnect
        ServerPlayConnectionEvents.DISCONNECT.register { handler, _ ->
            OOHSHINYManager.onPlayerDisconnect(handler.player.uuid)
        }
        
        // Protect Ooh Shiny blocks from being broken
        // Using AttackBlockCallback which fires when player starts breaking (left-click)
        // This prevents the break from even starting, avoiding client desync
        AttackBlockCallback.EVENT.register { player, world, hand, pos, direction ->
            // Only check on server side with actual players
            if (!world.isClient && player is ServerPlayerEntity) {
                // Check if this block has an Ooh Shiny entry
                val entry = OOHSHINYManager.getLootEntry(player.server, world.registryKey, pos)
                
                if (entry != null) {
                    // Block has Ooh Shiny data - prevent breaking and notify player
                    player.sendMessage(OOHSHINYMessages.blockProtected(player), false)
                    logger.info("Blocked player ${player.nameForScoreboard} from attacking Ooh Shiny at $pos")
                    return@register ActionResult.FAIL // FAIL cancels the attack
                }
            }
            
            ActionResult.PASS // PASS allows normal block interaction
        }
    }
    
    /**
     * Processes block right-click events based on player mode and permissions.
     * 
     * Priority order:
     * 1. Setup mode (admin creating rewards)
     * 2. Remove mode (admin deleting rewards)
     * 3. Claim mode (player claiming rewards)
     */
    private fun handleBlockUse(
        player: ServerPlayerEntity, 
        hitResult: net.minecraft.util.hit.BlockHitResult
    ): ActionResult {
        val world = player.serverWorld
        val position = hitResult.blockPos
        val dimension = world.registryKey
        
        // Priority 1: Admin in setup mode creates a reward here
        if (OOHSHINYManager.isInSetupMode(player)) {
            val heldItem = player.mainHandStack
            val success = OOHSHINYManager.createLootEntry(player, dimension, position, heldItem)
            return if (success) ActionResult.SUCCESS else ActionResult.FAIL
        }
        
        // Priority 2: Admin in remove mode deletes a reward here
        if (OOHSHINYManager.isInRemoveMode(player)) {
            val success = OOHSHINYManager.removeLootEntry(player, dimension, position)
            return if (success) ActionResult.SUCCESS else ActionResult.FAIL
        }
        
        // Priority 3: Player tries to claim a reward here
        // First check if there's a loot entry at this location
        val entry = OOHSHINYManager.getLootEntry(player.server, dimension, position)
        
        if (entry != null) {
            // Check if player has permission to claim this category
            if (LuckPermsUtil.hasClaimPermission(player, entry.category)) {
                val success = OOHSHINYManager.claimLoot(player, dimension, position)
                
                // SUCCESS cancels the event and prevents normal block interactions (like opening chests)
                return if (success) ActionResult.SUCCESS else ActionResult.FAIL
            } else {
                // Player doesn't have permission for this category
                player.sendMessage(OOHSHINYMessages.noClaimPermission(player, entry.category), false)
                return ActionResult.FAIL
            }
        }
        
        // No special handling needed - allow normal Minecraft behavior
        return ActionResult.PASS
    }
}
