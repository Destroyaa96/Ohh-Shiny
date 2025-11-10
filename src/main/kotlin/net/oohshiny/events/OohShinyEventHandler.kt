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
    private val logger = LoggerFactory.getLogger("OOHSHINY")
    
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
        
        // Spawn particles when players join to ensure they see them immediately
        ServerPlayConnectionEvents.JOIN.register { handler, _, _ ->
            val player = handler.player
            val server = player.server
            
            // Spawn particles for all nearby unclaimed rewards when player joins
            server.execute {
                try {
                    val allEntries = OOHSHINYManager.getAllLootEntries(server)
                    val playerPos = player.blockPos
                    val playerWorld = player.serverWorld
                    
                    for (entry in allEntries.values) {
                        try {
                            // Only spawn particles for rewards in the same dimension
                            if (entry.world != playerWorld) continue
                            
                            // Only spawn particles if player hasn't claimed this reward
                            if (entry.hasPlayerClaimed(player.uuid)) continue
                            
                            // Only spawn particles if player is within 16 blocks
                            val distance = playerPos.getSquaredDistance(entry.position)
                            if (distance <= 16.0 * 16.0) {
                                OOHSHINYParticles.spawnParticlesAt(entry)
                            }
                        } catch (e: Exception) {
                            // Silently skip rewards in unloaded chunks
                        }
                    }
                } catch (e: Exception) {
                    logger.error("Error spawning particles on player login", e)
                }
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
            // Double-check permissions for security
            if (!LuckPermsUtil.hasPermission(player.commandSource, LuckPermsUtil.Permissions.OOHSHINY_SET)) {
                player.sendMessage(OOHSHINYMessages.noPermission(LuckPermsUtil.Permissions.OOHSHINY_SET), false)
                return ActionResult.FAIL
            }
            
            val heldItem = player.mainHandStack
            val success = OOHSHINYManager.createLootEntry(player, dimension, position, heldItem)
            
            return if (success) ActionResult.SUCCESS else ActionResult.FAIL
        }
        
        // Priority 2: Admin in remove mode deletes a reward here
        if (OOHSHINYManager.isInRemoveMode(player)) {
            // Double-check permissions for security
            if (!LuckPermsUtil.hasPermission(player.commandSource, LuckPermsUtil.Permissions.OOHSHINY_REMOVE)) {
                player.sendMessage(OOHSHINYMessages.noPermission(LuckPermsUtil.Permissions.OOHSHINY_REMOVE), false)
                return ActionResult.FAIL
            }
            
            val success = OOHSHINYManager.removeLootEntry(player, dimension, position)
            
            return if (success) ActionResult.SUCCESS else ActionResult.FAIL
        }
        
        // Priority 3: Player tries to claim a reward here
        if (LuckPermsUtil.hasPermission(player.commandSource, LuckPermsUtil.Permissions.OOHSHINY_CLAIM)) {
            val success = OOHSHINYManager.claimLoot(player, dimension, position)
            
            // SUCCESS cancels the event and prevents normal block interactions (like opening chests)
            // PASS allows normal block interactions to continue if there was no reward here
            return if (success) ActionResult.SUCCESS else ActionResult.PASS
        }
        
        // No special handling needed - allow normal Minecraft behavior
        return ActionResult.PASS
    }
}
