package net.ohhshiny

import net.minecraft.item.ItemStack
import net.minecraft.registry.RegistryKey
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.ohhshiny.data.OhhShinyEntry
import net.ohhshiny.data.OhhShinyState
import net.ohhshiny.util.OhhShinyMessages
import net.ohhshiny.util.OhhShinyParticles
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Central manager for all Ohh Shiny operations.
 * 
 * Manages:
 * - Creating and removing reward locations (admin operations)
 * - Player claiming mechanics
 * - Setup and remove mode tracking for admins
 * - Data persistence and reloading
 */
object OhhShinyManager {
    private val logger = LoggerFactory.getLogger("ohhshiny")
    
    // Track which players are currently in setup or remove mode (for admin operations)
    private val playersInSetupMode: MutableSet<UUID> = ConcurrentHashMap.newKeySet()
    private val playersInRemoveMode: MutableSet<UUID> = ConcurrentHashMap.newKeySet()
    
    /**
     * Gets the singleton state object that manages all reward data.
     */
    private fun getState(server: MinecraftServer): OhhShinyState {
        return OhhShinyState
    }
    
    /**
     * Enables setup mode for an admin player.
     * While in setup mode, the next block they right-click will become a reward location.
     */
    fun enableSetupMode(player: ServerPlayerEntity) {
        playersInSetupMode.add(player.uuid)
        playersInRemoveMode.remove(player.uuid) // Ensure player is not in both modes at once
        player.sendMessage(OhhShinyMessages.setupModeEnabled(), false)
        
        logger.info("Player ${player.nameForScoreboard} entered Ohh Shiny setup mode")
    }
    
    /**
     * Enables remove mode for an admin player.
     * While in remove mode, the next reward location they right-click will be deleted.
     */
    fun enableRemoveMode(player: ServerPlayerEntity) {
        playersInRemoveMode.add(player.uuid)
        playersInSetupMode.remove(player.uuid) // Ensure player is not in both modes at once
        player.sendMessage(OhhShinyMessages.removeModeEnabled(), false)
        
        logger.info("Player ${player.nameForScoreboard} entered Ohh Shiny remove mode")
    }
    
    /**
     * Disables any active admin mode (setup or remove) for a player.
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
     * Checks if a player is currently in setup mode.
     */
    fun isInSetupMode(player: ServerPlayerEntity): Boolean {
        return playersInSetupMode.contains(player.uuid)
    }
    
    /**
     * Checks if a player is currently in remove mode.
     */
    fun isInRemoveMode(player: ServerPlayerEntity): Boolean {
        return playersInRemoveMode.contains(player.uuid)
    }
    
