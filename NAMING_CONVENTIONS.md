# OohShiny Naming Conventions

This document outlines the professional naming conventions used throughout the OohShiny mod.

## Config Directory
**Location:** `config/oohshiny/`

All configuration files are stored in lowercase:
- `config/oohshiny/oohshiny.json` - Main loot data
- `config/oohshiny/lang.json` - Language/translation file

## Permissions
All permissions use lowercase with dot notation:

### Base Permissions
- `oohshiny` - Base permission for command access
- `oohshiny.admin` - Admin access

### Command Permissions
- `oohshiny.command.set` - Create loot entries
- `oohshiny.command.remove` - Remove loot entries
- `oohshiny.command.list` - List all loot entries
- `oohshiny.command.reload` - Reload configuration
- `oohshiny.command.reset` - Reset player claims
- `oohshiny.command.clearall` - Clear all loot data
- `oohshiny.command.give` - Give special items
- `oohshiny.command.teleport` - Teleport to loot locations

### Player Permissions
- `oohshiny.claim` - Claim loot rewards

## Logging
All log messages are tagged with lowercase identifiers:
- Logger name: `oohshiny`
- Consistent across all mod components

## Mod ID
- Internal mod ID: `oohshiny` (lowercase)
- Display name: "OohShiny" or "Ooh Shiny"

## Commands
All commands use lowercase:
- `/oohshiny` - Base command
- `/oohshiny set <category>` - Setup mode
- `/oohshiny create <category>` - Create category
- `/oohshiny remove` - Remove mode
- `/oohshiny list [category]` - List entries
- `/oohshiny reload` - Reload config
- `/oohshiny reset <player>` - Reset claims
- `/oohshiny clearall` - Clear all data
- `/oohshiny give <type> <subtype>` - Give items

## Categories
- Default category: `default` (lowercase)
- Custom categories: Use lowercase for consistency (e.g., `winter`, `summer`, `rare`)

## Why Lowercase?

1. **Cross-platform compatibility** - Lowercase names work consistently on all operating systems
2. **Professional standard** - Most Minecraft mods use lowercase for configs and permissions
3. **No confusion** - Eliminates case-sensitivity issues
4. **URL/Path friendly** - Works well in file systems and web contexts
5. **LuckPerms convention** - Follows standard permission naming patterns

## Migration from Previous Versions

If you're upgrading from an older version that used uppercase names:
- Old configs in `config/OOHSHINY/` will continue to work
- The mod will automatically use the lowercase directory going forward
- Permissions should be updated to lowercase in your permission manager
- Both uppercase and lowercase permissions work during transition period

## Best Practices

When working with OohShiny:
- Always use lowercase for category names
- Use lowercase permission nodes in LuckPerms/permission plugins
- Reference the config directory as `config/oohshiny/`
- Use descriptive, lowercase category names (e.g., `event_halloween`, `tier_legendary`)
