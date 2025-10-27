package net.seto.ohhshiny

import net.minecraft.item.ItemStack
import net.minecraft.registry.RegistryKey
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.seto.ohhshiny.data.OhhShinyEntry
import net.seto.ohhshiny.data.OhhShinyState
import net.seto.ohhshiny.util.OhhShinyMessages
import net.seto.ohhshiny.util.OhhShinyParticles
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Central manager for Ohh Shiny operations
 * Handles loot creation, removal, claiming, and setup mode tracking
 */
object OhhShinyManager {
    private val logger = LoggerFactory.getLogger("ohhshiny")
    
    // Track players in setup modes
    private val playersInSetupMode: MutableSet<UUID> = ConcurrentHashMap.newKeySet()
    private val playersInRemoveMode: MutableSet<UUID> = ConcurrentHashMap.newKeySet()
    
    /**
     * Gets the Ohh Shiny state (now a singleton object)
     */
    private fun getState(server: MinecraftServer): OhhShinyState {
        return OhhShinyState
    }
    
    /**
     * Enables setup mode for a player
     */
    fun enableSetupMode(player: ServerPlayerEntity) {
        playersInSetupMode.add(player.uuid)
        playersInRemoveMode.remove(player.uuid) // Disable remove mode if active
        player.sendMessage(OhhShinyMessages.setupModeEnabled(), false)
        
        logger.info("Player ${player.nameForScoreboard} entered Ohh Shiny setup mode")
    }
    
    /**
     * Enables remove mode for a player
     */
    fun enableRemoveMode(player: ServerPlayerEntity) {
        playersInRemoveMode.add(player.uuid)
        playersInSetupMode.remove(player.uuid) // Disable setup mode if active
        player.sendMessage(OhhShinyMessages.removeModeEnabled(), false)
        
        logger.info("Player ${player.nameForScoreboard} entered Ohh Shiny remove mode")
    }
    
    /**
     * Disables any active mode for a player
     */
    fun disableMode(player: ServerPlayerEntity) {
        val wasInSetup = playersInSetupMode.remove(player.uuid)
        val wasInRemove = playersInRemoveMode.remove(player.uuid)
        
        if (wasInSetup) {
            player.sendMessage(OhhShinyMessages.setupModeDisabled(), false)
        } else if (wasInRemove) {
            player.sendMessage(OhhShinyMessages.removeModeDisabled(), false)
        }
    }
    
    /**
     * Checks if a player is in setup mode
     */
    fun isInSetupMode(player: ServerPlayerEntity): Boolean {
        return playersInSetupMode.contains(player.uuid)
    }
    
    /**
     * Checks if a player is in remove mode
     */
    fun isInRemoveMode(player: ServerPlayerEntity): Boolean {
        return playersInRemoveMode.contains(player.uuid)
    }
    
    /**
     * Creates a new Ohh Shiny entry at the specified location
     * This is called when a player in setup mode right-clicks a block
     */
    fun createLootEntry(
        player: ServerPlayerEntity, 
        dimension: RegistryKey<World>, 
        position: BlockPos, 
        heldItem: ItemStack
    ): Boolean {
        if (heldItem.isEmpty) {
            player.sendMessage(OhhShinyMessages.emptyHandError(), false)
            return false
        }
        
        val server = player.server
        val state = getState(server)
        
        // Get the ServerWorld from the dimension key
        val serverWorld = server.getWorld(dimension) ?: run {
            player.sendMessage(OhhShinyMessages.emptyHandError(), false) // Reuse error message
            return false
        }
        
        // Create a copy of the held item to store
        val rewardItem = heldItem.copy()
        
        // Create the loot entry
        val entry = OhhShinyEntry(serverWorld, position, rewardItem)
        
        // Add to state
        state.addLootEntry(entry)
        
        // Spawn creation particle effect
        OhhShinyParticles.spawnCreateEffect(serverWorld, position)
        
        // Send success message
        val itemName = rewardItem.name.string
        player.sendMessage(OhhShinyMessages.lootCreated(position, dimension, itemName), false)
        
        // Disable setup mode
        playersInSetupMode.remove(player.uuid)
        player.sendMessage(OhhShinyMessages.setupModeDisabled(), false)
        
        // Log the action
        logger.info(
            "Player ${player.nameForScoreboard} created Ohh Shiny at ${dimension.value} [${position.x}, ${position.y}, ${position.z}] with item: ${itemName}"
        )
        
        return true
    }
    
