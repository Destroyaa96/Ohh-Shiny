package net.OOHSHINY.util

import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.OOHSHINY.OOHSHINY

/**
 * Utility for optional TextPlaceholderAPI integration.
 * 
 * If TextPlaceholderAPI is present, this allows text messages to include:
 * - Simplified Text Format tags like <red>, <bold>, <rainbow>, etc.
 * - Placeholders like %player:name%, %server:tps%, etc.
 */
object TextPlaceholderUtil {
    
    private var placeholderApiLoaded: Boolean = false
    
    init {
        // Check if TextPlaceholderAPI is available
        try {
            Class.forName("eu.pb4.placeholders.api.TextParserUtils")
            placeholderApiLoaded = true
            OOHSHINY.LOGGER.info("TextPlaceholderAPI detected - text formatting and placeholder support enabled")
        } catch (e: ClassNotFoundException) {
            OOHSHINY.LOGGER.info("TextPlaceholderAPI not found - using plain text messages")
        }
    }
    
    /**
     * Parses a text string with Simplified Text Format and placeholders if TextPlaceholderAPI is available.
     * 
     * Supports:
     * - Simplified Text Format: <red>, <bold>, <rainbow>, etc.
     * - Placeholders: %player:name%, %server:tps%, etc.
     * 
     * @param text The text to parse (can contain formatting and placeholders)
     * @param player The player context for placeholder resolution (optional)
     * @return Parsed Text object with formatting and placeholders resolved, or plain Text if API not available
     */
    fun parseText(text: String, player: ServerPlayerEntity? = null): Text {
        if (!placeholderApiLoaded) {
            return Text.literal(text)
        }
        
        return try {
            // First parse the Simplified Text Format (handles <red>, <bold>, etc.)
            @Suppress("DEPRECATION")
            val parsedText = eu.pb4.placeholders.api.TextParserUtils.formatText(text)
            
            // Then resolve placeholders if player context is provided
            if (player != null) {
                eu.pb4.placeholders.api.PlaceholderContext.of(player).let { context ->
                    eu.pb4.placeholders.api.Placeholders.parseText(parsedText, context)
                }
            } else {
                parsedText
            }
        } catch (e: Exception) {
            OOHSHINY.LOGGER.warn("Failed to parse text formatting: $text", e)
            Text.literal(text)
        }
    }
    
    /**
     * Parses a Text object with placeholders if TextPlaceholderAPI is available.
     * 
     * This overload is for Text objects that have already been formatted.
     * It only resolves placeholders, not the Simplified Text Format.
     * 
     * @param text The Text object to parse
     * @param player The player context for placeholder resolution (optional)
     * @return Parsed Text object with placeholders resolved, or original Text if API not available
     */
    fun parseText(text: Text, player: ServerPlayerEntity? = null): Text {
        if (!placeholderApiLoaded) {
            return text
        }
        
        return try {
            if (player != null) {
                eu.pb4.placeholders.api.PlaceholderContext.of(player).let { context ->
                    eu.pb4.placeholders.api.Placeholders.parseText(text, context)
                }
            } else {
                text
            }
        } catch (e: Exception) {
            OOHSHINY.LOGGER.warn("Failed to parse placeholders in text", e)
            text
        }
    }
    
    /**
     * Checks if TextPlaceholderAPI is loaded and available.
     */
    fun isAvailable(): Boolean = placeholderApiLoaded
}
