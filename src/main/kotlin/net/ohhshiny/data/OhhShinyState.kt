package net.seto.ohhshiny.data

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
 * JSON file storage for Ohh Shiny data
 * Data is saved in config/ohhshiny/ohhshiny.json
 */
object OhhShinyState {
    private val logger = LoggerFactory.getLogger("ohhshiny")
    private val lootEntries: MutableMap<String, OhhShinyEntry> = mutableMapOf()
    private val storageFile: File
    
    init {
        val configDir = FabricLoader.getInstance().configDir.resolve("ohhshiny").toFile()
        if (!configDir.exists()) configDir.mkdirs()
        storageFile = File(configDir, "ohhshiny.json")
        loadFromDisk()
    }
    
    /**
     * Adds a new Ohh Shiny entry
     */
    fun addLootEntry(entry: OhhShinyEntry) {
        lootEntries[entry.getLocationKey()] = entry
        saveToDisk()
    }
    
    /**
     * Removes an Ohh Shiny entry by location key
     */
    fun removeLootEntry(dimension: RegistryKey<World>, position: BlockPos): OhhShinyEntry? {
        val key = "${dimension.value}|${position.x}|${position.y}|${position.z}"
        val removed = lootEntries.remove(key)
        if (removed != null) {
            saveToDisk()
        }
        return removed
    }
    
    /**
     * Gets an Ohh Shiny entry by location
     */
    fun getLootEntry(dimension: RegistryKey<World>, position: BlockPos): OhhShinyEntry? {
        val key = "${dimension.value}|${position.x}|${position.y}|${position.z}"
        return lootEntries[key]
    }
    
    /**
     * Gets all loot entries
     */
    fun getAllLootEntries(): Map<String, OhhShinyEntry> {
        return lootEntries.toMap()
    }
    
    /**
     * Removes all loot entries
     */
    fun clearAllEntries() {
        lootEntries.clear()
        saveToDisk()
    }
    
    /**
     * Gets the count of loot entries
     */
    fun getEntryCount(): Int = lootEntries.size
    
    /**
     * Save changes to disk when entries are modified (used by resetPlayerClaims)
     */
    fun markDirty() {
        saveToDisk()
    }
    
    /**
     * Reloads all data from disk, discarding current in-memory data
     */
    fun reload(server: MinecraftServer? = null) {
        lootEntries.clear()
        loadFromDisk(server)
    }
    
    private fun loadFromDisk(server: MinecraftServer? = null) {
        if (!storageFile.exists()) return
        
        try {
            val reader = FileReader(storageFile)
            val jsonObject = JsonParser.parseReader(reader).asJsonObject
            reader.close()
            
            for ((key, entryElement) in jsonObject.entrySet()) {
                try {
                    val entryObj = entryElement.asJsonObject
                    
                    // We need the server to get the world, but at init time we don't have it
                    // Skip loading if no server is available
                    if (server == null) {
                        logger.warn("Cannot load Ohh Shiny entries without server context")
                        continue
                    }
                    
                    // Get the dimension from the JSON
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
                    
                    val entry = OhhShinyEntry.readFromJson(world, entryObj)
                    if (entry != null) {
                        lootEntries[key] = entry
                    } else {
                        logger.warn("Failed to load Ohh Shiny entry with key: $key")
                    }
                } catch (e: Exception) {
                    logger.warn("Failed to load Ohh Shiny entry for key $key", e)
                }
            }
            
            logger.info("Loaded ${lootEntries.size} Ohh Shiny entries")
        } catch (e: Exception) {
            logger.error("Failed to load Ohh Shiny storage file", e)
        }
    }
    
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
            logger.error("Failed to save Ohh Shiny storage file", e)
        }
    }
}
