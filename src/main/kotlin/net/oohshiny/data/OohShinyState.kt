package net.OOHSHINY.data

import com.google.gson.*
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.registry.RegistryKey
import net.minecraft.server.MinecraftServer
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileReader
import java.io.FileWriter

/**
 * Singleton object that manages persistent storage of all Ooh Shiny rewards.
 * 
 * Data is stored in JSON format at: config/oohshiny/oohshiny.json
 * The file is automatically created on first use and updated whenever rewards are added/removed.
 */
object OOHSHINYState {
    private val logger = LoggerFactory.getLogger("oohshiny")
    private val lootEntries: MutableMap<String, OOHSHINYEntry> = mutableMapOf()
    private val storageFile: File
    private val categories: MutableSet<String> = mutableSetOf("default")
    
    init {
        val configDir = FabricLoader.getInstance().configDir.resolve("oohshiny").toFile()
        if (!configDir.exists()) configDir.mkdirs()
        storageFile = File(configDir, "oohshiny.json")
        loadFromDisk()
    }
    
    /**
     * Adds a new reward entry and immediately saves to disk.
     */
    fun addLootEntry(entry: OOHSHINYEntry) {
        lootEntries[entry.getLocationKey()] = entry
        saveToDisk()
    }
    
    /**
     * Removes a reward entry by its location and saves to disk if found.
     * @return The removed entry, or null if no reward existed at that location
     */
    fun removeLootEntry(dimension: RegistryKey<World>, position: BlockPos): OOHSHINYEntry? {
        val key = "${dimension.value}|${position.x}|${position.y}|${position.z}"
        val removed = lootEntries.remove(key)
        if (removed != null) {
            saveToDisk()
        }
        return removed
    }
    
    /**
     * Retrieves a reward entry by its location.
     * @return The entry if one exists at that location, otherwise null
     */
    fun getLootEntry(dimension: RegistryKey<World>, position: BlockPos): OOHSHINYEntry? {
        val key = "${dimension.value}|${position.x}|${position.y}|${position.z}"
        return lootEntries[key]
    }
    
    /**
     * Returns a copy of all reward entries (keyed by location string).
     */
    fun getAllLootEntries(): Map<String, OOHSHINYEntry> {
        return lootEntries.toMap()
    }
    
    /**
     * Deletes all reward entries and clears the storage file.
     */
    fun clearAllEntries() {
        lootEntries.clear()
        saveToDisk()
    }
    
    /**
     * Returns the total number of active reward locations.
     */
    fun getEntryCount(): Int = lootEntries.size
    
    /**
     * Creates a new category if it doesn't exist.
     */
    fun createCategory(category: String) {
        categories.add(category)
        saveToDisk()
    }
    
    /**
     * Returns all available categories.
     */
    fun getCategories(): Set<String> = categories.toSet()
    
    /**
     * Returns all entries in a specific category.
     */
    fun getEntriesByCategory(category: String): Map<String, OOHSHINYEntry> {
        return lootEntries.filter { it.value.category == category }
    }
    
    /**
     * Marks data as modified and triggers a save to disk.
     * Used when claim data is updated without adding/removing entries.
     */
    fun markDirty() {
        saveToDisk()
    }
    
    /**
     * Discards in-memory data and reloads everything from the storage file.
     */
    fun reload(server: MinecraftServer? = null) {
        lootEntries.clear()
        loadFromDisk(server)
    }
    
    /**
     * Parses a dimension string into a world instance.
     */
    private fun parseDimension(dimensionStr: String, server: MinecraftServer?): net.minecraft.server.world.ServerWorld? {
        if (server == null) return null
        
        val dimensionId = net.minecraft.util.Identifier.tryParse(dimensionStr)
        val dimensionKey = net.minecraft.registry.RegistryKey.of(
            net.minecraft.registry.RegistryKeys.WORLD,
            dimensionId
        )
        return server.getWorld(dimensionKey)
    }
    
