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
 * Data is stored in JSON format at: config/OOHSHINY/OOHSHINY.json
 * The file is automatically created on first use and updated whenever rewards are added/removed.
 */
object OOHSHINYState {
    private val logger = LoggerFactory.getLogger("OOHSHINY")
    private val lootEntries: MutableMap<String, OOHSHINYEntry> = mutableMapOf()
    private val storageFile: File
    
    init {
        val configDir = FabricLoader.getInstance().configDir.resolve("OOHSHINY").toFile()
        if (!configDir.exists()) configDir.mkdirs()
        storageFile = File(configDir, "OOHSHINY.json")
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
     * Loads reward data from the JSON file on disk.
     * Requires a server instance to resolve world references.
     */
    private fun loadFromDisk(server: MinecraftServer? = null) {
        if (!storageFile.exists()) return
        
        try {
            val reader = FileReader(storageFile)
            val jsonObject = JsonParser.parseReader(reader).asJsonObject
            reader.close()
            
            for ((key, entryElement) in jsonObject.entrySet()) {
                try {
                    val entryObj = entryElement.asJsonObject
                    
                    // World instances are needed to deserialize ItemStacks, but aren't available during mod init
                    // Data will be loaded on the first /OOHSHINY reload command after server starts
                    if (server == null) {
                        logger.warn("Cannot load Ooh Shiny entries without server context")
                        continue
                    }
                    
                    // Resolve the dimension to get the world instance
                    val dimensionStr = entryObj.get("dimension").asString
                    val dimensionId = net.minecraft.util.Identifier.tryParse(dimensionStr)
                    val dimensionKey = net.minecraft.registry.RegistryKey.of(
                        net.minecraft.registry.RegistryKeys.WORLD,
                        dimensionId
                    )
                    val world = server.getWorld(dimensionKey)
                    
                    if (world == null) {
                        logger.warn("Could not find world for dimension: $dimensionStr")
                        continue
                    }
                    
                    val entry = OOHSHINYEntry.readFromJson(world, entryObj)
                    if (entry != null) {
                        lootEntries[key] = entry
                    } else {
                        logger.warn("Failed to load Ooh Shiny entry with key: $key")
                    }
                } catch (e: Exception) {
                    logger.warn("Failed to load Ooh Shiny entry for key $key", e)
                }
            }
            
            logger.info("Loaded ${lootEntries.size} Ooh Shiny entries")
        } catch (e: Exception) {
            logger.error("Failed to load Ooh Shiny storage file", e)
        }
    }
    
    /**
     * Writes all reward data to the JSON file on disk.
     */
    private fun saveToDisk() {
        try {
            val jsonObject = JsonObject()
            
            for ((key, entry) in lootEntries) {
                jsonObject.add(key, entry.writeToJson())
            }
            
            val writer = FileWriter(storageFile)
            val gson = GsonBuilder().setPrettyPrinting().create()
            gson.toJson(jsonObject, writer)
            writer.close()
        } catch (e: Exception) {
            logger.error("Failed to save Ooh Shiny storage file", e)
        }
    }
}
