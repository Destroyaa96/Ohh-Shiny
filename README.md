# Ooh Shiny

A Minecraft Fabric mod that allows server administrators to create interactive, one-time reward points that players can claim by right-clicking blocks in the world.

## Features

- **Interactive Rewards**: Create claimable reward locations anywhere in your Minecraft world
- **One-Time Claims**: Each reward can only be claimed once per player
- **Visual Feedback**: Particle effects for creating, removing, and claiming rewards
- **Block Protection**: Rewards are protected from being broken
- **Permission System**: Full LuckPerms integration for granular access control
- **Persistent Storage**: All reward data is automatically saved to disk
- **Multi-Dimension Support**: Place rewards in any dimension (Overworld, Nether, End, custom dimensions)
- **Custom Items**: Built-in support for giving custom textured player heads (chests and pokeballs)

## Requirements

- **Minecraft**: 1.21.1
- **Fabric Loader**: 0.17.2 or newer
- **Fabric API**: Required
- **Fabric Language Kotlin**: Required
- **Java**: 21 or newer
- **LuckPerms**: Optional (for advanced permission management)
- **TextPlaceholderAPI**: Optional (for text formatting in language files)

## Installation

1. Place the JAR file in your `mods` folder
2. (Optional) Install [LuckPerms](https://luckperms.net/) for advanced permission management
3. (Optional) Install [TextPlaceholderAPI](https://modrinth.com/mod/placeholder-api) for text formatting
4. Start your server

## Usage

### Creating Rewards

1. Hold the item you want to give as a reward in your main hand
2. Run `/oohshiny set` to enter setup mode
3. Right-click on any block to create a reward at that location
4. The reward is immediately saved and ready for players to claim

**Note**: Setup mode automatically exits after placing one reward. Run `/oohshiny set` again to place another.

### Removing Rewards

1. Run `/oohshiny remove` to enter remove mode
2. Right-click on a block with an existing reward to delete it
3. The reward and all associated claim data are permanently removed

**Note**: Remove mode automatically exits after removing one reward.

### Claiming Rewards

Players with claim permissions can simply right-click on any block that has a reward:
- If they haven't claimed it before, they receive the reward item
- If they've already claimed it, they see a message indicating they've already claimed it
- If no reward exists at that location, normal block interactions occur

### Administrative Commands

All commands require appropriate permissions (see Permissions section below).

#### `/oohshiny set`
Enters setup mode to create a new reward location.

#### `/oohshiny remove`
Enters remove mode to delete an existing reward location.

#### `/oohshiny list`
Lists all active reward locations with details:
- World dimension
- Coordinates
- Reward item
- Number of players who have claimed it
- Clickable teleport link for easy navigation

#### `/oohshiny give chest <type>`
Gives a custom textured chest player head.
- Types: `copper`, `iron`, `gold`
- These are special items perfect for use as visually distinctive rewards

#### `/oohshiny give pokeball <type>`
Gives a custom textured pokeball player head.
- Types: `poke`, `ultra`, `master`
- Great for themed servers or special events

#### `/oohshiny reload`
Reloads all reward data from the configuration file on disk.
- Useful for applying manual edits or recovering from errors
- Shows the number of rewards loaded

#### `/oohshiny reloadlang`
Reloads the language file (`lang.json`) without restarting the server.
- Apply message customizations immediately
- No server restart required

#### `/oohshiny reset <player>`
Resets a specific player's claim history, allowing them to claim all rewards again.
- Useful for testing or special events
- Shows the number of claims reset

#### `/oohshiny clearall`
**DESTRUCTIVE**: Permanently deletes all rewards and claim data.
- Requires confirmation (run the command twice within 30 seconds)
- This action cannot be undone
- Use with extreme caution

## Permissions

Ooh Shiny uses LuckPerms for permission management. If LuckPerms is not installed, all commands default to requiring operator status (level 2+).

### Available Permissions

| Permission | Description | Default |
|-----------|-------------|---------|
| `oohshiny.base` | Access to the base `/oohshiny` command | OP |
| `oohshiny.claim` | Ability to claim rewards by right-clicking blocks | OP |
| `oohshiny.set` | Create new reward locations | OP |
| `oohshiny.remove` | Delete existing reward locations | OP |
| `oohshiny.list` | View all active reward locations | OP |
| `oohshiny.give` | Spawn custom textured items | OP |
| `oohshiny.reload` | Reload data from disk | OP |
| `oohshiny.reset` | Reset player claim histories | OP |
| `oohshiny.clearall` | Delete all reward data | OP |

### Permission Examples

To allow all players to claim rewards:
```
lp group default permission set oohshiny.claim true
```

To give moderators full access except clearall:
```
lp group moderator permission set oohshiny.base true
lp group moderator permission set oohshiny.claim true
lp group moderator permission set oohshiny.set true
lp group moderator permission set oohshiny.remove true
lp group moderator permission set oohshiny.list true
lp group moderator permission set oohshiny.give true
lp group moderator permission set oohshiny.reload true
lp group moderator permission set oohshiny.reset true
```

To give admins complete access:
```
lp group admin permission set oohshiny.* true
```

## Language Configuration

All messages are fully configurable through a language file. The file is automatically created at:
```
config/oohshiny/lang.json
```

### Basic Usage

Edit the `lang.json` file to customize any message:
```json
{
  "prefix": "<aqua>[Ooh Shiny]</aqua>",
  "loot.claimed": "<aqua>You found something shiny: {item}!",
  "error.already_claimed": "<red>You've already claimed this!"
}
```

After editing, reload with:
```
/oohshiny reloadlang
```

### Text Formatting

Requires [TextPlaceholderAPI](https://modrinth.com/mod/placeholder-api) (optional but recommended).

**Colors:**
- `<red>`, `<green>`, `<blue>`, `<yellow>`, `<aqua>`, `<gold>`, etc.
- Hex colors: `<#FF0000>`

**Formatting:**
- `<bold>`, `<italic>`, `<underline>`, `<strikethrough>`

**Advanced:**
- Gradients: `<gradient:blue:aqua>text</gradient>`
- Rainbow: `<rainbow>text</rainbow>`
- Hover: `<hover:'tooltip text'>hover me</hover>`

**Placeholders:**
- Built-in: `{x}`, `{y}`, `{z}`, `{item}`, `{player}`, `{count}`
- TextPlaceholderAPI: `%player:name%`, `%server:tps%`, etc.

**Example:**
```json
{
  "prefix": "<gradient:aqua:blue><bold>[Ooh Shiny]</bold></gradient>",
  "loot.claimed": "<rainbow>✨ You found: {item}! ✨</rainbow>"
}
```

## Data Storage

All reward data is stored in JSON format at:
```
config/oohshiny/oohshiny.json
```

The file is automatically created on first use and updated whenever:
- Rewards are created or removed
- Players claim rewards
- Data is manually reloaded

### Data Structure

Each reward entry contains:
- **Location**: Dimension and block coordinates
- **Reward Item**: Serialized item data (type, count, NBT data)
- **Claimed Players**: List of player UUIDs who have claimed this reward

You can manually edit this file, but remember to run `/oohshiny reload` afterward to apply changes.

## License

This project is licensed under the Apache License 2.0. See the [LICENSE](LICENSE) file for details.

## Authors

- **Destroyaa**

## Support

For bug reports, feature requests, or questions, please open an issue on the project repository.

### Performance

- Efficient storage: JSON-based with automatic saving
- Thread-safe: Uses concurrent data structures where needed
- Optimized particle effects: Runs on server tick with distance checks

## Tips and Best Practices

1. **Backup your data**: Always backup `config/oohshiny/oohshiny.json` before major operations
2. **Use custom items**: The built-in chest and pokeball heads make rewards more visually appealing
3. **Strategic placement**: Place rewards in interesting or hard-to-reach locations
4. **Regular reloads**: If you manually edit the JSON file, always reload afterward
5. **Permission hierarchy**: Give players only the permissions they need
6. **Testing**: Use `/oohshiny reset` to test rewards without creating new ones
7. **Visual feedback**: The particle effects help players know they've found something special

## Troubleshooting

### Players can't claim rewards
- Verify they have the `oohshiny.claim` permission
- Check the server logs for permission errors
- Ensure LuckPerms is properly configured

### Rewards not saving
- Check file permissions on the `config/oohshiny` directory
- Look for errors in the server console
- Try manually running `/oohshiny reload`

### Rewards disappear after restart
- Ensure the `oohshiny.json` file exists and is not corrupted
- Check that the mod loaded correctly during startup
- Verify no other mods are interfering with the config directory

### Particles not showing
- Particles automatically load on server start (as of v1.1.0)
- Particles only appear for players who haven't claimed the reward
- Particles only render within 16 blocks of a player
- Try running `/oohshiny reload` if particles still don't appear
