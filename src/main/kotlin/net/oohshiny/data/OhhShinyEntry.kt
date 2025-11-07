package net.oohshiny.data

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.minecraft.item.ItemStack
import net.minecraft.nbt.StringNbtReader
import net.minecraft.registry.Registries
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryWrapper
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import java.util.*

/**
 * Represents a single Ohh Shiny reward location in the world.
 * 
 * Contains the location, the reward item, and tracks which players have already claimed it.
 * Provides serialization/deserialization for persistent storage.
 */
data class OhhShinyEntry(
    val world: ServerWorld,
    val position: BlockPos,
    val rewardItem: ItemStack,
    val claimedPlayers: MutableSet<UUID> = mutableSetOf()
) {

    /** Convenience property to get the dimension key from the world */
    val dimension: RegistryKey<World> get() = world.registryKey

    /** Creates a unique string key for this location (used as map key in storage) */
    fun getLocationKey(): String =
        "${dimension.value}|${position.x}|${position.y}|${position.z}"

    /** Checks if a specific player has already claimed this reward */
    fun hasPlayerClaimed(playerUuid: UUID) = claimedPlayers.contains(playerUuid)
    
    /** Records that a player has claimed this reward */
    fun claimForPlayer(playerUuid: UUID) = claimedPlayers.add(playerUuid)
    
    /** Removes a player's claim record, allowing them to claim again */
    fun resetPlayerClaim(playerUuid: UUID) = claimedPlayers.remove(playerUuid)

    /** Serializes this entry to JSON for file storage */
    fun writeToJson(): JsonObject {
        val json = JsonObject()
        json.addProperty("dimension", dimension.value.toString())

        val posJson = JsonObject().apply {
            addProperty("x", position.x)
            addProperty("y", position.y)
            addProperty("z", position.z)
        }
        json.add("position", posJson)

        json.add("rewardItem", serializeItemStack(rewardItem))

        val claimedArray = JsonArray()
        claimedPlayers.forEach { claimedArray.add(it.toString()) }
        json.add("claimedPlayers", claimedArray)

        return json
    }

    /**
     * Serializes an ItemStack to JSON, preserving NBT data if present.
     * Falls back to basic item+count if NBT encoding fails.
     */
    private fun serializeItemStack(itemStack: ItemStack): JsonObject {
        val obj = JsonObject()
        if (itemStack.isEmpty) {
            obj.addProperty("empty", true)
            return obj
        }

        val registryLookup: RegistryWrapper.WrapperLookup = world.registryManager
        try {
            // Try to encode with full NBT data (preserves enchantments, custom names, etc.)
            val encoded = itemStack.encode(registryLookup)
            if (encoded != null) {
                obj.addProperty("nbt", encoded.toString())
                return obj
            }
        } catch (e: Exception) {
            // Fall back to basic serialization if NBT encoding fails
        }

        obj.addProperty("item", Registries.ITEM.getId(itemStack.item).toString())
        obj.addProperty("count", itemStack.count)
        return obj
    }

    companion object {
        /**
         * Deserializes an OhhShinyEntry from JSON.
         * Returns null if the data is malformed or cannot be parsed.
         */
        fun readFromJson(world: ServerWorld, json: JsonObject): OhhShinyEntry? {
            try {
                val posJson = json.getAsJsonObject("position")
                val position = BlockPos(
                    posJson.get("x").asInt,
                    posJson.get("y").asInt,
                    posJson.get("z").asInt
                )

                val itemJson = json.getAsJsonObject("rewardItem")
                val rewardItem = deserializeItemStack(world, itemJson)

                val claimedArray = json.getAsJsonArray("claimedPlayers")
                val claimedPlayers = mutableSetOf<UUID>()
                claimedArray.forEach { element ->
                    try {
                        claimedPlayers.add(UUID.fromString(element.asString))
                    } catch (_: Exception) {}
                }

                return OhhShinyEntry(world, position, rewardItem, claimedPlayers)
            } catch (e: Exception) {
                return null
            }
        }

        /**
         * Deserializes an ItemStack from JSON, handling both NBT and basic formats.
         */
        private fun deserializeItemStack(world: ServerWorld, obj: JsonObject): ItemStack {
            if (obj.has("empty") && obj.get("empty").asBoolean) return ItemStack.EMPTY

            val lookup = world.registryManager
            if (obj.has("nbt")) {
                val nbtStr = obj.get("nbt").asString
                val parsed = StringNbtReader.parse(nbtStr)
                return ItemStack.fromNbtOrEmpty(lookup, parsed)
            }

            if (obj.has("item") && obj.has("count")) {
                val id = Identifier.tryParse(obj.get("item").asString)
                val item = Registries.ITEM.get(id)
                val count = obj.get("count").asInt
                return ItemStack(item, count)
            }

            return ItemStack.EMPTY
        }
    }
}
