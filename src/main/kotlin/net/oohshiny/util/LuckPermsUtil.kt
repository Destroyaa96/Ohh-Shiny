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
    
    /**
     * Checks if a player has permission to claim loots in a specific category.
     * Checks in order:
     * 1. oohshiny.claim.category.<category> (specific category permission)
     * 2. oohshiny.claim.* (all categories)
     * 3. oohshiny.claim (base claim permission)
     */
    fun hasClaimPermission(player: ServerPlayerEntity, category: String): Boolean {
        // Initialize LuckPerms if not already done
        initializeLuckPerms()
        
        // Try LuckPerms first
        luckPerms?.let { lp ->
            try {
                val user = lp.userManager.getUser(player.uuid)
                if (user != null) {
                    val permissionData = user.cachedData.permissionData
                    
                    // Check specific category permission first
                    val categoryPerm = "${Permissions.OOHSHINY_CLAIM}.category.$category"
                    if (permissionData.checkPermission(categoryPerm).asBoolean()) {
                        return true
                    }
                    
                    // Check wildcard category permission
                    val wildcardPerm = "${Permissions.OOHSHINY_CLAIM}.category.*"
                    if (permissionData.checkPermission(wildcardPerm).asBoolean()) {
                        return true
                    }
                    
                    // Check base claim permission
                    val basePerm = Permissions.OOHSHINY_CLAIM
                    if (permissionData.checkPermission(basePerm).asBoolean()) {
                        return true
                    }
                    
                    // No permissions matched
                    return false
                }
            } catch (e: Exception) {
                logger.warn("Error checking claim permission for category '$category' for player ${player.nameForScoreboard}", e)
            }
        }
        
        // Fallback to vanilla permissions (OP level 2)
        return player.commandSource.hasPermissionLevel(2)
    }
    
    fun isLuckPermsAvailable(): Boolean {
        initializeLuckPerms()
        return luckPerms != null
    }
    
    // Permission constants for Ooh Shiny
    object Permissions {
        // Base permission node
        const val OOHSHINY_BASE = "oohshiny"
        
        // Admin wildcard - grants all admin commands
        const val OOHSHINY_ADMIN = "oohshiny.admin"
        
        // Command permissions - for administrative operations
        const val OOHSHINY_COMMAND_CREATE = "oohshiny.command.create"
        const val OOHSHINY_COMMAND_SET = "oohshiny.command.set"
        const val OOHSHINY_COMMAND_REMOVE = "oohshiny.command.remove"
        const val OOHSHINY_COMMAND_LIST = "oohshiny.command.list"
        const val OOHSHINY_COMMAND_RELOAD = "oohshiny.command.reload"
        const val OOHSHINY_COMMAND_RESET = "oohshiny.command.reset"
        const val OOHSHINY_COMMAND_CLEARALL = "oohshiny.command.clearall"
        const val OOHSHINY_COMMAND_GIVE = "oohshiny.command.give"
        const val OOHSHINY_COMMAND_COMPLETION = "oohshiny.command.completion"
        const val OOHSHINY_COMMAND_TELEPORT = "oohshiny.command.teleport"
        
        // Claim permissions - for players to claim loots
        const val OOHSHINY_CLAIM = "oohshiny.claim"
        // Per-category claim: oohshiny.claim.category.<category>
        // Wildcard category claim: oohshiny.claim.category.*
        
        // Legacy permissions for backward compatibility
        @Deprecated("Use OOHSHINY_COMMAND_SET instead", ReplaceWith("OOHSHINY_COMMAND_SET"))
        const val OOHSHINY_SET = "oohshiny.command.set"
        @Deprecated("Use OOHSHINY_COMMAND_REMOVE instead", ReplaceWith("OOHSHINY_COMMAND_REMOVE"))
        const val OOHSHINY_REMOVE = "oohshiny.command.remove"
        @Deprecated("Use OOHSHINY_COMMAND_LIST instead", ReplaceWith("OOHSHINY_COMMAND_LIST"))
        const val OOHSHINY_LIST = "oohshiny.command.list"
        @Deprecated("Use OOHSHINY_COMMAND_RELOAD instead", ReplaceWith("OOHSHINY_COMMAND_RELOAD"))
        const val OOHSHINY_RELOAD = "oohshiny.command.reload"
        @Deprecated("Use OOHSHINY_COMMAND_RESET instead", ReplaceWith("OOHSHINY_COMMAND_RESET"))
        const val OOHSHINY_RESET = "oohshiny.command.reset"
        @Deprecated("Use OOHSHINY_COMMAND_CLEARALL instead", ReplaceWith("OOHSHINY_COMMAND_CLEARALL"))
        const val OOHSHINY_CLEARALL = "oohshiny.command.clearall"
        @Deprecated("Use OOHSHINY_COMMAND_GIVE instead", ReplaceWith("OOHSHINY_COMMAND_GIVE"))
        const val OOHSHINY_GIVE = "oohshiny.command.give"
    }
}
