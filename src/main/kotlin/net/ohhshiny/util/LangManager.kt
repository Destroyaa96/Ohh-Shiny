package net.ohhshiny.util

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileReader
import java.io.FileWriter

/**
 * Manages language files for configurable messages.
 * 
 * Supports:
 * - Loading language files from config directory
 * - Creating default language file if none exists
 * - TextPlaceholderAPI integration for dynamic placeholders
 * - Hot-reloading of language files
 */
object LangManager {
    private val logger = LoggerFactory.getLogger("ohhshiny-lang")
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    
    private val messages: MutableMap<String, String> = mutableMapOf()
    private lateinit var langFile: File
    
    /**
     * Initializes the language manager with the config directory.
     * Creates default language file if it doesn't exist.
     */
    fun initialize(configDirectory: File) {
        langFile = File(configDirectory, "lang.json")
        
        if (!langFile.exists()) {
            logger.info("Creating default language file at ${langFile.path}")
            createDefaultLangFile()
        }
        
        reload()
    }
    
    /**
     * Reloads language data from the configuration file.
     */
    fun reload() {
        messages.clear()
        
        if (!langFile.exists()) {
            logger.warn("Language file not found at ${langFile.path}, using default messages")
            loadDefaultMessages()
            return
        }
        
        try {
            FileReader(langFile).use { reader ->
                val json = gson.fromJson(reader, JsonObject::class.java)
                json.entrySet().forEach { (key, value) ->
                    if (value.isJsonPrimitive && value.asJsonPrimitive.isString) {
                        messages[key] = value.asString
                    }
                }
            }
            logger.info("Loaded ${messages.size} language entries from ${langFile.name}")
        } catch (e: Exception) {
            logger.error("Failed to load language file: ${e.message}", e)
            loadDefaultMessages()
        }
    }
    
    /**
     * Gets a message by key, with optional placeholder replacement.
     * 
     * @param key The message key
     * @param replacements Map of placeholder names to values (e.g., "x" to "123")
     * @return The formatted message with placeholders replaced, or the key if not found
     */
    fun getMessage(key: String, replacements: Map<String, String> = emptyMap()): String {
        var message = messages[key]
        
        if (message == null) {
            logger.warn("Missing language key: $key")
            return key
        }
        
        // Replace placeholders in format {placeholder_name}
        replacements.forEach { (placeholder, value) ->
            message = message!!.replace("{$placeholder}", value)
        }
        
        return message!!
    }
    
    /**
     * Loads default messages in case the language file is missing or corrupted.
     */
    private fun loadDefaultMessages() {
        messages.putAll(getDefaultMessages())
    }
    
    /**
     * Creates the default language file with all message keys.
     */
    private fun createDefaultLangFile() {
        try {
            langFile.parentFile?.mkdirs()
            
            val json = JsonObject()
            getDefaultMessages().forEach { (key, value) ->
                json.addProperty(key, value)
            }
            
            FileWriter(langFile).use { writer ->
                gson.toJson(json, writer)
            }
            
            logger.info("Created default language file at ${langFile.path}")
        } catch (e: Exception) {
            logger.error("Failed to create default language file: ${e.message}", e)
        }
    }
    
    /**
     * Returns the default English messages.
     * Uses Simplified Text Format for coloring (requires TextPlaceholderAPI).
     */
    private fun getDefaultMessages(): Map<String, String> {
        return mapOf(
            // Success messages
            "loot.created" to "<green>Ohh Shiny loot set at [{x}, {y}, {z}] in {dimension} with {item}",
            "loot.claimed" to "<aqua>You found something shiny: <r>{item}!",
            "loot.removed" to "<green>Removed Ohh Shiny loot at [{x}, {y}, {z}] in {dimension}",
            
            // Setup mode messages
            "setup.enabled" to "<yellow>Right-click a block to bind your held item as Ohh Shiny loot",
            "setup.disabled" to "<gray>Setup mode disabled",
            "remove.enabled" to "<yellow>Right-click an Ohh Shiny block to remove it",
            "remove.disabled" to "<gray>Remove mode disabled",
            
            // Error messages
            "error.already_claimed" to "<red>You've already claimed this loot!",
            "error.no_loot" to "<red>No Ohh Shiny loot at this location",
            "error.empty_hand" to "<red>You must hold an item in your main hand to create Ohh Shiny loot",
            "error.no_permission" to "<red>You don't have permission: {permission}",
            "error.permission_service" to "<red>Permission service unavailable; contact an administrator",
            "error.block_protected" to "<red>This block is protected and cannot be broken!",
            "error.no_player" to "<red>No valid player found",
            
            // List command messages
            "list.header" to "<gold>Active Ohh Shiny loot entries ({count} total):",
            "list.entry" to "• [{x}, {y}, {z}] in {dimension}: {item} (claimed by {claimed} players)",
            "list.empty" to "<gray>No Ohh Shiny loot entries found",
            "list.teleport_hover" to "<yellow>Click to teleport",
            
            // Admin command messages
            "admin.reload" to "<green>Ohh Shiny data reloaded ({count} entries)",
            "admin.reset" to "<green>Reset {count} Ohh Shiny claims for player {player}",
            "admin.cleared" to "<green>Cleared all Ohh Shiny data ({count} entries removed)",
            "admin.confirm" to "<yellow>This action cannot be undone. Run '{command}' again to confirm",
            "admin.given" to "<green>Given {item}",
            
            // Prefix - supports Simplified Text Format and placeholders
            "prefix" to "<aqua>[Ohh Shiny]</aqua>",
            
            // You can use:
            // - Simplified Text Format: <red>, <bold>, <gradient:blue:aqua>, <rainbow>, etc.
            // - TextPlaceholderAPI placeholders: %player:name%, %server:tps%, etc.
            // - Custom placeholders: {x}, {y}, {z}, {item}, {player}, etc.
        )
    }
}
