package net.seto.ohhshiny.events

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.ActionResult
import net.seto.ohhshiny.OhhShinyManager
import net.seto.ohhshiny.util.OhhShinyMessages
import net.seto.ohhshiny.util.LuckPermsUtil
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import org.slf4j.LoggerFactory

/**
 * Handles all player interactions with blocks for Ohh Shiny functionality.
 * 
 * Intercepts right-click events to:
 * - Allow admins in setup mode to create rewards
 * - Allow admins in remove mode to delete rewards  
 * - Allow players to claim rewards at marked locations
 * 
 * Also protects Ohh Shiny blocks from being broken.
 */
object OhhShinyEventHandler {
    private val logger = LoggerFactory.getLogger("ohhshiny")
    
    /**
     * Registers all event listeners needed for Ohh Shiny functionality.
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
            OhhShinyManager.onPlayerDisconnect(handler.player.uuid)
        }
        
        // Protect Ohh Shiny blocks from being broken
        // Using BEFORE event to cancel the break before it happens
        PlayerBlockBreakEvents.BEFORE.register(PlayerBlockBreakEvents.Before { world, player, pos, state, blockEntity ->
            // Only check on server side with actual players
            if (!world.isClient && player is ServerPlayerEntity) {
                // Check if this block has an Ohh Shiny entry
                val entry = OhhShinyManager.getLootEntry(player.server, world.registryKey, pos)
                
                if (entry != null) {
                    // Block has Ohh Shiny data - prevent breaking and notify player
                    player.sendMessage(OhhShinyMessages.blockProtected(), false)
                    logger.info("Blocked player ${player.nameForScoreboard} from breaking Ohh Shiny at $pos")
                    return@Before true // Return true to CANCEL the break
                }
            }
            
            false // Return false to ALLOW the break
        })
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
        if (OhhShinyManager.isInSetupMode(player)) {
            // Double-check permissions for security
            if (!LuckPermsUtil.hasPermission(player.commandSource, LuckPermsUtil.Permissions.OHHSHINY_SET)) {
                player.sendMessage(OhhShinyMessages.noPermission(LuckPermsUtil.Permissions.OHHSHINY_SET), false)
                return ActionResult.FAIL
            }
            
            val heldItem = player.mainHandStack
            val success = OhhShinyManager.createLootEntry(player, dimension, position, heldItem)
            
            return if (success) ActionResult.SUCCESS else ActionResult.FAIL
        }
        
        // Priority 2: Admin in remove mode deletes a reward here
        if (OhhShinyManager.isInRemoveMode(player)) {
            // Double-check permissions for security
            if (!LuckPermsUtil.hasPermission(player.commandSource, LuckPermsUtil.Permissions.OHHSHINY_REMOVE)) {
                player.sendMessage(OhhShinyMessages.noPermission(LuckPermsUtil.Permissions.OHHSHINY_REMOVE), false)
                return ActionResult.FAIL
            }
            
            val success = OhhShinyManager.removeLootEntry(player, dimension, position)
            
            return if (success) ActionResult.SUCCESS else ActionResult.FAIL
        }
        
        // Priority 3: Player tries to claim a reward here
        if (LuckPermsUtil.hasPermission(player.commandSource, LuckPermsUtil.Permissions.OHHSHINY_CLAIM)) {
            val success = OhhShinyManager.claimLoot(player, dimension, position)
            
            // SUCCESS cancels the event and prevents normal block interactions (like opening chests)
            // PASS allows normal block interactions to continue if there was no reward here
            return if (success) ActionResult.SUCCESS else ActionResult.PASS
        }
        
        // No special handling needed - allow normal Minecraft behavior
        return ActionResult.PASS
    }
}
