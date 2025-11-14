# OohShiny Categories

The OohShiny mod now supports categories for organizing loot entries. This allows you to group rewards by themes, events, or any organizational structure you prefer.

## Commands

### Create a Category
```
/oohshiny create <category>
```
Creates a new category. Example:
```
/oohshiny create winter
```

### Set Active Category
```
/oohshiny set <category>
```
Enters setup mode with a specific category. The next loot you place will be added to this category. Example:
```
/oohshiny set winter
```

**Important:** The category must exist before you can use it. If you try to use a non-existent category, you'll get an error message telling you to create it first.

You can also use `/oohshiny set` without arguments to use the default category.

**Tab Completion:** When typing `/oohshiny set `, press Tab to see suggestions for all available categories.

### List Loot by Category
```
/oohshiny list
```
Lists all loot entries grouped by category.

```
/oohshiny list <category>
```
Lists only loot entries in a specific category. Example:
```
/oohshiny list winter
```

**Tab Completion:** Press Tab after `/oohshiny list ` to see all available categories.

## Workflow Example

1. **Create a category:**
   ```
   /oohshiny create winter
   ```

2. **Set the category for placing loot:**
   ```
   /oohshiny set winter
   ```
   You'll see: "Setup mode enabled for category: winter"

3. **Place your loot:**
   - Hold the item you want to give as a reward
   - Right-click a block
   - The loot entry is now associated with the "winter" category

4. **View loot in that category:**
   ```
   /oohshiny list winter
   ```

## Config File Format

The config file (`config/oohshiny/oohshiny.json`) is organized with categories at the top level, followed by entries under each category:

```json
{
  "default": {
    "minecraft:overworld|50|64|100": {
      "dimension": "minecraft:overworld",
      "position": {
        "x": 50,
        "y": 64,
        "z": 100
      },
      "rewardItems": [...],
      "claimedPlayers": [...]
    }
  },
  "winter": {
    "minecraft:overworld|100|64|200": {
      "dimension": "minecraft:overworld",
      "position": {
        "x": 100,
        "y": 64,
        "z": 200
      },
      "rewardItems": [...],
      "claimedPlayers": [...]
    },
    "minecraft:overworld|150|70|250": {
      "dimension": "minecraft:overworld",
      "position": {
        "x": 150,
        "y": 70,
        "z": 250
      },
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

## Backward Compatibility

- All existing loot entries without a category will automatically be assigned to the "default" category
- Old config files will be migrated automatically when loaded
- The "default" category is always available

## Use Cases

- **Seasonal Events:** Create categories like "winter", "summer", "halloween", "christmas"
- **Difficulty Tiers:** Categories like "easy", "medium", "hard" for different reward levels
- **Regions:** Categories like "spawn", "nether", "end" for location-based organization
- **Types:** Categories like "rare", "common", "legendary" for reward rarity
