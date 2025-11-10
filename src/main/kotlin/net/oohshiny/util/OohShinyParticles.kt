package net.OOHSHINY.util

import net.minecraft.particle.DustParticleEffect
import net.minecraft.particle.ParticleTypes
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.OOHSHINY.data.OOHSHINYEntry
import org.joml.Vector3f
import kotlin.math.cos
import kotlin.math.sin

/**
 * Handles particle effects for Ooh Shiny locations
 */
object OOHSHINYParticles {
    
    /**
     * Spawns particles at an Ooh Shiny location
     * Should be called periodically (e.g., via a tick event)
     * @param entry The Ooh Shiny entry
     * @param playerUuid Optional player UUID to check if they've already claimed (null = show for everyone)
     */
    fun spawnParticlesAt(entry: OOHSHINYEntry, playerUuid: java.util.UUID? = null) {
        // Don't spawn particles if the specific player has already claimed this loot
        if (playerUuid != null && entry.hasPlayerClaimed(playerUuid)) {
            return
        }
        
        val world = entry.world
        val pos = entry.position
        
        // Spawn particles at the center of the block
        val x = pos.x + 0.5
        val y = pos.y + 0.5
        val z = pos.z + 0.5
        
        // Calculate rainbow color based on time (7 second cycle = 7000ms)
        val timeMs = System.currentTimeMillis()
        val cycleProgress = (timeMs % 7000) / 7000.0 // 0.0 to 1.0 over 7 seconds
        val hue = cycleProgress * 360.0 // 0 to 360 degrees
        val rgb = hsvToRgb(hue, 1.0, 1.0)
        
        // Create dust particle effect with current rainbow color
        val dustEffect = DustParticleEffect(Vector3f(rgb[0], rgb[1], rgb[2]), 1.0f)
        
        // Spawn particles from the center with velocity in all directions
        world.spawnParticles(
            dustEffect,
            x,
            y,
            z,
            15, // count - increased particle count
            0.3, // deltaX - spread in X direction
            0.3, // deltaY - spread in Y direction
            0.3, // deltaZ - spread in Z direction
            0.05 // speed
        )
    }
    
    /**
     * Spawns a burst of particles when loot is claimed
     */
    fun spawnClaimEffect(world: ServerWorld, pos: BlockPos) {
        val x = pos.x + 0.5
        val y = pos.y + 0.5
        val z = pos.z + 0.5
        
        // Spawn a burst of happy villager particles
        world.spawnParticles(
            ParticleTypes.HAPPY_VILLAGER,
            x,
            y,
            z,
            15, // count
            0.5, // deltaX
            0.5, // deltaY
            0.5, // deltaZ
            0.1  // speed
        )
    }
    
    /**
     * Spawns particles when an Ooh Shiny location is created
     */
    fun spawnCreateEffect(world: ServerWorld, pos: BlockPos) {
        val x = pos.x + 0.5
        val y = pos.y + 0.5
        val z = pos.z + 0.5
        
        // Spawn a burst of portal particles
        world.spawnParticles(
            ParticleTypes.PORTAL,
            x,
            y,
            z,
            20, // count
            0.5, // deltaX
            0.5, // deltaY
            0.5, // deltaZ
            0.5  // speed
        )
    }
    
    /**
     * Spawns particles when an Ooh Shiny location is removed
     */
    fun spawnRemoveEffect(world: ServerWorld, pos: BlockPos) {
        val x = pos.x + 0.5
        val y = pos.y + 0.5
        val z = pos.z + 0.5
        
        // Spawn a burst of smoke particles
        world.spawnParticles(
            ParticleTypes.LARGE_SMOKE,
            x,
            y,
            z,
            10, // count
            0.3, // deltaX
            0.3, // deltaY
            0.3, // deltaZ
            0.05 // speed
        )
    }
    
    /**
     * Converts HSV color to RGB
     * @param h Hue (0-360)
     * @param s Saturation (0-1)
     * @param v Value (0-1)
     * @return FloatArray of [r, g, b] where each component is 0-1
     */
    private fun hsvToRgb(h: Double, s: Double, v: Double): FloatArray {
        val c = v * s
        val x = c * (1 - kotlin.math.abs((h / 60.0) % 2 - 1))
        val m = v - c
        
        val (r, g, b) = when {
            h < 60 -> Triple(c, x, 0.0)
            h < 120 -> Triple(x, c, 0.0)
            h < 180 -> Triple(0.0, c, x)
            h < 240 -> Triple(0.0, x, c)
            h < 300 -> Triple(x, 0.0, c)
            else -> Triple(c, 0.0, x)
        }
        
        return floatArrayOf(
            ((r + m) * 255).toFloat() / 255f,
            ((g + m) * 255).toFloat() / 255f,
            ((b + m) * 255).toFloat() / 255f
        )
    }
}
