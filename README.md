# Ohh Shiny

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
- **LuckPerms**: Optional but recommended for permission management

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) for Minecraft 1.21.1
2. Download and install [Fabric API](https://modrinth.com/mod/fabric-api)
3. Download and install [Fabric Language Kotlin](https://modrinth.com/mod/fabric-language-kotlin)
4. Download the Ohh Shiny mod JAR file
5. Place the JAR file in your `mods` folder
6. (Optional) Install [LuckPerms](https://luckperms.net/) for permission management
7. Start your server

## Usage

### Creating Rewards

1. Hold the item you want to give as a reward in your main hand
2. Run `/ohhshiny set` to enter setup mode
3. Right-click on any block to create a reward at that location
4. The reward is immediately saved and ready for players to claim

**Note**: Setup mode automatically exits after placing one reward. Run `/ohhshiny set` again to place another.

### Removing Rewards

1. Run `/ohhshiny remove` to enter remove mode
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

#### `/ohhshiny set`
Enters setup mode to create a new reward location.

#### `/ohhshiny remove`
Enters remove mode to delete an existing reward location.

#### `/ohhshiny list`
Lists all active reward locations with details:
- World dimension
- Coordinates
- Reward item
- Number of players who have claimed it
- Clickable teleport link for easy navigation

#### `/ohhshiny give chest <type>`
Gives a custom textured chest player head.
- Types: `copper`, `iron`, `gold`
- These are special items perfect for use as visually distinctive rewards

#### `/ohhshiny give pokeball <type>`
Gives a custom textured pokeball player head.
- Types: `poke`, `ultra`, `master`
- Great for themed servers or special events

#### `/ohhshiny reload`
Reloads all reward data from the configuration file on disk.
- Useful for applying manual edits or recovering from errors
- Shows the number of rewards loaded

#### `/ohhshiny reset <player>`
Resets a specific player's claim history, allowing them to claim all rewards again.
- Useful for testing or special events
- Shows the number of claims reset

#### `/ohhshiny clearall`
**DESTRUCTIVE**: Permanently deletes all rewards and claim data.
- Requires confirmation (run the command twice within 30 seconds)
- This action cannot be undone
- Use with extreme caution

## Permissions

Ohh Shiny uses LuckPerms for permission management. If LuckPerms is not installed, all commands default to requiring operator status (level 2+).

### Available Permissions

| Permission | Description | Default |
|-----------|-------------|---------|
| `ohhshiny.base` | Access to the base `/ohhshiny` command | OP |
| `ohhshiny.claim` | Ability to claim rewards by right-clicking blocks | OP |
| `ohhshiny.set` | Create new reward locations | OP |
| `ohhshiny.remove` | Delete existing reward locations | OP |
| `ohhshiny.list` | View all active reward locations | OP |
| `ohhshiny.give` | Spawn custom textured items | OP |
| `ohhshiny.reload` | Reload data from disk | OP |
| `ohhshiny.reset` | Reset player claim histories | OP |
| `ohhshiny.clearall` | Delete all reward data | OP |

### Permission Examples

To allow all players to claim rewards:
```
lp group default permission set ohhshiny.claim true
```

To give moderators full access except clearall:
```
lp group moderator permission set ohhshiny.base true
lp group moderator permission set ohhshiny.claim true
lp group moderator permission set ohhshiny.set true
lp group moderator permission set ohhshiny.remove true
lp group moderator permission set ohhshiny.list true
lp group moderator permission set ohhshiny.give true
lp group moderator permission set ohhshiny.reload true
lp group moderator permission set ohhshiny.reset true
```

To give admins complete access:
```
lp group admin permission set ohhshiny.* true
```

## Data Storage

All reward data is stored in JSON format at:
```
config/ohhshiny/ohhshiny.json
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

You can manually edit this file, but remember to run `/ohhshiny reload` afterward to apply changes.

## License

This project is licensed under the Apache License 2.0. See the [LICENSE](LICENSE) file for details.

## Authors

- **Saxophonist Reaper**
- **Destroyaa**

## Support

For bug reports, feature requests, or questions, please open an issue on the project repository.

## Version History

### 1.1.0
- **Fixed**: Corrected package structure.
- **Fixed**: Particles now persist across server restarts and player relogs
- **Added**: Automatic data loading on server start (no manual reload required)
- **Improved**: Rainbow particle effects now cycle smoothly over 7 seconds
- **Improved**: Particles only render for nearby players who haven't claimed the reward

### 1.0.0
- Initial release
- Basic reward creation and claiming system
- LuckPerms integration
- Custom textured item support
- Block protection
- Particle effects
- Persistent storage
- Multi-dimension support

### Performance

- Efficient storage: JSON-based with automatic saving
- Thread-safe: Uses concurrent data structures where needed
- Optimized particle effects: Runs on server tick with distance checks

## Tips and Best Practices

1. **Backup your data**: Always backup `config/ohhshiny/ohhshiny.json` before major operations
2. **Use custom items**: The built-in chest and pokeball heads make rewards more visually appealing
3. **Strategic placement**: Place rewards in interesting or hard-to-reach locations
4. **Regular reloads**: If you manually edit the JSON file, always reload afterward
5. **Permission hierarchy**: Give players only the permissions they need
6. **Testing**: Use `/ohhshiny reset` to test rewards without creating new ones
7. **Visual feedback**: The particle effects help players know they've found something special

## Troubleshooting

### Players can't claim rewards
- Verify they have the `ohhshiny.claim` permission
- Check the server logs for permission errors
- Ensure LuckPerms is properly configured

### Rewards not saving
- Check file permissions on the `config/ohhshiny` directory
- Look for errors in the server console
- Try manually running `/ohhshiny reload`

### Rewards disappear after restart
- Ensure the `ohhshiny.json` file exists and is not corrupted
- Check that the mod loaded correctly during startup
- Verify no other mods are interfering with the config directory

### Particles not showing
- Particles automatically load on server start (as of v1.1.0)
- Particles only appear for players who haven't claimed the reward
- Particles only render within 16 blocks of a player
- Try running `/ohhshiny reload` if particles still don't appear
