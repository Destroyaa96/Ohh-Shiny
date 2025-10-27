package net.seto.ohhshiny

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.server.command.CommandManager.literal
import net.seto.ohhshiny.commands.OhhShinyCommand
import net.seto.ohhshiny.events.OhhShinyEventHandler
import net.seto.ohhshiny.events.OhhShinyTickHandler
import net.seto.ohhshiny.util.LuckPermsUtil
import org.slf4j.LoggerFactory

/**
 * Main entry point for the Ohh Shiny mod.
 * 
 * This mod allows admins to create interactive, one-time reward points that players can claim
 * by right-clicking blocks in the world. Each reward can only be claimed once per player.
 */
object OhhShiny : ModInitializer {
    const val MOD_ID = "ohhshiny"
    val LOGGER = LoggerFactory.getLogger("ohhshiny")

    override fun onInitialize() {
        LOGGER.info("Initializing Ohh Shiny mod")
        
        // Register event handlers for player interactions and particle effects
        OhhShinyEventHandler.register()
        OhhShinyTickHandler.register()
        
        // Register the /ohhshiny command with permission checks
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            val root = literal("ohhshiny")
                .requires { source -> 
                    // Allow access to the command if the player has either base or claim permissions
                    LuckPermsUtil.hasPermission(source, LuckPermsUtil.Permissions.OHHSHINY_BASE) ||
                    LuckPermsUtil.hasPermission(source, LuckPermsUtil.Permissions.OHHSHINY_CLAIM)
                }
            
            OhhShinyCommand.register(root)
            dispatcher.register(root)
        }
        
        LOGGER.info("Ohh Shiny mod initialized successfully")
    }
}
