# OohShiny Wiki

Complete documentation for the OohShiny Minecraft mod.

---

## Table of Contents

1. [Categories](#categories)
2. [Category Completion Commands](#category-completion-commands)
3. [Permissions System](#permissions-system)
4. [Permission Migration Guide](#permission-migration-guide)

---

# Categories

The OohShiny mod supports categories for organizing loot entries. This allows you to group rewards by themes, events, or any organizational structure you prefer.

## Category Commands

### Create a Category
```bash
/oohshiny create <category>
```
Creates a new category.

**Example:**
```bash
/oohshiny create winter
```

### Set Active Category
```bash
/oohshiny set <category>
```
Enters setup mode with a specific category. The next loot you place will be added to this category.

**Example:**
```bash
/oohshiny set winter
```

**Important:** The category must exist before you can use it. If you try to use a non-existent category, you'll get an error message.

You can also use `/oohshiny set` without arguments to use the default category.

**Tab Completion:** Press Tab after `/oohshiny set ` to see all available categories.

### List Loot by Category
```bash
/oohshiny list                  # Lists all loot entries grouped by category
/oohshiny list <category>       # Lists only loot entries in a specific category
```

**Example:**
```bash
/oohshiny list winter
```

**Tab Completion:** Press Tab after `/oohshiny list ` to see all available categories.

## Category Workflow

### Creating and Using a Category

1. **Create the category:**
   ```bash
   /oohshiny create winter
   ```

2. **Set the category for placing loot:**
   ```bash
   /oohshiny set winter
   ```
   You'll see: "Setup mode enabled for category: winter"

3. **Place your loot:**
   - Hold the item you want to give as a reward
   - Right-click a block
   - The loot entry is now associated with the "winter" category

4. **View loot in that category:**
   ```bash
   /oohshiny list winter
   ```

## Category Features

### Tab Completion / Suggestions

When using commands that require a category, press **Tab** to see suggestions for all available categories.

**Commands with Tab Completion:**
- `/oohshiny set <TAB>` - Shows all available categories
- `/oohshiny list <TAB>` - Shows all available categories
- `/oohshiny completion add <TAB>` - Shows all available categories

**How It Works:**
1. Type `/oohshiny set `
2. Press **Tab**
3. See a list of all created categories (e.g., "default", "winter", "summer")
4. Continue typing or select from suggestions

### Category Validation

Commands that accept a category name will **validate** that the category exists before executing.

**Validation Rules:**
- ✅ Category must exist before you can use it
- ❌ Invalid category names will be rejected with an error message
- 💡 Error message tells you how to create the missing category

**Examples:**

**Success (Category Exists):**
```
> /oohshiny set winter
✓ Setup mode enabled for category: winter
```

**Failure (Category Doesn't Exist):**
```
> /oohshiny set nonexistent
✗ Unknown category: nonexistent. Use /oohshiny create <category> to create it first.
```

### Built-in Categories
- **default** - Always available, doesn't need to be created
- Any categories you create with `/oohshiny create <name>`

## Config File Format

The config file (`config/oohshiny/oohshiny.json`) is organized with categories at the top level, with `completionCommands` nested within each category:

```json
{
  "default": {
    "minecraft:overworld|50|64|100": {
      "dimension": "minecraft:overworld",
      "position": {"x": 50, "y": 64, "z": 100},
      "rewardItems": [...],
      "claimedPlayers": [...]
    }
  },
  "winter": {
    "completionCommands": [
      "give {player} diamond 3",
      "say {player} completed winter collection!"
    ],
    "minecraft:overworld|100|64|200": {
      "dimension": "minecraft:overworld",
      "position": {"x": 100, "y": 64, "z": 200},
      "rewardItems": [...],
      "claimedPlayers": [...]
    }
  }
}
```

This structure makes it easy to:
- See all entries in a category at a glance
- Manually edit or add entries to specific categories
- Quickly understand your loot organization

## Category Use Cases

- **Seasonal Events:** Create categories like "winter", "summer", "halloween", "christmas"
- **Difficulty Tiers:** Categories like "easy", "medium", "hard" for different reward levels
- **Regions:** Categories like "spawn", "nether", "end" for location-based organization
- **Types:** Categories like "rare", "common", "legendary" for reward rarity
- **VIP Content:** Exclusive categories for premium players
- **Event Series:** Organize multi-part events or quests

## Backward Compatibility

- All existing loot entries without a category are automatically assigned to the "default" category
- Old config files are migrated automatically when loaded
- The "default" category is always available

---

# Category Completion Commands

Execute console commands automatically when a player completes all loot entries within a category.

## Overview

When a player claims the last unclaimed loot in a category, the server automatically executes any configured completion commands for that category. This happens instantly and can be used to:
- Give additional rewards
- Broadcast announcements
- Grant achievements or titles
- Trigger other game mechanics

## Completion Commands

### Add a Completion Command
```bash
/oohshiny completion add <category> <command>
```

Adds a command to be executed when all loots in the specified category are claimed by a player.

**Placeholder:**
- `{player}` - Replaced with the player's username who completed the category

**Examples:**
```bash
# Give a diamond
/oohshiny completion add winter give {player} diamond 1

# Broadcast a message
/oohshiny completion add rare say {player} found all rare items!

# Grant an advancement
/oohshiny completion add summer advancement grant {player} only custom:summer_complete

# Teleport player
/oohshiny completion add secret tp {player} 100 64 200

# Multiple commands
/oohshiny completion add epic give {player} diamond 10
/oohshiny completion add epic give {player} netherite_ingot 1
/oohshiny completion add epic tellraw @a {"text":"{player} is now EPIC!","color":"gold"}
```

### Remove a Completion Command
```bash
/oohshiny completion remove <category> <index>
```

Removes a completion command by its index (shown in the list command).

**Example:**
```bash
/oohshiny completion remove winter 0
```

### List Completion Commands
```bash
/oohshiny completion list              # Lists all completion commands
/oohshiny completion list <category>   # Lists commands for specific category
```

## Quick Setup Example

```bash
# 1. Create a category
/oohshiny create treasure_hunt

# 2. Add completion rewards
/oohshiny completion add treasure_hunt give {player} diamond 5
/oohshiny completion add treasure_hunt say {player} completed the Treasure Hunt!

# 3. Place loots in the category
/oohshiny set treasure_hunt
# Right-click blocks with items to place loots

# 4. Done! When a player claims all loots, commands execute automatically
```

## How It Works

1. **Create a category** and add loot entries to it
2. **Configure completion commands** using `/oohshiny completion add`
3. **When a player claims** the last unclaimed loot in that category, all completion commands are executed automatically
4. **Commands are run from the console** with the player's name substituted for `{player}`

## Important Notes

### Command Execution
- Commands are executed **from the console** (server command source)
- Commands run with **full permissions** (server-level authority)
- Commands execute **immediately** when the last loot is claimed
- All configured commands for a category run in sequence

### Placeholder Substitution
- The `{player}` placeholder is **case-sensitive**
- It will be replaced with the **exact username** of the player who completed the category
- Use it anywhere in your command string

### Multiple Commands
- You can add **multiple commands** to a single category
- They execute in the **order they were added**
- Use indices (0, 1, 2...) to remove specific commands

### Category Validation
- Commands can only be added to **existing categories**
- If a category doesn't exist, you'll get an error message
- Create the category first using `/oohshiny create <category>`

### Re-completion
- If a player's claims are reset using `/oohshiny reset <player>`, they can complete categories again
- Commands will execute again when they reclaim all loots

## Use Cases

### Event Completion Rewards
```bash
/oohshiny completion add halloween give {player} carved_pumpkin 1
/oohshiny completion add halloween give {player} candy 64
```

### Achievements and Titles
```bash
/oohshiny completion add legendary advancement grant {player} only custom:legendary_hunter
/oohshiny completion add legendary title {player} title {"text":"Legendary Hunter","color":"gold"}
```

### Broadcasting Success
```bash
/oohshiny completion add rare tellraw @a {"text":"{player} found all rare items!","color":"gold","bold":true}
```

### Tiered Progression
```bash
# Easy tier completion
/oohshiny completion add easy give {player} iron_ingot 10

# Medium tier completion
/oohshiny completion add medium give {player} gold_ingot 10

# Hard tier completion
/oohshiny completion add hard give {player} diamond 5
```

## Permissions

- **Management:** Requires `oohshiny.command.completion` permission
- **Triggering:** Players don't need special permissions to trigger completion commands - they just need to claim all loots in a category

## Storage

All completion commands are saved in `config/oohshiny/oohshiny.json` and persist across server restarts automatically.

## Tips

1. **Test your commands first** - Run them manually before adding as completion commands
2. **Use meaningful category names** - Makes managing completion commands easier
3. **List commands regularly** - Check what's configured with `/oohshiny completion list`
4. **Backup your config** - The config file stores all completion commands
5. **Be careful with powerful commands** - They execute with console permissions

---

# Permissions System

OohShiny uses LuckPerms when available, with automatic fallback to vanilla operator permissions (OP level 2+) when LuckPerms is not installed.

## Permission Hierarchy

### Wildcard Permissions

| Permission | Description |
|-----------|-------------|
| `oohshiny.*` | Grants ALL permissions (admin + claim + commands) |
| `oohshiny.admin` | Grants all admin/command permissions |
| `oohshiny.command.*` | Grants all command permissions |
| `oohshiny.claim.*` | Grants claim permissions for all categories |
| `oohshiny.claim.category.*` | Grants claim permissions for all categories (explicit) |

### Base Permissions

| Permission | Description | Default |
|-----------|-------------|---------|
| `oohshiny` | Base permission node | OP |

## Claim Permissions

Claim permissions control which players can claim loots and which categories they can access.

### General Claim Permission

| Permission | Description | Default |
|-----------|-------------|---------|
| `oohshiny.claim` | Base claim permission - allows claiming in ALL categories | OP |

### Per-Category Claim Permissions

You can grant claim permissions on a per-category basis using the following format:

**Format:** `oohshiny.claim.category.<category_name>`

**Examples:**
- `oohshiny.claim.category.winter` - Can claim loots in the "winter" category
- `oohshiny.claim.category.treasure_hunt` - Can claim loots in the "treasure_hunt" category
- `oohshiny.claim.category.rare` - Can claim loots in the "rare" category

### Permission Check Order

When a player tries to claim a loot, the system checks permissions in this order:

1. **Specific Category Permission** - `oohshiny.claim.category.<category>`
2. **Category Wildcard** - `oohshiny.claim.category.*`
3. **Base Claim Permission** - `oohshiny.claim`
4. **Fallback** - Vanilla OP level 2+ (if LuckPerms unavailable)

If any of these checks pass, the player can claim the loot.

## Command Permissions

Command permissions control access to administrative features.

| Permission | Command | Description | Default |
|-----------|---------|-------------|---------|
| `oohshiny.command.create` | `/oohshiny create` | Create new categories | OP |
| `oohshiny.command.set` | `/oohshiny set` | Enter setup mode to place loots | OP |
| `oohshiny.command.remove` | `/oohshiny remove` | Enter remove mode to delete loots | OP |
| `oohshiny.command.list` | `/oohshiny list` | View all loot entries | OP |
| `oohshiny.command.give` | `/oohshiny give` | Spawn custom textured items | OP |
| `oohshiny.command.reload` | `/oohshiny reload` | Reload data from disk | OP |
| `oohshiny.command.reset` | `/oohshiny reset` | Reset player claim histories | OP |
| `oohshiny.command.clearall` | `/oohshiny clearall` | Delete all loot data (destructive) | OP |
| `oohshiny.command.completion` | `/oohshiny completion` | Manage category completion commands | OP |
| `oohshiny.command.teleport` | Teleport links | Enables clickable teleport coordinates | OP |

## Common Permission Setups

### Setup 1: All Players Can Claim Everything

```bash
lp group default permission set oohshiny.claim true
```

### Setup 2: Per-Category Access for Different Groups

**Winter Event Example:**
```bash
# VIP players can claim winter event loots
lp group vip permission set oohshiny.claim.category.winter true

# Everyone can claim default loots
lp group default permission set oohshiny.claim.category.default true
```

**Tiered Progression Example:**
```bash
# New players can only claim "easy" loots
lp group newbie permission set oohshiny.claim.category.easy true

# Members can claim easy and medium
lp group member permission set oohshiny.claim.category.easy true
lp group member permission set oohshiny.claim.category.medium true

# Veterans can claim all
lp group veteran permission set oohshiny.claim.category.* true
```

### Setup 3: Moderator Access

Give moderators full command access:

```bash
# Option 1: Individual permissions (without clearall)
lp group moderator permission set oohshiny.command.create true
lp group moderator permission set oohshiny.command.set true
lp group moderator permission set oohshiny.command.remove true
lp group moderator permission set oohshiny.command.list true
lp group moderator permission set oohshiny.command.give true
lp group moderator permission set oohshiny.command.reload true
lp group moderator permission set oohshiny.command.reset true
lp group moderator permission set oohshiny.command.completion true
lp group moderator permission set oohshiny.command.teleport true
lp group moderator permission set oohshiny.claim true

# Option 2: Wildcard (includes clearall)
lp group moderator permission set oohshiny.admin true
```

### Setup 4: Admin Full Access

```bash
lp group admin permission set oohshiny.* true
```

## Per-Player Permissions

### Grant specific category access
```bash
lp user PlayerName permission set oohshiny.claim.category.secret true
```

### Grant command access
```bash
lp user PlayerName permission set oohshiny.command.set true
lp user PlayerName permission set oohshiny.command.remove true
```

### Grant full access
```bash
lp user PlayerName permission set oohshiny.* true
```

## Temporary Permissions

Grant temporary access for events:

```bash
# Grant winter category access for 7 days
lp group default permission set oohshiny.claim.category.winter true 7d

# Grant admin access for 1 hour
lp user EventHost permission set oohshiny.admin true 1h
```

## Permission Negation

Explicitly deny permissions:

```bash
# Prevent a specific player from claiming
lp user BadPlayer permission set oohshiny.claim false

# Prevent access to a specific category
lp user PlayerName permission set oohshiny.claim.category.secret false
```

## Checking Permissions

```bash
# Check all oohshiny permissions
lp user PlayerName permission info

# Search for specific permissions
lp user PlayerName permission check oohshiny.claim
lp user PlayerName permission check oohshiny.claim.category.winter
```

## Permission Inheritance

Groups can inherit from other groups:

```bash
# Create a hierarchy
lp creategroup player
lp creategroup vip
lp creategroup moderator

# Set up inheritance
lp group vip parent add player
lp group moderator parent add vip

# Grant permissions
lp group player permission set oohshiny.claim true
lp group vip permission set oohshiny.claim.category.* true
lp group moderator permission set oohshiny.admin true
```

## Troubleshooting

### Players Can't Claim Loots

1. **Check if LuckPerms is installed**: `/lp info`
2. **Verify player has claim permission**: `/lp user <player> permission check oohshiny.claim`
3. **Check category-specific permission**: `/lp user <player> permission check oohshiny.claim.category.<category>`
4. **Without LuckPerms**: Ensure player is OP level 2+ using `/op <player>`

### Commands Not Working

1. **Check command permission**: `/lp user <player> permission check oohshiny.command.<command>`
2. **Try admin wildcard**: `/lp user <player> permission set oohshiny.admin true`
3. **Without LuckPerms**: Use `/op <player>`

### Per-Category Claims Not Working

1. **Ensure category exists**: `/oohshiny list` to see all categories
2. **Check exact category name**: Category names are case-sensitive
3. **Verify permission format**: Must be `oohshiny.claim.category.<exact_category_name>`
4. **Check permission hierarchy**: Player might have `oohshiny.claim` which overrides category restrictions

## Error Messages

When a player lacks permission, they'll see:

- **No claim permission**: "You don't have permission to claim loots in the '<category>' category"
- **No command permission**: Command won't appear in tab completion or will fail silently
- **No admin permission**: "You don't have permission to use this command"

## Best Practices

1. **Use Groups**: Manage permissions through groups rather than individual players
2. **Principle of Least Privilege**: Only grant permissions users actually need
3. **Category Organization**: Use categories to organize loots by accessibility level
4. **Test Permissions**: Create test accounts to verify permission setups
5. **Document Changes**: Keep track of which groups have which permissions
6. **Regular Audits**: Periodically review and clean up unused permissions
7. **Backup Permissions**: Export LuckPerms data before major changes: `/lp export`

---

# Permission Migration Guide

## What Changed

The permission system has been completely overhauled to provide better organization, per-category claim permissions, and proper LuckPerms integration.

### New Permission Structure

```
oohshiny.*
├── oohshiny.admin (all admin commands)
│   └── oohshiny.command.*
│       ├── oohshiny.command.create
│       ├── oohshiny.command.set
│       ├── oohshiny.command.remove
│       ├── oohshiny.command.list
│       ├── oohshiny.command.give
│       ├── oohshiny.command.reload
│       ├── oohshiny.command.reset
│       ├── oohshiny.command.clearall
│       ├── oohshiny.command.completion (NEW)
│       └── oohshiny.command.teleport (NEW)
└── oohshiny.claim (all categories)
    └── oohshiny.claim.category.* (NEW)
        ├── oohshiny.claim.category.default
        ├── oohshiny.claim.category.winter
        └── oohshiny.claim.category.<any_category>
```

### New Features

#### Per-Category Claim Permissions

**Before:**
- `oohshiny.claim` - All or nothing

**After:**
- `oohshiny.claim` - Still grants access to all categories (backward compatible)
- `oohshiny.claim.category.<category>` - Grant access to specific category
- `oohshiny.claim.category.*` - Grant access to all categories (explicit)

#### Permission Check Hierarchy

When a player tries to claim a loot, permissions are checked in this order:

1. `oohshiny.claim.category.<specific_category>` - Most specific
2. `oohshiny.claim.category.*` - Category wildcard
3. `oohshiny.claim` - Base claim permission
4. OP level 2+ (if LuckPerms not available)

**First match grants access.**

### Renamed Permissions (Old → New)

All command permissions have been renamed for consistency:

| Old Permission | New Permission | Status |
|---------------|----------------|--------|
| `oohshiny.set` | `oohshiny.command.set` | Old still works (deprecated) |
| `oohshiny.remove` | `oohshiny.command.remove` | Old still works (deprecated) |
| `oohshiny.list` | `oohshiny.command.list` | Old still works (deprecated) |
| `oohshiny.give` | `oohshiny.command.give` | Old still works (deprecated) |
| `oohshiny.reload` | `oohshiny.command.reload` | Old still works (deprecated) |
| `oohshiny.reset` | `oohshiny.command.reset` | Old still works (deprecated) |
| `oohshiny.clearall` | `oohshiny.command.clearall` | Old still works (deprecated) |

**Note:** Old permission nodes are still functional for backward compatibility.

### New Permissions

| Permission | Purpose |
|-----------|---------|
| `oohshiny.admin` | Grant all admin commands |
| `oohshiny.command.*` | Wildcard for all commands |
| `oohshiny.command.create` | Create new categories |
| `oohshiny.command.completion` | Manage completion commands |
| `oohshiny.command.teleport` | Enable teleport links |
| `oohshiny.claim.category.<name>` | Claim specific category |
| `oohshiny.claim.category.*` | Claim all categories (explicit) |

## Migration Options

### Option 1: Keep Current Setup (No Migration Needed)

If your current permission setup works, **you don't need to change anything**. The old permission nodes are still supported.

```bash
# This still works
lp group default permission set oohshiny.claim true
lp group moderator permission set oohshiny.set true
lp group moderator permission set oohshiny.remove true
```

### Option 2: Update to New Permission Nodes (Recommended)

**Before:**
```bash
lp group moderator permission set oohshiny.set true
lp group moderator permission set oohshiny.remove true
lp group moderator permission set oohshiny.list true
lp group moderator permission set oohshiny.give true
lp group moderator permission set oohshiny.reload true
```

**After (Option A - Individual permissions):**
```bash
lp group moderator permission set oohshiny.command.set true
lp group moderator permission set oohshiny.command.remove true
lp group moderator permission set oohshiny.command.list true
lp group moderator permission set oohshiny.command.give true
lp group moderator permission set oohshiny.command.reload true
```

**After (Option B - Wildcard, recommended):**
```bash
lp group moderator permission set oohshiny.admin true
```

### Option 3: Use Per-Category Claims (New Feature)

```bash
# Remove blanket claim permission
lp group default permission unset oohshiny.claim

# Grant specific category access
lp group default permission set oohshiny.claim.category.default true
lp group default permission set oohshiny.claim.category.easy true

# VIP gets access to special events
lp group vip permission set oohshiny.claim.category.winter true
lp group vip permission set oohshiny.claim.category.rare true
```

## Breaking Changes

**None!** All old permission nodes still work. This update is fully backward compatible.

However, we recommend updating to the new permission structure for:
- Better organization
- Access to new features
- Future compatibility
- Clearer permission hierarchy

## New Use Cases

### Per-Category Access Control

Create tiered progression systems:

```bash
# Beginner tier
lp group beginner permission set oohshiny.claim.category.tutorial true
lp group beginner permission set oohshiny.claim.category.easy true

# Intermediate tier
lp group intermediate parent add beginner
lp group intermediate permission set oohshiny.claim.category.medium true

# Advanced tier
lp group advanced parent add intermediate
lp group advanced permission set oohshiny.claim.category.hard true
lp group advanced permission set oohshiny.claim.category.rare true
```

### Event-Based Access

Grant temporary access to event categories:

```bash
# Winter event for 30 days
lp group default permission set oohshiny.claim.category.winter true 30d

# Halloween event for 1 week
lp group default permission set oohshiny.claim.category.halloween true 7d
```

### VIP-Only Content

Create exclusive content for VIP players:

```bash
# VIP-only categories
lp group vip permission set oohshiny.claim.category.exclusive true
lp group vip permission set oohshiny.claim.category.legendary true
```

---

*For basic usage and installation, see README.md*
