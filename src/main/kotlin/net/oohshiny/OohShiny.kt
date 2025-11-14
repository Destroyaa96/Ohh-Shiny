package net.OOHSHINY

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.minecraft.server.command.CommandManager.literal
import net.OOHSHINY.commands.OOHSHINYCommand
import net.OOHSHINY.events.OOHSHINYEventHandler
import net.OOHSHINY.events.OOHSHINYTickHandler
import net.OOHSHINY.util.LangManager
import net.OOHSHINY.util.LuckPermsUtil
import org.slf4j.LoggerFactory
import net.fabricmc.loader.api.FabricLoader
import java.io.File

/**
 * Main entry point for the Ooh Shiny mod.
 * 
 * This mod allows admins to create interactive, one-time reward points that players can claim
 * by right-clicking blocks in the world. Each reward can only be claimed once per player.
 */
object OOHSHINY : ModInitializer {
    const val MOD_ID = "oohshiny"
    val LOGGER = LoggerFactory.getLogger("oohshiny")

    override fun onInitialize() {
        LOGGER.info("Initializing Ooh Shiny mod")
        
        // Initialize language manager with config directory
        val configDir = File(FabricLoader.getInstance().configDir.toFile(), MOD_ID)
        configDir.mkdirs()
        LangManager.initialize(configDir)
        
        // Register event handlers for player interactions and particle effects
        OOHSHINYEventHandler.register()
        OOHSHINYTickHandler.register()
        
        // Auto-load data when server starts
        ServerLifecycleEvents.SERVER_STARTED.register { server ->
            LOGGER.info("Server started, loading Ooh Shiny data...")
            OOHSHINYManager.reloadData(server)
        }
        
        // Register the /oohshiny command with permission checks
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            val root = literal("oohshiny")
                .requires { source -> 
                    // Allow access to the command if the player has either base or claim permissions
                    LuckPermsUtil.hasPermission(source, LuckPermsUtil.Permissions.OOHSHINY_BASE) ||
                    LuckPermsUtil.hasPermission(source, LuckPermsUtil.Permissions.OOHSHINY_CLAIM)
                }
            
            OOHSHINYCommand.register(root)
            dispatcher.register(root)
        }
        
        LOGGER.info("Ooh Shiny mod initialized successfully")
    }
}
