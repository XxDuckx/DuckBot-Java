# DuckBot Game-Specific Script Architecture

**Feature:** Game-Organized Scripts, Images, and Popups  
**Date:** November 13, 2025  
**Status:** ✅ Implemented & Tested

---

## 📋 Overview

DuckBot now supports organizing scripts, images, and popup definitions by game. Each game has its own isolated directory structure, allowing multiple games to coexist without conflicts.

**Supported Games (Built-in):**
- ✅ **Ants** - Ants Underground Kingdom
- ✅ **AL** - Ants Legend
- ✅ **ROE** - Rise of Empires
- ✅ **Generic** - Any game/app (extensible)

---

## 📁 Directory Structure

```
data/
├── scripts/
│   ├── ants/
│   │   ├── farm_script.json
│   │   ├── march_script.json
│   │   └── daily_tasks_script.json
│   ├── al/
│   │   ├── building_upgrade.json
│   │   ├── march_attack.json
│   │   └── rally_defense.json
│   ├── roe/
│   │   ├── resource_gather.json
│   │   └── troop_training.json
│   └── generic/
│       └── custom_automation.json
│
└── images/
    ├── ants/
    │   ├── popups/
    │   │   ├── ok_button.png
    │   │   ├── confirm_dialog.png
    │   │   ├── error_popup.png
    │   │   └── definitions.json
    │   └── game/
    │       ├── ant_queen.png
    │       ├── farm_icon.png
    │       ├── menu_button.png
    │       └── assets.json
    │
    ├── al/
    │   ├── popups/
    │   │   ├── ok_button.png
    │   │   ├── confirm_dialog.png
    │   │   └── definitions.json
    │   └── game/
    │       ├── building_icon.png
    │       ├── march_button.png
    │       └── assets.json
    │
    ├── roe/
    │   ├── popups/
    │   │   └── definitions.json
    │   └── game/
    │       └── gathering_icon.png
    │
    └── generic/
        ├── popups/
        │   └── definitions.json
        └── game/
```

---

## 🎮 Game Classes & APIs

### 1. GameRegistry - Central Game Management

```java
// Get a specific game
GameRegistry.GameDefinition game = GameRegistry.getGame("ants");

// Get all games
for (GameRegistry.GameDefinition game : GameRegistry.getAllGames()) {
    System.out.println(game.getDisplayName());
}

// Get game IDs for UI dropdowns
List<String> gameIds = GameRegistry.getGameIds();

// Check if a game is supported
boolean supported = GameRegistry.isGameSupported("roe");

// Add a new game dynamically
GameRegistry.registerGameDynamic("myGame", "My Custom Game", 
    "images/myGame", "scripts/myGame");
```

**GameDefinition Methods:**
- `getId()` - Returns game ID (e.g., "ants")
- `getDisplayName()` - Returns user-friendly name (e.g., "Ants Underground Kingdom")
- `getImagePath()` - Returns base image directory
- `getScriptPath()` - Returns base script directory
- `getPopupImagesPath()` - Returns popup images directory
- `getGameImagesPath()` - Returns game assets directory

### 2. GameScriptManager - Script & Image Organization

```java
// Get scripts for a game
GameScriptManager.GameScripts antScripts = 
    bootstrap.getGameScriptManager().getGameScripts("ants");

// List scripts
for (GameScriptManager.GameScriptFile script : antScripts.getScripts()) {
    System.out.println(script.getName());
    String content = script.getContent();
}

// List game images
for (GameScriptManager.GameImageFile img : antScripts.getGameImages()) {
    System.out.println(img.getName() + " [" + img.getCategory() + "]");
}

// List popup images
for (GameScriptManager.GamePopupFile popup : antScripts.getPopupImages()) {
    System.out.println(popup.getName());
    // Use for IfImageStep detection
}

// Get counts
System.out.println("Ants has " + antScripts.getScriptCount() + " scripts");
System.out.println("Ants has " + antScripts.getGameImageCount() + " images");
System.out.println("Ants has " + antScripts.getPopupImageCount() + " popups");
```

**GameScriptFile Methods:**
- `getName()` - Script name (without .json)
- `getFilePath()` - Full path to script file
- `getContent()` - Read script JSON content

**GameImageFile Methods:**
- `getName()` - Image filename
- `getFilePath()` - Full path to image
- `getCategory()` - "game" or other category

**GamePopupFile Methods:**
- `getName()` - Popup filename
- `getFilePath()` - Full path to popup image
- `getImageName()` - Name without extension (for matching)

### 3. GamePopupManager - Popup Definitions

