package net.seto.ohhshiny.events

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.server.MinecraftServer
import net.seto.ohhshiny.OhhShinyManager
import net.seto.ohhshiny.util.OhhShinyParticles

/**
 * Handles periodic particle effects for active reward locations.
 * 
 * Spawns colorful particles above rewards to make them visible to players,
 * but only when an unclaimed player is nearby to reduce server load.
 */
object OhhShinyTickHandler {
    
    private var tickCounter = 0
    private const val PARTICLE_SPAWN_INTERVAL = 20 // Check every 20 ticks (1 second) for performance
    
    /**
     * Registers this handler to run on every server tick.
     */
    fun register() {
        ServerTickEvents.END_SERVER_TICK.register(::onServerTick)
    }
    
    /**
     * Called every server tick (20 times per second).
     */
    private fun onServerTick(server: MinecraftServer) {
        tickCounter++
        
        // Only check for particle spawning once per second to reduce overhead
        if (tickCounter >= PARTICLE_SPAWN_INTERVAL) {
            tickCounter = 0
            spawnParticlesAtLootLocations(server)
        }
    }
    
    /**
     * Spawns particles at reward locations, but only if:
     * - A player who hasn't claimed the reward is within 16 blocks
     * - The player is in the same dimension as the reward
     * 
     * This optimization prevents unnecessary particle spawning in empty areas.
     */
    private fun spawnParticlesAtLootLocations(server: MinecraftServer) {
        val allEntries = OhhShinyManager.getAllLootEntries(server)
        val onlinePlayers = server.playerManager.playerList
        
        for (entry in allEntries.values) {
            try {
                val lootPos = entry.position
                val lootWorld = entry.world
                
                // Only spawn particles if there's a nearby player who can still claim this reward
                val hasNearbyUnclaimedPlayer = onlinePlayers.any { player ->
                    // Must be in same dimension
                    if (player.serverWorld != lootWorld) {
                        return@any false
                    }
                    
                    // Must not have already claimed this reward
                    if (entry.hasPlayerClaimed(player.uuid)) {
                        return@any false
                    }
                    
                    // Must be within 16 blocks (squared distance for performance)
                    val playerPos = player.blockPos
                    val distance = playerPos.getSquaredDistance(lootPos)
                    distance <= 16.0 * 16.0
                }
                
                if (hasNearbyUnclaimedPlayer) {
                    OhhShinyParticles.spawnParticlesAt(entry)
                }
            } catch (e: Exception) {
                // Silently skip rewards in unloaded worlds/dimensions
            }
        }
    }
}
