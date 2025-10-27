package net.seto.ohhshiny

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.server.command.CommandManager.literal
import net.seto.ohhshiny.commands.OhhShinyCommand
import net.seto.ohhshiny.events.OhhShinyEventHandler
import net.seto.ohhshiny.events.OhhShinyTickHandler
import net.seto.ohhshiny.util.LuckPermsUtil
import org.slf4j.LoggerFactory

object OhhShiny : ModInitializer {
    const val MOD_ID = "ohhshiny"
    val LOGGER = LoggerFactory.getLogger("ohhshiny")

    override fun onInitialize() {
        LOGGER.info("Initializing Ohh Shiny mod")
        
        // Register event handlers
        OhhShinyEventHandler.register()
        OhhShinyTickHandler.register()
        
        // Register commands
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            val root = literal("ohhshiny")
                .requires { source -> 
                    // Allow access if user has any Ohh Shiny permission
                    LuckPermsUtil.hasPermission(source, LuckPermsUtil.Permissions.OHHSHINY_BASE) ||
                    LuckPermsUtil.hasPermission(source, LuckPermsUtil.Permissions.OHHSHINY_CLAIM)
                }
            
            OhhShinyCommand.register(root)
            dispatcher.register(root)
        }
        
        LOGGER.info("Ohh Shiny mod initialized successfully")
    }
}