    /**
     * Loads reward data from the JSON file on disk.
     * Requires a server instance to resolve world references.
     */
    private fun loadFromDisk(server: MinecraftServer? = null) {
        if (!storageFile.exists()) return
        
        try {
            val reader = FileReader(storageFile)
            val rootObject = JsonParser.parseReader(reader).asJsonObject
            reader.close()
            
            // Check if this is the new category-first format
            val isNewFormat = rootObject.entrySet().any { 
                it.value.isJsonObject && it.value.asJsonObject.entrySet().any { entry ->
                    entry.value.isJsonObject && entry.value.asJsonObject.has("dimension")
                }
            }
            
            if (isNewFormat) {
                // New format: categories -> entries
                categories.clear()
                
                for ((category, categoryEntriesElement) in rootObject.entrySet()) {
                    if (!categoryEntriesElement.isJsonObject) continue
                    
                    categories.add(category)
                    val categoryEntries = categoryEntriesElement.asJsonObject
                    
                    for ((key, entryElement) in categoryEntries.entrySet()) {
                        try {
                            val entryObj = entryElement.asJsonObject
                            
                            if (server == null) {
                                logger.warn("Cannot load Ooh Shiny entries without server context")
                                continue
                            }
                            
                            val dimensionStr = entryObj.get("dimension").asString
                            val world = parseDimension(dimensionStr, server)
                            
                            if (world == null) {
                                logger.warn("Could not find world for dimension: $dimensionStr")
                                continue
                            }
                            
                            val entry = OOHSHINYEntry.readFromJson(world, entryObj, category)
                            if (entry != null) {
                                lootEntries[key] = entry
                            } else {
                                logger.warn("Failed to load Ooh Shiny entry with key: $key")
                            }
                        } catch (e: Exception) {
                            logger.warn("Failed to load Ooh Shiny entry for key $key in category $category", e)
                        }
                    }
                }
            } else {
                // Old format: direct entries or entries object
                val jsonObject = if (rootObject.has("entries")) {
                    // Old format with "entries" wrapper
                    if (rootObject.has("categories")) {
                        val categoriesArray = rootObject.getAsJsonArray("categories")
                        categories.clear()
                        categoriesArray.forEach { categories.add(it.asString) }
                    }
                    rootObject.getAsJsonObject("entries")
                } else {
                    // Very old format - root is entries
                    rootObject
                }
                
                for ((key, entryElement) in jsonObject.entrySet()) {
                    try {
                        val entryObj = entryElement.asJsonObject
                        
                        if (server == null) {
                            logger.warn("Cannot load Ooh Shiny entries without server context")
                            continue
                        }
                        
                        val dimensionStr = entryObj.get("dimension").asString
                        val world = parseDimension(dimensionStr, server)
                        
                        if (world == null) {
                            logger.warn("Could not find world for dimension: $dimensionStr")
                            continue
                        }
                        
                        val entry = OOHSHINYEntry.readFromJson(world, entryObj)
                        if (entry != null) {
                            lootEntries[key] = entry
                            // Add category from entry to categories set
                            categories.add(entry.category)
                        } else {
                            logger.warn("Failed to load Ooh Shiny entry with key: $key")
                        }
                    } catch (e: Exception) {
                        logger.warn("Failed to load Ooh Shiny entry for key $key", e)
                    }
                }
            }
            
            logger.info("Loaded ${lootEntries.size} Ooh Shiny entries across ${categories.size} categories")
        } catch (e: Exception) {
            logger.error("Failed to load Ooh Shiny storage file", e)
        }
    }
    
    /**
     * Writes all reward data to the JSON file on disk.
     * Organizes entries by category for better readability.
     */
    private fun saveToDisk() {
        try {
            val rootObject = JsonObject()
            
            // Group entries by category
            val entriesByCategory = lootEntries.values.groupBy { it.category }
            
            // Save each category with its entries
            for (category in categories.sorted()) {
                val categoryEntries = entriesByCategory[category] ?: emptyList()
                val categoryObject = JsonObject()
                
                for (entry in categoryEntries) {
                    categoryObject.add(entry.getLocationKey(), entry.writeToJson())
                }
                
                rootObject.add(category, categoryObject)
            }
            
            val writer = FileWriter(storageFile)
            val gson = GsonBuilder().setPrettyPrinting().create()
            gson.toJson(rootObject, writer)
            writer.close()
        } catch (e: Exception) {
            logger.error("Failed to save Ooh Shiny storage file", e)
        }
    }
}
