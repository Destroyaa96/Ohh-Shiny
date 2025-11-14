package net.OOHSHINY.util

import net.luckperms.api.LuckPerms
import net.luckperms.api.LuckPermsProvider
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import org.slf4j.LoggerFactory

object LuckPermsUtil {
    private val logger = LoggerFactory.getLogger("oohshiny")
    private var luckPerms: LuckPerms? = null
    private var initialized = false
    
    private fun initializeLuckPerms() {
        if (initialized) return
        initialized = true
        
        try {
            // Check if LuckPerms class exists first
            Class.forName("net.luckperms.api.LuckPermsProvider")
            luckPerms = LuckPermsProvider.get()
            logger.info("LuckPerms integration enabled")
        } catch (e: ClassNotFoundException) {
            logger.info("LuckPerms not found, using vanilla permissions")
            luckPerms = null
        } catch (e: Exception) {
            logger.warn("LuckPerms found but failed to initialize, falling back to vanilla permissions: ${e.message}")
            luckPerms = null
        }
    }
    
    fun hasPermission(source: ServerCommandSource, permission: String): Boolean {
        // Initialize LuckPerms if not already done
        initializeLuckPerms()
        
        // If not a player, check if console/command block (vanilla behavior)
        if (source.entity !is ServerPlayerEntity) {
            return source.hasPermissionLevel(2)
        }
        
        val player = source.entity as ServerPlayerEntity
        
        // Try LuckPerms first
        luckPerms?.let { lp ->
            try {
                val user = lp.userManager.getUser(player.uuid)
                if (user != null) {
                    val result = user.cachedData.permissionData.checkPermission(permission)
                    return result.asBoolean()
                }
            } catch (e: Exception) {
                logger.warn("Error checking LuckPerms permission '$permission' for player ${player.nameForScoreboard}", e)
            }
        }
        
        // Fallback to vanilla permissions
        return source.hasPermissionLevel(2)
    }
    
    fun isLuckPermsAvailable(): Boolean {
        initializeLuckPerms()
        return luckPerms != null
    }
    
    // Permission constants for Ooh Shiny
    object Permissions {
        const val BASE = "oohshiny.admin"
        
        // Ooh Shiny permissions
        const val OOHSHINY_BASE = "oohshiny"
        const val OOHSHINY_SET = "oohshiny.command.set"
        const val OOHSHINY_REMOVE = "oohshiny.command.remove"
        const val OOHSHINY_LIST = "oohshiny.command.list"
        const val OOHSHINY_RELOAD = "oohshiny.command.reload"
        const val OOHSHINY_RESET = "oohshiny.command.reset"
        const val OOHSHINY_CLEARALL = "oohshiny.command.clearall"
        const val OOHSHINY_CLAIM = "oohshiny.claim"
        const val OOHSHINY_TELEPORT = "oohshiny.command.teleport"
        const val OOHSHINY_GIVE = "oohshiny.command.give"
    }
}
