package net.ohhshiny.util

import net.luckperms.api.LuckPerms
import net.luckperms.api.LuckPermsProvider
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import org.slf4j.LoggerFactory

object LuckPermsUtil {
    private val logger = LoggerFactory.getLogger("ohhshiny-luckperms")
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
    
    // Permission constants for Ohh Shiny
    object Permissions {
        const val BASE = "ohhshiny.admin"
        
        // Ohh Shiny permissions
        const val OHHSHINY_BASE = "ohhshiny"
        const val OHHSHINY_SET = "ohhshiny.command.set"
        const val OHHSHINY_REMOVE = "ohhshiny.command.remove"
        const val OHHSHINY_LIST = "ohhshiny.command.list"
        const val OHHSHINY_RELOAD = "ohhshiny.command.reload"
        const val OHHSHINY_RESET = "ohhshiny.command.reset"
        const val OHHSHINY_CLEARALL = "ohhshiny.command.clearall"
        const val OHHSHINY_CLAIM = "ohhshiny.claim"
        const val OHHSHINY_TELEPORT = "ohhshiny.command.teleport"
        const val OHHSHINY_GIVE = "ohhshiny.command.give"
    }
}