    /**
     * Removes an Ohh Shiny entry at the specified location
     * This is called when a player in remove mode right-clicks an Ohh Shiny block
     */
    fun removeLootEntry(
        player: ServerPlayerEntity, 
        dimension: RegistryKey<World>, 
        position: BlockPos
    ): Boolean {
        val server = player.server
        val state = getState(server)
        
        val removedEntry = state.removeLootEntry(dimension, position)
        
        if (removedEntry != null) {
            // Spawn removal particle effect
            OhhShinyParticles.spawnRemoveEffect(removedEntry.world, position)
            
            player.sendMessage(OhhShinyMessages.lootRemoved(position, dimension), false)
            
            // Disable remove mode
            playersInRemoveMode.remove(player.uuid)
            player.sendMessage(OhhShinyMessages.removeModeDisabled(), false)
            
            // Log the action
            logger.info(
                "Player ${player.nameForScoreboard} removed Ohh Shiny at ${dimension.value} [${position.x}, ${position.y}, ${position.z}]"
            )
            
            return true
        } else {
            player.sendMessage(OhhShinyMessages.noLootAtLocation(), false)
            return false
        }
    }
    
    /**
     * Attempts to claim Ohh Shiny at the specified location
     * This is called when a player right-clicks a block (not in setup/remove mode)
     */
    fun claimLoot(
        player: ServerPlayerEntity, 
        dimension: RegistryKey<World>, 
        position: BlockPos
    ): Boolean {
        val server = player.server
        val state = getState(server)
        
        val entry = state.getLootEntry(dimension, position) ?: return false
        
        // Check if player has already claimed
        if (entry.hasPlayerClaimed(player.uuid)) {
            player.sendMessage(OhhShinyMessages.alreadyClaimed(), false)
            return false
        }
        
        // Give the reward item to the player
        val rewardCopy = entry.rewardItem.copy()
        if (!player.giveItemStack(rewardCopy)) {
            // If inventory is full, drop the item
            player.dropItem(rewardCopy, false)
        }
        
        // Mark as claimed
        entry.claimForPlayer(player.uuid)
        
        // Spawn claim particle effect
        OhhShinyParticles.spawnClaimEffect(entry.world, position)
        
        // Save the claim to disk
        state.markDirty()
        
        // Send success message
        player.sendMessage(OhhShinyMessages.lootClaimed(entry.rewardItem), false)
        
        // Log the action
        val itemName = entry.rewardItem.name.string
        logger.info(
            "Player ${player.nameForScoreboard} claimed Ohh Shiny at ${dimension.value} [${position.x}, ${position.y}, ${position.z}]: ${itemName}"
        )
        
        return true
    }
    
    /**
     * Gets all loot entries for listing
     */
    fun getAllLootEntries(server: MinecraftServer): Map<String, OhhShinyEntry> {
        return getState(server).getAllLootEntries()
    }
    
    /**
     * Gets the count of loot entries
     */
    fun getLootCount(server: MinecraftServer): Int {
        return getState(server).getEntryCount()
    }
    
    /**
     * Reloads Ohh Shiny data from disk
     */
    fun reloadData(server: MinecraftServer): Int {
        val state = getState(server)
        state.reload(server)
        val count = state.getEntryCount()
        
        logger.info("Ohh Shiny data reloaded from disk: ${count} entries")
        return count
    }
    
    /**
     * Resets all claims for a specific player
     */
    fun resetPlayerClaims(server: MinecraftServer, targetPlayerUuid: UUID): Int {
        val state = getState(server)
        var resetCount = 0
        
        state.getAllLootEntries().values.forEach { entry ->
            if (entry.resetPlayerClaim(targetPlayerUuid)) {
                resetCount++
            }
        }
        
        if (resetCount > 0) {
            // Mark state as dirty to save changes
            state.markDirty()
        }
        
        logger.info("Reset ${resetCount} Ohh Shiny claims for player UUID: ${targetPlayerUuid}")
        return resetCount
    }
    
    /**
     * Clears all Ohh Shiny data
     */
    fun clearAllData(server: MinecraftServer): Int {
        val state = getState(server)
        val count = state.getEntryCount()
        
        state.clearAllEntries()
        
        logger.info("Cleared all Ohh Shiny data: ${count} entries removed")
        return count
    }
    
    /**
     * Called when a player disconnects to clean up mode tracking
     */
    fun onPlayerDisconnect(playerUuid: UUID) {
        playersInSetupMode.remove(playerUuid)
        playersInRemoveMode.remove(playerUuid)
    }
}