    /**
     * Creates a new Ohh Shiny reward at the specified location.
     * 
     * Called when an admin in setup mode right-clicks a block. The item in their main hand
     * becomes the reward that players will receive when they claim this location.
     * 
     * @return true if the reward was successfully created, false otherwise
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
        
        // Retrieve the world instance for this dimension
        val serverWorld = server.getWorld(dimension) ?: run {
            player.sendMessage(OhhShinyMessages.emptyHandError(), false)
            return false
        }
        
        // Make a copy of the held item to store as the reward
        val rewardItem = heldItem.copy()
        
        // Create the reward entry and add it to persistent storage
        val entry = OhhShinyEntry(serverWorld, position, rewardItem)
        state.addLootEntry(entry)
        
        // Show visual feedback at the location
        OhhShinyParticles.spawnCreateEffect(serverWorld, position)
        
        // Notify the admin of successful creation
        player.sendMessage(OhhShinyMessages.lootCreated(position, dimension, rewardItem), false)
        
        // Automatically exit setup mode after placing one reward
        playersInSetupMode.remove(player.uuid)
        player.sendMessage(OhhShinyMessages.setupModeDisabled(), false)
        
        // Log the creation for server admins
        logger.info(
            "Player ${player.nameForScoreboard} created Ohh Shiny at ${dimension.value} [${position.x}, ${position.y}, ${position.z}] with item: ${rewardItem.name.string}"
        )
        
        return true
    }
    
    /**
     * Removes an Ohh Shiny reward at the specified location.
     * 
     * Called when an admin in remove mode right-clicks a block. If a reward exists at that
     * location, it will be permanently deleted.
     * 
     * @return true if a reward was found and removed, false otherwise
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
            // Show visual feedback for the removal
            OhhShinyParticles.spawnRemoveEffect(removedEntry.world, position)
            
            player.sendMessage(OhhShinyMessages.lootRemoved(position, dimension), false)
            
            // Automatically exit remove mode after removing one reward
            playersInRemoveMode.remove(player.uuid)
            player.sendMessage(OhhShinyMessages.removeModeDisabled(), false)
            
            // Log the removal for server admins
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
     * Attempts to claim an Ohh Shiny reward at the specified location.
     * 
     * Called when a player with claim permissions right-clicks a block. If a reward exists
     * at that location and they haven't claimed it before, they receive the reward item.
     * 
     * @return true if the reward was successfully claimed, false otherwise
     */
    fun claimLoot(
        player: ServerPlayerEntity, 
        dimension: RegistryKey<World>, 
        position: BlockPos
    ): Boolean {
        val server = player.server
        val state = getState(server)
        
        val entry = state.getLootEntry(dimension, position) ?: return false
        
        // Prevent players from claiming the same reward twice
        if (entry.hasPlayerClaimed(player.uuid)) {
            player.sendMessage(OhhShinyMessages.alreadyClaimed(player), false)
            return false
        }
        
        // Give the reward item to the player (or drop it if inventory is full)
        val rewardCopy = entry.rewardItem.copy()
        if (!player.giveItemStack(rewardCopy)) {
            player.dropItem(rewardCopy, false)
        }
        
        // Record that this player has claimed this reward
        entry.claimForPlayer(player.uuid)
        
        // Show visual feedback for the successful claim
        OhhShinyParticles.spawnClaimEffect(entry.world, position)
        
        // Save the updated claim data to disk immediately
        state.markDirty()
        
        // Notify the player of their reward
        player.sendMessage(OhhShinyMessages.lootClaimed(entry.rewardItem, player), false)
        
        // Log the claim for server records
        val itemName = entry.rewardItem.name.string
        logger.info(
            "Player ${player.nameForScoreboard} claimed Ohh Shiny at ${dimension.value} [${position.x}, ${position.y}, ${position.z}]: ${itemName}"
        )
        
        return true
    }
    
    /**
     * Retrieves a specific loot entry at the given location.
     * Used for block protection checks.
     */
    fun getLootEntry(server: MinecraftServer, dimension: RegistryKey<World>, position: BlockPos): OhhShinyEntry? {
        return getState(server).getLootEntry(dimension, position)
    }
    
    /**
     * Retrieves all reward entries for display in the list command.
     */
    fun getAllLootEntries(server: MinecraftServer): Map<String, OhhShinyEntry> {
        return getState(server).getAllLootEntries()
    }
    
    /**
     * Returns the total number of active reward locations.
     */
    fun getLootCount(server: MinecraftServer): Int {
        return getState(server).getEntryCount()
    }
    
    /**
     * Reloads all reward data from the configuration file on disk.
     * Useful for applying manual changes or recovering from errors.
     */
    fun reloadData(server: MinecraftServer): Int {
        val state = getState(server)
        state.reload(server)
        val count = state.getEntryCount()
        
        logger.info("Ohh Shiny data reloaded from disk: ${count} entries")
        return count
    }
    
    /**
     * Resets all claim records for a specific player.
     * This allows them to claim all rewards again as if they were new.
     * 
     * @return The number of rewards that were reset for this player
     */
    fun resetPlayerClaims(server: MinecraftServer, targetPlayerUuid: UUID): Int {
        val state = getState(server)
        var resetCount = 0
        
        // Remove this player from all claim records
        state.getAllLootEntries().values.forEach { entry ->
            if (entry.resetPlayerClaim(targetPlayerUuid)) {
                resetCount++
            }
        }
        
        if (resetCount > 0) {
            // Persist the changes to disk
            state.markDirty()
        }
        
        logger.info("Reset ${resetCount} Ohh Shiny claims for player UUID: ${targetPlayerUuid}")
        return resetCount
    }
    
    /**
     * Permanently deletes all reward locations and claim data.
     * This action cannot be undone.
     * 
     * @return The number of rewards that were deleted
     */
    fun clearAllData(server: MinecraftServer): Int {
        val state = getState(server)
        val count = state.getEntryCount()
        
        state.clearAllEntries()
        
        logger.info("Cleared all Ohh Shiny data: ${count} entries removed")
        return count
    }
    
    /**
     * Cleans up mode tracking when a player disconnects.
     * Ensures they're not stuck in setup/remove mode if they log out while in it.
     */
    fun onPlayerDisconnect(playerUuid: UUID) {
        playersInSetupMode.remove(playerUuid)
        playersInRemoveMode.remove(playerUuid)
    }
}
