# WARP.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

## Project Overview

**Ohh-Shiny** is a Fabric Minecraft mod written in Kotlin that allows server administrators to create interactive, one-time reward points that players can claim by right-clicking blocks in the world. Each reward can only be claimed once per player.

- **Target Minecraft Version**: 1.21.1
- **Language**: Kotlin 2.2.0
- **Java Version**: 21
- **Mod Loader**: Fabric with fabric-language-kotlin
- **Dependencies**: Fabric API, LuckPerms (optional, for permissions)

## Build Commands

```powershell
# Build the mod
.\gradlew.bat build

# Run the Minecraft client for testing (if configured)
.\gradlew.bat runClient

# Run the Minecraft server for testing (if configured)
.\gradlew.bat runServer

# Clean build artifacts
.\gradlew.bat clean

# Generate data (for data generation)
.\gradlew.bat runDatagen
```

The compiled mod JAR will be located in `build/libs/` after building.

## Architecture

### Core Components

**OhhShiny** (`OhhShiny.kt`)
- Main mod entry point implementing `ModInitializer`
- Registers commands and event handlers on initialization
- Defines the mod ID: `"ohhshiny"`

**OhhShinyManager** (`OhhShinyManager.kt`)
- Central singleton manager for all reward operations
- Tracks admin mode state (setup mode, remove mode)
- Handles reward creation, removal, and player claiming
- Manages data persistence and reload operations
- Provides block protection checks

**OhhShinyState** (`data/OhhShinyState.kt`)
- Singleton for persistent storage of all reward data
- Stores data in `config/ohhshiny/ohhshiny.json`
- Automatically saves changes to disk
- Handles serialization/deserialization of entries

**OhhShinyEntry** (`data/OhhShinyEntry.kt`)
- Data class representing a single reward location
- Contains: world, position, reward item, and claimed player UUIDs
- Provides JSON serialization/deserialization with full NBT support
- Location key format: `"dimension|x|y|z"`

### Event System

**OhhShinyEventHandler** (`events/OhhShinyEventHandler.kt`)
- Intercepts `UseBlockCallback` for right-click interactions
- Priority order: setup mode → remove mode → claim mode
- Protects reward blocks from being broken via `PlayerBlockBreakEvents.BEFORE`
- Cleans up mode tracking on player disconnect

**OhhShinyTickHandler** (`events/OhhShinyTickHandler.kt`)
- Spawns particle effects at reward locations periodically

### Command System

**OhhShinyCommand** (`commands/OhhShinyCommand.kt`)
Commands available under `/ohhshiny`:
- `set` - Enter setup mode (right-click with item to create reward)
- `remove` - Enter remove mode (right-click to delete reward)
- `list` - View all active reward locations with claim counts
- `give chest <copper|iron|gold>` - Give custom chest player heads
- `give pokeball <poke|ultra|master>` - Give custom pokeball player heads
- `reload` - Reload all data from disk
- `reset <player>` - Reset a player's claim history
- `clearall` - Delete all rewards (requires confirmation within 30 seconds)

### Permission System

**LuckPermsUtil** (`util/LuckPermsUtil.kt`)
- Optional LuckPerms integration with vanilla fallback
- Falls back to permission level 2 (ops) when LuckPerms is unavailable
- Permission nodes:
  - `ohhshiny.claim` - Claim rewards (players)
  - `ohhshiny.command.set` - Create rewards
  - `ohhshiny.command.remove` - Delete rewards
  - `ohhshiny.command.list` - View reward list
  - `ohhshiny.command.give` - Give special items
  - `ohhshiny.command.reload` - Reload data
  - `ohhshiny.command.reset` - Reset player claims
  - `ohhshiny.command.clearall` - Delete all rewards

### Data Flow

1. **Creating a Reward**: Admin runs `/ohhshiny set` → enters setup mode → right-clicks block with item in hand → `OhhShinyManager.createLootEntry()` → `OhhShinyState.addLootEntry()` → saves to JSON
2. **Claiming a Reward**: Player right-clicks reward block → `OhhShinyEventHandler` intercepts → `OhhShinyManager.claimLoot()` → checks if already claimed → gives item → records claim → saves to JSON
3. **Persistence**: All changes immediately trigger `OhhShinyState.saveToDisk()` which writes to `config/ohhshiny/ohhshiny.json`

## Important Patterns

### Mode Tracking
- Admin modes (setup/remove) are tracked in-memory using `ConcurrentHashMap.newKeySet()`
- Modes are mutually exclusive (enabling one disables the other)
- Modes auto-disable after one use (after creating or removing a reward)
- Mode state is cleared when players disconnect

### Block Protection
- Blocks with Ohh Shiny rewards cannot be broken
- Protection is implemented via `PlayerBlockBreakEvents.BEFORE` returning `true` to cancel

### Confirmation Pattern
- Destructive commands like `/ohhshiny clearall` require double execution within 30 seconds
- Uses `pendingConfirmations` map with timestamp tracking

### ItemStack Serialization
- Attempts full NBT encoding first (preserves all data including enchantments, custom names)
- Falls back to basic item+count serialization if NBT encoding fails
- Requires `ServerWorld` instance for `RegistryWrapper.WrapperLookup`

### Error Handling
- Config directory is created automatically if missing
- Data loading gracefully handles missing or malformed entries
- World references are validated before deserializing entries
- Initial load without server context is deferred to first reload

## Development Notes

- Package structure: `net.seto.ohhshiny` (not `net.ohhshiny` as might be expected)
- Uses Kotlin object singletons extensively for stateless managers
- All server-side only - no client-side code needed
- Event handlers check `world.isClient` to ensure server-side execution
- Main hand interactions only (`hand.name == "MAIN_HAND"`)
- Particle effects require separate tick handler registration
