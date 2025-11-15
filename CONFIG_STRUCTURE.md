# OohShiny Config Structure

## New Format (Current)

The config file now nests `completionCommands` within each category for better organization.

### Structure

```json
{
  "categoryName": {
    "completionCommands": [
      "command 1",
      "command 2"
    ],
    "location_key_1": {
      "dimension": "minecraft:overworld",
      "position": {"x": 0, "y": 64, "z": 0},
      "rewardItems": [...],
      "claimedPlayers": [...]
    },
    "location_key_2": {
      ...
    }
  }
}
```

### Complete Example

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
      "rewardItems": [
        {
          "nbt": "{count:1,id:\"minecraft:diamond\"}"
        }
      ],
      "claimedPlayers": []
    }
  },
  "winter": {
    "completionCommands": [
      "give {player} diamond 3",
      "say {player} completed winter collection!"
    ],
    "minecraft:overworld|100|64|200": {
      "dimension": "minecraft:overworld",
      "position": {
        "x": 100,
        "y": 64,
        "z": 200
      },
      "rewardItems": [
        {
          "nbt": "{count:1,id:\"minecraft:diamond_sword\"}"
        }
      ],
      "claimedPlayers": ["uuid-here"]
    }
  },
  "winterhunt25": {
    "completionCommands": [
      "advancement grant {player} only custom:winter_hunt_complete",
      "tellraw @a {\"text\":\"{player} completed the Winter Hunt 2025!\",\"color\":\"gold\"}"
    ],
    "minecraft:overworld|123|154|-281": {
      "dimension": "minecraft:overworld",
      "position": {
        "x": 123,
        "y": 154,
        "z": -281
      },
      "rewardItems": [
        {
          "nbt": "{count:1,id:\"minecraft:diamond\"}"
        }
      ],
      "claimedPlayers": []
    }
  }
}
```

## Benefits

1. **Logical Grouping**: Completion commands are grouped with their category
2. **Better Organization**: Each category is self-contained
3. **Easier Manual Editing**: Find everything related to a category in one place
4. **Clearer Structure**: The hierarchy is more intuitive

## Migration

The system automatically migrates from the old format:

### Old Format (Deprecated)
```json
{
  "completionCommands": {
    "winter": ["command1", "command2"]
  },
  "winter": {
    "location1": {...}
  }
}
```

### New Format (Automatic)
```json
{
  "winter": {
    "completionCommands": ["command1", "command2"],
    "location1": {...}
  }
}
```

When the mod loads an old format config, it will automatically convert it to the new format on the next save.

## Notes

- `completionCommands` is optional - categories without completion commands won't have this field
- Empty categories (no loots, no commands) will be saved as empty objects: `"categoryName": {}`
- The `completionCommands` array appears first in the category for readability
- Location keys follow the format: `dimension|x|y|z`