```java
// Load popups for a game
GamePopupManager popupManager = bootstrap.getGamePopupManager();

// Get all popups for a game
List<GamePopupManager.PopupDefinition> popups = 
    popupManager.getGamePopups("ants");

// Get a specific popup
GamePopupManager.PopupDefinition popup = 
    popupManager.getPopupDefinition("ants", "ok_button");

// Create popup definition
GamePopupManager.PopupDefinition def = new GamePopupManager.PopupDefinition();
def.id = "confirm_dialog";
def.type = "dialog";
def.game = "ants";
def.imagePath = "confirm_dialog.png";
def.description = "Confirmation dialog with yes/no buttons";
def.multipleMatches = true;
popupManager.addPopupDefinition("ants", def);

// Save to file
popupManager.saveGamePopups("ants", dataRoot);
```

**PopupDefinition Structure:**
- `id` - Unique identifier
- `type` - button, dialog, message, error, loading, etc.
- `game` - Game ID
- `imagePath` - Path to popup image
- `description` - Human-readable description
- `coordinates` - Optional: x, y position
- `size` - Optional: width, height
- `multipleMatches` - Can appear in multiple locations
- `action` - Optional: action to take (tap, swipe, etc.)
- `metadata` - Game-specific custom data

**Popup Definitions JSON Format:**
```json
{
  "popups": [
    {
      "id": "ok_button",
      "type": "button",
      "game": "ants",
      "imagePath": "ok_button.png",
      "description": "Generic OK confirmation button",
      "coordinates": {"x": 512, "y": 800},
      "size": {"width": 100, "height": 40}
    },
    {
      "id": "confirm_dialog",
      "type": "dialog",
      "game": "ants",
      "imagePath": "confirm_dialog.png",
      "description": "Confirmation dialog",
      "multipleMatches": true,
      "action": "tap"
    }
  ]
}
```

---

## 🤖 Integration with Bot Profiles & Scripts

### Bot Profile with Game

```java
BotProfile bot = new BotProfile();
bot.name = "Ants Farmer";
bot.game = "ants";  // ← Game-specific
bot.instances.add(new BotInstanceBinding("LDPlayer-1", new ArrayList<>()));
bot.scripts.add(new BotScriptRef("farm_script", true));
bot.scripts.add(new BotScriptRef("daily_tasks_script", true));
```

### Script with Game

```java
Script script = new Script();
script.name = "farm_script";
script.game = "ants";  // ← Must match bot's game
script.author = "DuckBot";
// Add steps...
```

### UI Integration

When a user selects a game in the Bots tab, the UI can:
1. Load game-specific scripts from `GameScriptManager.getGameScripts(gameId)`
2. Show available popup images for image detection
3. Display game-specific settings
4. Auto-organize by game in dropdowns

---

## 📸 Using Game Images in Steps

### IfImageStep with Game Images

```java
// Example: IfImageStep detecting an Ants game popup
IfImageStep step = new IfImageStep();
step.imagePath = "popups/ok_button.png";  // Relative to game's image dir
step.targetX = 512;
step.targetY = 800;
step.action = "tap";

// The ScriptContext knows which game is running
// It can resolve the full path: data/images/ants/popups/ok_button.png
```

### Image Matching in Code

```java
// Get game-specific images during execution
String gameId = botProfile.game;
GameScriptManager.GameScripts gameScripts = 
    bootstrap.getGameScriptManager().getGameScripts(gameId);

// Find a popup image
for (GameScriptManager.GamePopupFile popup : gameScripts.getPopupImages()) {
    if (popup.getName().equals("ok_button.png")) {
        // Use popup.getFilePath() for image matching
        boolean matches = imageService.matches(popup.getFilePath(), screenshot);
    }
}
```

---

## 🎯 Workflow Example: Ants Underground Kingdom

**Step 1: Create Bot Profile**
```
Bot Name: "Ants Farmer"
Game: "Ants Underground Kingdom" (ants)
Instance: LDPlayer-1
Scripts: farm_script, daily_tasks_script
```

**Step 2: System Auto-Organizes**
```
✅ Scripts loaded from: data/scripts/ants/
✅ Popup images available from: data/images/ants/popups/
✅ Game assets available from: data/images/ants/game/
```

**Step 3: Run Bot**
```
1. Load bot profile (game = "ants")
2. Load scripts from data/scripts/ants/
3. Execute scripts with game-specific context
4. Use image detection from data/images/ants/popups/
5. All logs tagged with game ID
```

---

## 💾 Adding New Games

### Option 1: Register at Startup

Edit `GameRegistry.java` and add in the static initializer:
```java
registerGame(new GameDefinition("myGame", "My Game Name", 
    "images/myGame", "scripts/myGame"));
```

