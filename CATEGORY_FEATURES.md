# OohShiny Category Command Features

## Tab Completion / Suggestions

When using commands that require a category, you can press **Tab** to see suggestions for all available categories.

### Commands with Tab Completion:
- `/oohshiny set <TAB>` - Shows all available categories
- `/oohshiny list <TAB>` - Shows all available categories

### How It Works:
1. Type `/oohshiny set `
2. Press **Tab**
3. See a list of all created categories (e.g., "default", "winter", "summer")
4. Continue typing or select from suggestions

## Category Validation

Commands that accept a category name will **validate** that the category exists before executing.

### Validation Rules:
- ✅ Category must exist before you can use it
- ❌ Invalid category names will be rejected with an error message
- 💡 Error message tells you how to create the missing category

### Examples:

#### Success (Category Exists):
```
> /oohshiny set winter
✓ Setup mode enabled for category: winter
```

#### Failure (Category Doesn't Exist):
```
> /oohshiny set nonexistent
✗ Unknown category: nonexistent. Use /oohshiny create <category> to create it first.
```

## Workflow

### Creating and Using a New Category:
```bash
# Step 1: Create the category
/oohshiny create summer

# Step 2: Set it as active (with Tab completion)
/oohshiny set summer
# Press Tab after "set " to see "summer" in suggestions

# Step 3: Place your loot
# [Hold item and right-click a block]

# Step 4: View loot in that category
/oohshiny list summer
# Press Tab after "list " to see "summer" in suggestions
```

### Built-in Categories:
- **default** - Always available, doesn't need to be created
- Any categories you create with `/oohshiny create <name>`

## Benefits

1. **No Typos**: Tab completion helps avoid spelling mistakes
2. **Discovery**: Easily see what categories are available
3. **Safety**: Can't accidentally use wrong category names
4. **Consistency**: Ensures all commands use valid, existing categories
5. **User-Friendly**: Clear error messages guide you to the solution

## Technical Details

- Uses Brigadier's `.suggests()` method for tab completion
- Suggestions are dynamically loaded from the server at runtime
- Categories are validated at command execution time (not parse time)
- Both `/oohshiny set` and `/oohshiny list` support suggestions
- The "default" category is always available and doesn't need creation
- Uses `StringArgumentType.word()` with custom suggestion provider
