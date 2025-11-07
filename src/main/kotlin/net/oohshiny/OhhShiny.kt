package net.oohshiny

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.minecraft.server.command.CommandManager.literal
import net.oohshiny.commands.OhhShinyCommand
import net.oohshiny.events.OhhShinyEventHandler
import net.oohshiny.events.OhhShinyTickHandler
import net.oohshiny.util.LangManager
import net.oohshiny.util.LuckPermsUtil
import org.slf4j.LoggerFactory
import net.fabricmc.loader.api.FabricLoader
import java.io.File

/**
 * Main entry point for the Ohh Shiny mod.
 * 
 * This mod allows admins to create interactive, one-time reward points that players can claim
 * by right-clicking blocks in the world. Each reward can only be claimed once per player.
 */
object OhhShiny : ModInitializer {
    const val MOD_ID = "oohshiny"
    val LOGGER = LoggerFactory.getLogger("oohshiny")

    override fun onInitialize() {
        LOGGER.info("Initializing Ohh Shiny mod")
        
        // Initialize language manager with config directory
        val configDir = File(FabricLoader.getInstance().configDir.toFile(), MOD_ID)
        configDir.mkdirs()
        LangManager.initialize(configDir)
        
        // Register event handlers for player interactions and particle effects
        OhhShinyEventHandler.register()
        OhhShinyTickHandler.register()
        
        // Auto-load data when server starts
        ServerLifecycleEvents.SERVER_STARTED.register { server ->
            LOGGER.info("Server started, loading Ohh Shiny data...")
            OhhShinyManager.reloadData(server)
        }
        
        // Register the /oohshiny command with permission checks
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            val root = literal("oohshiny")
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
