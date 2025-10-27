package net.seto.ohhshiny.data

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

data class OhhShinyEntry(
    val world: ServerWorld,
    val position: BlockPos,
    val rewardItem: ItemStack,
    val claimedPlayers: MutableSet<UUID> = mutableSetOf()
) {

    /** Derived property for compatibility */
    val dimension: RegistryKey<World> get() = world.registryKey

    fun getLocationKey(): String =
        "${dimension.value}|${position.x}|${position.y}|${position.z}"

    fun hasPlayerClaimed(playerUuid: UUID) = claimedPlayers.contains(playerUuid)
    fun claimForPlayer(playerUuid: UUID) = claimedPlayers.add(playerUuid)
    fun resetPlayerClaim(playerUuid: UUID) = claimedPlayers.remove(playerUuid)

    /** Uses world.registryManager directly */
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

    private fun serializeItemStack(itemStack: ItemStack): JsonObject {
        val obj = JsonObject()
        if (itemStack.isEmpty) {
            obj.addProperty("empty", true)
            return obj
        }

        val registryLookup: RegistryWrapper.WrapperLookup = world.registryManager
        try {
            val encoded = itemStack.encode(registryLookup)
            if (encoded != null) {
                obj.addProperty("nbt", encoded.toString())
                return obj
            }
        } catch (e: Exception) {
            // fallback
        }

        obj.addProperty("item", Registries.ITEM.getId(itemStack.item).toString())
        obj.addProperty("count", itemStack.count)
        return obj
    }

    companion object {
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