### Option 2: Register Dynamically

```java
GameRegistry.registerGameDynamic("myGame", "My Game Name", 
    "images/myGame", "scripts/myGame");

// Auto-creates directories:
// data/scripts/myGame/
// data/images/myGame/popups/
// data/images/myGame/game/
```

### Option 3: Extend for Complex Games

Create game-specific classes:
```java
package com.duckbot.games.ants;

public class AntsGameModule {
    public static void initialize() {
        // Register Ants-specific image addresses
        // Register Ants-specific popup patterns
        // Register Ants-specific script templates
    }
}
```

---

## 🔧 Configuration Files

### Game Popup Definitions

**Location:** `data/images/{game}/popups/definitions.json`

```json
{
  "popups": [
    {
      "id": "ok_button",
      "type": "button",
      "game": "ants",
      "imagePath": "ok_button.png",
      "description": "OK button in dialogs",
      "coordinates": {"x": 512, "y": 800},
      "size": {"width": 100, "height": 40},
      "metadata": {
        "priority": 1,
        "fadeOut": true
      }
    }
  ]
}
```

### Game Assets

**Location:** `data/images/{game}/game/assets.json` (optional)

```json
{
  "assets": [
    {
      "id": "farm_icon",
      "name": "Farm Building Icon",
      "image": "farm_icon.png",
      "coordinates": {"x": 100, "y": 200},
      "bounds": {"width": 80, "height": 80}
    }
  ]
}
```

---

## 📊 Benefits of Game-Specific Organization

| Benefit | Description |
|---------|-------------|
| **Isolation** | Each game's scripts/images are separate |
| **Scalability** | Easy to add 10+ games without conflicts |
| **Maintainability** | Game files grouped logically |
| **Flexibility** | Mix games in a single DuckBot instance |
| **Organization** | Clear UI dropdowns by game |
| **Caching** | Efficient lazy-loading per game |
| **Extensibility** | Add game-specific modules |

---

## 🚀 Implementation Status

✅ **Implemented:**
- GameRegistry for game management
- GameScriptManager for script/image organization
- GamePopupManager for popup definitions
- Directory auto-creation for all games
- Bootstrap integration
- Full game-specific APIs

⏭️ **Next Steps (Optional):**
1. Integrate into UI (Bots tab shows game-specific scripts)
2. Create game-specific script templates
3. Add game configuration panels
4. Create LSS Bot migration utility
5. Add game module loader for complex games

---

## 📚 API Reference

### Access from DuckBotApp

```java
// In DuckBotApp constructor or method
Bootstrap bootstrap = Bootstrap.getInstance();

// Access game managers
GameScriptManager scriptMgr = bootstrap.getGameScriptManager();
GamePopupManager popupMgr = bootstrap.getGamePopupManager();

// Get game for current bot
String gameId = botProfile.game;
GameScriptManager.GameScripts gameScripts = scriptMgr.getGameScripts(gameId);

// List scripts
for (GameScriptManager.GameScriptFile script : gameScripts.getScripts()) {
    System.out.println(script.getName());
}

// List popups
List<GamePopupManager.PopupDefinition> popups = 
    popupMgr.getGamePopups(gameId);
```

---

## 🎓 Example: Building a Game-Specific Workflow

```java
// User selects "Ants Underground Kingdom" game
String selectedGame = "ants";

// Get game definition
GameRegistry.GameDefinition game = GameRegistry.getGame(selectedGame);

// Load game scripts
GameScriptManager.GameScripts gameScripts = 
    bootstrap.getGameScriptManager().getGameScripts(selectedGame);

// Populate UI with game scripts
ObservableList<String> scriptNames = FXCollections.observableArrayList();
for (GameScriptManager.GameScriptFile script : gameScripts.getScripts()) {
    scriptNames.add(script.getName());
}
scriptDropdown.setItems(scriptNames);

// Show available popup images
ObservableList<String> popupImages = FXCollections.observableArrayList();
for (GameScriptManager.GamePopupFile popup : gameScripts.getPopupImages()) {
    popupImages.add(popup.getImageName());
}
popupImageDropdown.setItems(popupImages);

// Create bot profile
BotProfile bot = new BotProfile();
bot.name = "Ants Farmer";
bot.game = selectedGame;
bot.scripts.add(new BotScriptRef(
    scriptNames.get(0), true));  // Select first game script
```

---

**Status: FEATURE COMPLETE ✅**

Game-specific script organization is fully implemented and tested. DuckBot now supports organizing scripts, images, and popup definitions by game, with full API support and automatic directory management.

All 4 built-in games (Ants, AL, ROE, Generic) are registered and ready for use.
