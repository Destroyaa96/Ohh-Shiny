package net.seto.ohhshiny.events

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.server.MinecraftServer
import net.seto.ohhshiny.OhhShinyManager
import net.seto.ohhshiny.util.OhhShinyParticles

/**
 * Handles periodic tick events for particle spawning
 */
object OhhShinyTickHandler {
    
    private var tickCounter = 0
    private const val PARTICLE_SPAWN_INTERVAL = 20 // Spawn particles every 20 ticks (1 second)
    
    /**
     * Registers the tick handler
     */
    fun register() {
        ServerTickEvents.END_SERVER_TICK.register(::onServerTick)
    }
    
    /**
     * Called every server tick
     */
    private fun onServerTick(server: MinecraftServer) {
        tickCounter++
        
        // Only spawn particles periodically
        if (tickCounter >= PARTICLE_SPAWN_INTERVAL) {
            tickCounter = 0
            spawnParticlesAtLootLocations(server)
        }
    }
    
    /**
     * Spawns particles at all active Ohh Shiny locations
     * Particles are only shown if a player who hasn't claimed is within 16 blocks
     */
    private fun spawnParticlesAtLootLocations(server: MinecraftServer) {
        val allEntries = OhhShinyManager.getAllLootEntries(server)
        
        // Get all online players
        val onlinePlayers = server.playerManager.playerList
        
        for (entry in allEntries.values) {
            try {
                val lootPos = entry.position
                val lootWorld = entry.world
                
                // Check if any online players who haven't claimed this loot are within 16 blocks
                val hasNearbyUnclaimedPlayer = onlinePlayers.any { player ->
                    // Check if player is in the same dimension
                    if (player.serverWorld != lootWorld) {
                        return@any false
                    }
                    
                    // Check if player hasn't claimed this loot
                    if (entry.hasPlayerClaimed(player.uuid)) {
                        return@any false
                    }
                    
                    // Check if player is within 16 blocks
                    val playerPos = player.blockPos
                    val distance = playerPos.getSquaredDistance(lootPos)
                    distance <= 16.0 * 16.0 // 16 blocks squared
                }
                
                if (hasNearbyUnclaimedPlayer) {
                    OhhShinyParticles.spawnParticlesAt(entry)
                }
            } catch (e: Exception) {
                // Silently skip if the world isn't loaded
            }
        }
    }
}
